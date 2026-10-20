package com.laundryhub.repository;

import com.laundryhub.exception.ApiException;
import com.laundryhub.model.GCashPayment;
import com.laundryhub.model.Location;
import com.laundryhub.model.Order;
import com.laundryhub.model.OrderRequest;
import com.laundryhub.model.OrderStatus;
import com.laundryhub.model.PriceBreakdown;
import com.laundryhub.model.ServiceType;
import com.laundryhub.model.StatusEvent;
import com.laundryhub.util.Callback;
import com.laundryhub.util.Callbacks;
import com.laundryhub.util.JsonArray;
import com.laundryhub.util.JsonObject;
import com.laundryhub.util.TimeUtils;
import com.laundryhub.util.Validators;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link OrderRepository} backed by Supabase (tables + rpc functions from migrations 0005/0007).
 */
public final class SupabaseOrderRepository implements OrderRepository {

    /** Private bucket for GCash screenshots. */
    public static final String PROOF_BUCKET = "gcash-proofs";

    private static final int MAX_ORDERS = 200;
    private static final int PROOF_LINK_SECONDS = 600;
    private static final int NOT_FOUND = 404;
    private static final int MAX_EXPORT = 5000;
    private static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;

    private final SupabaseClient client;

    /**
     * @param client Supabase client
     */
    public SupabaseOrderRepository(SupabaseClient client) {
        this.client = client;
    }

    @Override
    public void quote(ServiceType service, int kg, long soapId, Callback<PriceBreakdown> callback) {
        JsonObject params = new JsonObject()
                .put("p_service_type", service.getCode())
                .put("p_kg", kg)
                .put("p_soap_id", soapId);
        client.rpc("quote_order", params, Callbacks.map(callback, r -> ModelMapper.toBreakdown((JsonObject) r)));
    }

    @Override
    public void placeOrder(OrderRequest request, Callback<Order> callback) {
        Location pin = request.getLocation();
        JsonObject params = new JsonObject()
                .put("p_service_type", request.getService().getCode())
                .put("p_kg", request.getLoad().getKg())
                .put("p_soap_id", request.getLoad().getSoap().getId())
                .put("p_time_slot", request.getTimeSlot())
                .put("p_payment_method", request.getPayment().getCode())
                .put("p_lat", pin == null ? null : pin.getLat())
                .put("p_lng", pin == null ? null : pin.getLng())
                .put("p_address", pin == null ? null : pin.getAddress())
                .put("p_address_details", request.getAddressDetails());
        if (request.getPayment() instanceof GCashPayment) {
            GCashPayment g = (GCashPayment) request.getPayment();
            params.put("p_gcash_reference", g.getReference()).put("p_gcash_number", g.getSenderNumber());
        }
        // The function returns the order's main fields; fill the rest from the request.
        client.rpc("place_order", params, Callbacks.map(callback, r -> {
            JsonObject o = (JsonObject) r;
            o.put("service_type", request.getService().getCode())
             .put("payment_method", request.getPayment().getCode())
             .put("soap_id", request.getLoad().getSoap().getId());
            if (pin != null) {
                o.put("lat", pin.getLat()).put("lng", pin.getLng()).put("address", pin.getAddress());
            }
            return ModelMapper.toOrder(o);
        }));
    }

    @Override
    public void findMine(Callback<List<Order>> callback) {
        AuthUser.require(client, callback, userId ->
                client.select("orders", Query.select("*").eq("customer_id", userId).order("created_at", true).limit(MAX_ORDERS),
                        Callbacks.map(callback, SupabaseOrderRepository::toOrders)));
    }

    @Override
    public void findById(long orderId, Callback<Order> callback) {
        client.selectOne("orders", Query.select(ModelMapper.ORDER_SELECT).eq("id", orderId), new Callback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject row) {
                if (row == null) {
                    callback.onError(new ApiException(NOT_FOUND, "", "Order not found."));
                } else {
                    Callbacks.<JsonObject, Order>map(callback, ModelMapper::toOrder).onSuccess(row);
                }
            }

            @Override
            public void onError(ApiException error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void findAll(OrderStatus status, String search, Callback<List<Order>> callback) {
        Query q = Query.select(ModelMapper.ORDER_SELECT);
        if (status != null) {
            q.eq("status", status.name());
        }
        if (Validators.hasText(search)) {
            q.search(search, "order_no", "address", "soap_name");
        }
        client.select("orders", q.order("created_at", true).limit(MAX_ORDERS),
                Callbacks.map(callback, SupabaseOrderRepository::toOrders));
    }

    @Override
    public void findHistory(long orderId, Callback<List<StatusEvent>> callback) {
        client.select("order_status_history", Query.select("*").eq("order_id", orderId).order("created_at", false),
                Callbacks.map(callback, rows -> {
                    List<StatusEvent> events = new ArrayList<>();
                    for (JsonObject row : rows.objects()) {
                        events.add(ModelMapper.toStatusEvent(row));
                    }
                    return events;
                }));
    }

    @Override
    public void updateStatus(long orderId, OrderStatus status, Callback<Void> callback) {
        client.update("orders", Query.filter().eq("id", orderId), new JsonObject().put("status", status.name()),
                new Callback<JsonArray>() {
                    @Override
                    public void onSuccess(JsonArray rows) {
                        if (rows.isEmpty()) {
                            callback.onError(new ApiException(403, "", "Only admin or staff can change the status."));
                        } else {
                            callback.onSuccess(null);
                        }
                    }

                    @Override
                    public void onError(ApiException error) {
                        callback.onError(error);
                    }
                });
    }

    @Override
    public void markPaid(long orderId, Callback<Void> callback) {
        client.update("orders", Query.filter().eq("id", orderId), new JsonObject().put("payment_status", "PAID"),
                Callbacks.ignoreResult(callback));
    }

    @Override
    public void findBetween(String fromDate, String toDate, Callback<List<Order>> callback) {
        long endMillis = TimeUtils.parseIso(toDate + "T00:00:00+08:00") + MILLIS_PER_DAY;
        client.select("orders", Query.select(ModelMapper.ORDER_SELECT)
                        .gte("created_at", fromDate + "T00:00:00+08:00")
                        .lt("created_at", TimeUtils.isoUtc(endMillis))
                        .neq("status", "CANCELLED")
                        .order("created_at", false).limit(MAX_EXPORT),
                Callbacks.map(callback, SupabaseOrderRepository::toOrders));
    }

    @Override
    public void assignRider(long orderId, String riderId, Callback<Void> callback) {
        client.rpc("admin_assign_rider", new JsonObject().put("p_order_id", orderId).put("p_rider_id", riderId),
                Callbacks.ignoreResult(callback));
    }

    @Override
    public void verifyGcash(long orderId, boolean approve, String note, Callback<Void> callback) {
        client.rpc("verify_gcash_payment",
                new JsonObject().put("p_order_id", orderId).put("p_approve", approve).put("p_note", note),
                Callbacks.ignoreResult(callback));
    }

    @Override
    public void attachGcashProof(Order order, String userId, Object browserFile, String contentType,
                                 Callback<Void> callback) {
        String extension = contentType != null && contentType.contains("png") ? ".png" : ".jpg";
        String path = userId + "/" + order.getOrderNo() + extension;
        client.upload(PROOF_BUCKET, path, browserFile, contentType, new Callback<String>() {
            @Override
            public void onSuccess(String savedPath) {
                client.rpc("attach_gcash_proof",
                        new JsonObject().put("p_order_id", order.getId()).put("p_path", savedPath),
                        Callbacks.ignoreResult(callback));
            }

            @Override
            public void onError(ApiException error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void resubmitGcash(long orderId, String reference, Callback<Void> callback) {
        client.rpc("resubmit_gcash",
                new JsonObject().put("p_order_id", orderId).put("p_reference", Validators.compact(reference)),
                Callbacks.ignoreResult(callback));
    }

    @Override
    public void getProofUrl(String proofPath, Callback<String> callback) {
        client.createSignedUrl(PROOF_BUCKET, proofPath, PROOF_LINK_SECONDS, callback);
    }

    private static List<Order> toOrders(JsonArray rows) {
        List<Order> orders = new ArrayList<>();
        for (JsonObject row : rows.objects()) {
            orders.add(ModelMapper.toOrder(row));
        }
        return orders;
    }
}
