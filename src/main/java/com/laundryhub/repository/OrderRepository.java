package com.laundryhub.repository;

import com.laundryhub.model.Order;
import com.laundryhub.model.OrderRequest;
import com.laundryhub.model.OrderStatus;
import com.laundryhub.model.PriceBreakdown;
import com.laundryhub.model.ServiceType;
import com.laundryhub.model.StatusEvent;
import com.laundryhub.util.Callback;
import java.util.List;

/**
 * Orders: placing, reading, tracking, and the admin's status/payment actions.
 * Prices always come from the database functions.
 */
public interface OrderRepository {

    /**
     * Server price for the wizard (rpc quote_order).
     *
     * @param service  service type
     * @param kg       weight
     * @param soapId   soap id
     * @param callback receives the breakdown
     */
    void quote(ServiceType service, int kg, long soapId, Callback<PriceBreakdown> callback);

    /**
     * Places the order (rpc place_order).
     *
     * @param request  wizard choices
     * @param callback receives the new order (id, number, prices)
     */
    void placeOrder(OrderRequest request, Callback<Order> callback);

    /** @param callback receives the signed-in customer's orders, newest first */
    void findMine(Callback<List<Order>> callback);

    /**
     * @param orderId  order id
     * @param callback receives the order (with customer), or an error when not visible
     */
    void findById(long orderId, Callback<Order> callback);

    /**
     * Admin order list.
     *
     * @param status   only this status, or null for all
     * @param search   order no. or customer text, or null
     * @param callback receives the orders, newest first
     */
    void findAll(OrderStatus status, String search, Callback<List<Order>> callback);

    /**
     * @param orderId  order id
     * @param callback receives the timeline, oldest first
     */
    void findHistory(long orderId, Callback<List<StatusEvent>> callback);

    /**
     * Admin/staff status change (a trigger writes the history row).
     *
     * @param orderId  order id
     * @param status   new status
     * @param callback called when saved
     */
    void updateStatus(long orderId, OrderStatus status, Callback<Void> callback);

    /**
     * Customer cancels their own order (rpc cancel_order). Allowed while PENDING and not picked up.
     *
     * @param orderId  order id
     * @param reason   reason chosen by the customer, may be null
     * @param callback called when cancelled; error 409 with a friendly message when too late
     */
    void cancelOrder(long orderId, String reason, Callback<Void> callback);

    /**
     * Marks a cash order as paid (walk-in customer paid at the counter).
     *
     * @param orderId  order id
     * @param callback called when saved
     */
    void markPaid(long orderId, Callback<Void> callback);

    /**
     * Orders created between two Philippine dates (inclusive), oldest first, for the CSV export.
     *
     * @param fromDate first day, YYYY-MM-DD
     * @param toDate   last day, YYYY-MM-DD
     * @param callback receives the orders (with customer name)
     */
    void findBetween(String fromDate, String toDate, Callback<List<Order>> callback);

    /**
     * @param orderId  order id
     * @param riderId  rider profile id, or null to unassign
     * @param callback called when saved
     */
    void assignRider(long orderId, String riderId, Callback<Void> callback);

    /**
     * @param orderId  order id
     * @param approve  true = PAID, false = REJECTED
     * @param note     optional reason
     * @param callback called when saved
     */
    void verifyGcash(long orderId, boolean approve, String note, Callback<Void> callback);

    /**
     * Uploads the GCash screenshot to gcash-proofs/&lt;user id&gt;/&lt;order no&gt;.jpg and links it to the order.
     *
     * @param order       the order
     * @param userId      signed-in customer id
     * @param browserFile File from an input element
     * @param contentType MIME type
     * @param callback    called when saved
     */
    void attachGcashProof(Order order, String userId, Object browserFile, String contentType, Callback<Void> callback);

    /**
     * @param orderId   order id
     * @param reference new 13-digit reference
     * @param callback  called when saved
     */
    void resubmitGcash(long orderId, String reference, Callback<Void> callback);

    /**
     * @param proofPath storage path of a GCash screenshot
     * @param callback  receives a temporary URL
     */
    void getProofUrl(String proofPath, Callback<String> callback);
}
