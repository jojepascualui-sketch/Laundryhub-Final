package com.laundryhub.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Collect the laundry at the customer's pin and bring it to the shop (screen R2).
 *
 * <p>OOP: Inheritance — a RiderJob with four fixed steps.
 */
public final class PickupJob extends RiderJob {

    private static final List<String> STEPS = Collections.unmodifiableList(Arrays.asList(
            "Head to customer", "Arrived at pickup point", "Laundry picked up & weighed", "Dropped off at shop"));
    private static final List<String> ACTIONS = Collections.unmodifiableList(Arrays.asList(
            "Start trip", "I’ve arrived", "Confirm pickup", "Drop off at shop"));

    /**
     * @param id          job id
     * @param orderId     order id
     * @param status      job status
     * @param currentStep completed steps
     * @param riderId     rider id, may be null
     * @param cashOrder   true for cash orders
     */
    public PickupJob(long id, long orderId, JobStatus status, int currentStep, String riderId, boolean cashOrder) {
        super(id, orderId, status, currentStep, riderId, cashOrder);
    }

    @Override
    public String getType() {
        return TYPE_PICKUP;
    }

    @Override
    public String getTypeLabel() {
        return "Pickup";
    }

    @Override
    public List<String> getSteps() {
        return STEPS;
    }

    @Override
    public List<String> getActionLabels() {
        return ACTIONS;
    }

    @Override
    public String getPaymentNote() {
        return isCashOrder() ? "Cash on return · order total" : "GCash · order total";
    }

    @Override
    public String getDoneNote() {
        return "Laundry dropped at shop";
    }
}
