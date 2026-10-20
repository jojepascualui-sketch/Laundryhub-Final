package com.laundryhub.repository;

import com.laundryhub.model.Booking;
import com.laundryhub.model.CashPayment;
import com.laundryhub.model.JobStatus;
import com.laundryhub.model.Location;
import com.laundryhub.model.Order;
import com.laundryhub.model.OrderStatus;
import com.laundryhub.model.PaymentStatus;
import com.laundryhub.model.PriceBreakdown;
import com.laundryhub.model.Rider;
import com.laundryhub.model.RiderJob;
import com.laundryhub.model.Role;
import com.laundryhub.model.ServiceType;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.model.Soap;
import com.laundryhub.model.StatusEvent;
import com.laundryhub.model.User;
import com.laundryhub.util.JsonObject;
import com.laundryhub.util.MoneyFormatter;
import com.laundryhub.util.Validators;

/**
 * Converts database rows (JSON) into model objects. Pure Java, unit tested.
 */
public final class ModelMapper {

    /** Select list for orders with the customer's name and mobile. */
    public static final String ORDER_SELECT = "*,customer:profiles!orders_customer_id_fkey(full_name,mobile)";

    private ModelMapper() {
    }

    /**
     * @param row     profiles row
     * @param riderRow riders row for riders, may be null
     * @return the user
     */
    public static User toUser(JsonObject row, JsonObject riderRow) {
        Role role = Role.fromCode(row.getString("role"));
        User user = User.create(role, row.getString("id"), row.getString("full_name"), row.getString("email"),
                row.getString("mobile"), row.getBoolean("active", true), row.getString("job_title"));
        if (user instanceof Rider && riderRow != null) {
            return ((Rider) user).withVehicle(riderRow.getString("plate_no"), riderRow.getBoolean("on_duty", false));
        }
        return user;
    }

    /**
     * @param row shop_settings row
     * @return settings
     */
    public static ShopSettings toSettings(JsonObject row) {
        Location shop = row.isNull("shop_lat") || row.isNull("shop_lng") ? null
                : new Location(row.getDouble("shop_lat", 0), row.getDouble("shop_lng", 0), row.getString("shop_address"));
        return new ShopSettings(
                row.getCentavos("rate_per_kg"),
                row.getInt("min_kg", ShopSettings.DEFAULT_MIN_KG),
                row.getCentavos("delivery_fee"),
                row.getDouble("delivery_radius_km", ShopSettings.DEFAULT_RADIUS_KM),
                shop,
                row.getString("shop_hours"),
                row.getString("gcash_number"),
                row.getString("gcash_account_name"),
                row.getBoolean("cash_enabled", true),
                row.getBoolean("gcash_enabled", true));
    }

    /**
     * @param s settings
     * @return columns for an update of shop_settings
     */
    public static JsonObject fromSettings(ShopSettings s) {
        JsonObject o = new JsonObject()
                .put("rate_per_kg", MoneyFormatter.toPesos(s.getRatePerKg()))
                .put("min_kg", s.getMinKg())
                .put("delivery_fee", MoneyFormatter.toPesos(s.getDeliveryFee()))
                .put("delivery_radius_km", s.getDeliveryRadiusKm())
                .put("shop_hours", s.getShopHours())
                .put("gcash_number", Validators.hasText(s.getGcashNumber()) ? Validators.compact(s.getGcashNumber()) : null)
                .put("gcash_account_name", s.getGcashAccountName())
                .put("cash_enabled", s.isCashEnabled())
                .put("gcash_enabled", s.isGcashEnabled());
        if (s.getShopLocation() != null) {
            o.put("shop_lat", s.getShopLocation().getLat())
             .put("shop_lng", s.getShopLocation().getLng())
             .put("shop_address", s.getShopLocation().getAddress());
        }
        return o;
    }

    /**
     * @param row soaps row
     * @return soap
     */
    public static Soap toSoap(JsonObject row) {
        return new Soap(row.getLong("id", 0), row.getString("name"), row.getString("description"),
                row.getCentavos("add_on_per_kg"), row.getBoolean("available", true));
    }

    /**
     * @param row result of rpc quote_order
     * @return the breakdown
     */
    public static PriceBreakdown toBreakdown(JsonObject row) {
        return new PriceBreakdown(row.getInt("charged_kg", 0), row.getCentavos("rate_per_kg"),
                row.getCentavos("soap_add_on_per_kg"), row.getCentavos("laundry_cost"),
                row.getCentavos("soap_cost"), row.getCentavos("delivery_fee"));
    }

    /**
     * @param row orders row, optionally with an embedded {@code customer} object
     * @return the order
     */
    public static Order toOrder(JsonObject row) {
        JsonObject customer = row.getObject("customer");
        Location pin = row.isNull("lat") || row.isNull("lng") ? null
                : new Location(row.getDouble("lat", 0), row.getDouble("lng", 0), row.getString("address"));
        return new Order.Builder()
                .id(row.getLong("id", 0))
                .orderNo(row.getString("order_no"))
                .customerId(row.getString("customer_id"))
                .customerName(customer == null ? row.getString("customer_name") : customer.getString("full_name"))
                .customerMobile(customer == null ? null : customer.getString("mobile"))
                .serviceType(ServiceType.fromCode(row.getString("service_type")))
                .kg(row.getInt("kg", 0))
                .soapId(row.getLong("soap_id", 0))
                .soapName(row.getString("soap_name"))
                .timeSlot(row.getString("time_slot"))
                .location(pin)
                .addressDetails(row.getString("address_details"))
                .paymentMethod(row.getString("payment_method", CashPayment.CODE))
                .paymentStatus(PaymentStatus.fromCode(row.getString("payment_status")))
                .gcashReference(row.getString("gcash_reference"))
                .gcashProofPath(row.getString("gcash_proof_path"))
                .ratePerKg(row.getCentavos("rate_per_kg"))
                .soapAddOnPerKg(row.getCentavos("soap_add_on_per_kg"))
                .laundryCost(row.getCentavos("laundry_cost"))
                .soapCost(row.getCentavos("soap_cost"))
                .deliveryFee(row.getCentavos("delivery_fee"))
                .total(row.getCentavos("total"))
                .status(OrderStatus.fromCode(row.getString("status")))
                .riderId(row.getString("rider_id"))
                .createdAt(row.getString("created_at"))
                .cancelReason(row.getString("cancel_reason"))
                .build();
    }

    /**
     * @param row       rider_jobs row
     * @param cashOrder true when the order is paid in cash
     * @return a PickupJob or DeliveryJob
     */
    public static RiderJob toJob(JsonObject row, boolean cashOrder) {
        return RiderJob.create(row.getString("type"), row.getLong("id", 0), row.getLong("order_id", 0),
                JobStatus.fromCode(row.getString("status")), row.getInt("current_step", 0),
                row.getString("rider_id"), cashOrder);
    }

    /**
     * @param row rider_jobs row with an embedded {@code order} object
     * @return job + order
     */
    public static Booking toBooking(JsonObject row) {
        Order order = toOrder(row.getObject("order"));
        return new Booking(toJob(row, order.isCash()), order, row.getString("completed_at"));
    }

    /**
     * @param row order_status_history row
     * @return the event
     */
    public static StatusEvent toStatusEvent(JsonObject row) {
        return new StatusEvent(row.getString("event"), OrderStatus.fromCode(row.getString("status")),
                row.getString("note"), row.getString("created_at"));
    }
}
