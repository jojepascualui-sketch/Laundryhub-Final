package com.laundryhub.model;

import com.laundryhub.exception.ValidationException;
import java.util.List;

/**
 * One trip for a rider: picking laundry up or bringing it back.
 * {@code currentStep} is the number of steps already completed (same as the database).
 *
 * <p>OOP: Abstraction — pages show steps and the big button label without knowing the job type.
 * OOP: Polymorphism — {@link PickupJob} and {@link DeliveryJob} define their own steps.
 */
public abstract class RiderJob {

    /** Type code of a pickup job. */
    public static final String TYPE_PICKUP = "PICKUP";
    /** Type code of a return-delivery job. */
    public static final String TYPE_RETURN = "RETURN_DELIVERY";

    /** How a step is drawn in the checklist. */
    public enum StepState { DONE, CURRENT, UPCOMING }

    // OOP: Encapsulation — job progress can only change through the server (advance_job).
    private final long id;
    private final long orderId;
    private final JobStatus status;
    private final int currentStep;
    private final String riderId;
    private final boolean cashOrder;

    /**
     * @param id          job id
     * @param orderId     order id
     * @param status      job status
     * @param currentStep steps completed (0..total)
     * @param riderId     rider profile id, may be null
     * @param cashOrder   true when the order is paid in cash
     */
    protected RiderJob(long id, long orderId, JobStatus status, int currentStep, String riderId, boolean cashOrder) {
        if (currentStep < 0) {
            throw new ValidationException("Job step cannot be negative.");
        }
        this.id = id;
        this.orderId = orderId;
        this.status = status == null ? JobStatus.WAITING : status;
        this.currentStep = currentStep;
        this.riderId = riderId;
        this.cashOrder = cashOrder;
    }

    /** @return PICKUP or RETURN_DELIVERY */
    public abstract String getType();

    /** @return badge text, e.g. "Pickup" */
    public abstract String getTypeLabel();

    /** @return checklist step names, in order */
    public abstract List<String> getSteps();

    /** @return big-button label for each step, same length as {@link #getSteps()} */
    public abstract List<String> getActionLabels();

    /** @return payment strip text on the booking card */
    public abstract String getPaymentNote();

    /** @return summary line on the "Job completed" screen (R4) */
    public abstract String getDoneNote();

    /** @return job id */
    public long getId() {
        return id;
    }

    /** @return order id */
    public long getOrderId() {
        return orderId;
    }

    /** @return job status */
    public JobStatus getStatus() {
        return status;
    }

    /** @return number of completed steps */
    public int getCurrentStep() {
        return currentStep;
    }

    /** @return rider profile id, may be null */
    public String getRiderId() {
        return riderId;
    }

    /** @return true when the order is paid in cash */
    public boolean isCashOrder() {
        return cashOrder;
    }

    /** @return number of steps */
    public int getTotalSteps() {
        return getSteps().size();
    }

    /** @return true when every step is done */
    public boolean isDone() {
        return status == JobStatus.COMPLETED || currentStep >= getTotalSteps();
    }

    /** @return label of the big button for the next step, or "" when done */
    public String getNextActionLabel() {
        return isDone() ? "" : getActionLabels().get(currentStep);
    }

    /**
     * @param index step index (0-based)
     * @return how the step should be drawn
     */
    public StepState getStepState(int index) {
        if (index < currentStep) {
            return StepState.DONE;
        }
        return index == currentStep && !isDone() ? StepState.CURRENT : StepState.UPCOMING;
    }

    /**
     * Creates the right subclass for a database row.
     *
     * @param type        PICKUP or RETURN_DELIVERY
     * @param id          job id
     * @param orderId     order id
     * @param status      job status
     * @param currentStep completed steps
     * @param riderId     rider id, may be null
     * @param cashOrder   true for cash orders
     * @return a PickupJob or DeliveryJob
     */
    public static RiderJob create(String type, long id, long orderId, JobStatus status, int currentStep,
                                  String riderId, boolean cashOrder) {
        if (TYPE_PICKUP.equals(type)) {
            return new PickupJob(id, orderId, status, currentStep, riderId, cashOrder);
        }
        if (TYPE_RETURN.equals(type)) {
            return new DeliveryJob(id, orderId, status, currentStep, riderId, cashOrder);
        }
        throw new ValidationException("Unknown job type: " + type);
    }
}
