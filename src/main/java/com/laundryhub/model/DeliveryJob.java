package com.laundryhub.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Bring the clean laundry back to the customer (screen R3). Cash orders get an extra
 * "Cash collected" step before "Delivered".
 *
 * <p>OOP: Inheritance + Polymorphism — the steps depend on the payment method.
 */
public final class DeliveryJob extends RiderJob {

    private static final List<String> STEPS_CASH = Collections.unmodifiableList(Arrays.asList(
            "Collect laundry at shop", "On the way to customer", "Arrived at drop-off", "Cash collected", "Delivered"));
    private static final List<String> STEPS_GCASH = Collections.unmodifiableList(Arrays.asList(
            "Collect laundry at shop", "On the way to customer", "Arrived at drop-off", "Delivered"));
    private static final List<String> ACTIONS_CASH = Collections.unmodifiableList(Arrays.asList(
            "Picked up at shop", "Start trip", "I’ve arrived", "Cash received", "Mark as delivered"));
    private static final List<String> ACTIONS_GCASH = Collections.unmodifiableList(Arrays.asList(
            "Picked up at shop", "Start trip", "I’ve arrived", "Mark as delivered"));

    /**
     * @param id          job id
     * @param orderId     order id
     * @param status      job status
     * @param currentStep completed steps
     * @param riderId     rider id, may be null
     * @param cashOrder   true for cash orders
     */
    public DeliveryJob(long id, long orderId, JobStatus status, int currentStep, String riderId, boolean cashOrder) {
        super(id, orderId, status, currentStep, riderId, cashOrder);
    }

    @Override
    public String getType() {
        return TYPE_RETURN;
    }

    @Override
    public String getTypeLabel() {
        return "Return delivery";
    }

    @Override
    public List<String> getSteps() {
        return isCashOrder() ? STEPS_CASH : STEPS_GCASH;
    }

    @Override
    public List<String> getActionLabels() {
        return isCashOrder() ? ACTIONS_CASH : ACTIONS_GCASH;
    }

    @Override
    public String getPaymentNote() {
        return isCashOrder() ? "Collect cash" : "Paid via GCash";
    }

    @Override
    public String getDoneNote() {
        return isCashOrder() ? "Cash collected" : "Paid via GCash";
    }
}
