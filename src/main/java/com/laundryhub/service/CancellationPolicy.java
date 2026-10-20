package com.laundryhub.service;

import com.laundryhub.model.Order;
import com.laundryhub.model.PaymentStatus;
import com.laundryhub.model.OrderStatus;
import com.laundryhub.model.RiderJob;
import com.laundryhub.model.StatusEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * When the "Cancel" buttons are shown. The same rules are enforced by the database
 * (rpc cancel_order / release_job, migration 0013); this class only decides what to show.
 * Pure Java, unit tested.
 */
public final class CancellationPolicy {

    // OOP: Abstraction — pages ask "can this be cancelled?" without knowing the step numbers.
    /** Reasons offered to customers. */
    public static final List<String> CUSTOMER_REASONS = Collections.unmodifiableList(Arrays.asList(
            "Changed my mind", "Wrong address or pin", "Wrong schedule", "Ordered by mistake", "Other"));

    /** Reasons offered to riders. */
    public static final List<String> RIDER_REASONS = Collections.unmodifiableList(Arrays.asList(
            "Vehicle problem", "Can't reach the customer", "Too far / wrong route", "Emergency", "Other"));

    /** Pickup step at which the laundry is in the rider's hands ("Confirm pickup"). */
    private static final int PICKUP_TAKEN_STEP = 3;
    /** Return step at which the laundry left the shop ("Picked up at shop"). */
    private static final int RETURN_TAKEN_STEP = 1;

    private CancellationPolicy() {
    }

    /**
     * A customer may cancel while the order is still PENDING and the laundry has not been picked up.
     * (Walk-in orders move to WASHING when the shop receives them.)
     *
     * @param order  the order
     * @param events its history (looks for PICKED_UP), may be empty
     * @return true when the "Cancel order" button should be shown
     */
    public static boolean customerCanCancel(Order order, List<StatusEvent> events) {
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            return false;
        }
        if (events != null) {
            for (StatusEvent e : events) {
                if ("PICKED_UP".equals(e.getEvent())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * A rider may give a job back before taking the laundry: pickup before "Confirm pickup",
     * return delivery before "Picked up at shop".
     *
     * @param job the rider's job
     * @return true when the "Cancel job" button should be shown
     */
    public static boolean riderCanRelease(RiderJob job) {
        if (job == null || !job.getStatus().isActive()) {
            return false;
        }
        int limit = RiderJob.TYPE_PICKUP.equals(job.getType()) ? PICKUP_TAKEN_STEP : RETURN_TAKEN_STEP;
        return job.getCurrentStep() < limit;
    }

    /**
     * @param order an order the customer just cancelled
     * @return extra advice for paid GCash orders, or null
     */
    public static String refundNote(Order order) {
        return order != null && !order.isCash() && order.getPaymentStatus() == PaymentStatus.PAID
                ? "You paid by GCash. The shop will contact you about your refund."
                : null;
    }
}
