package com.laundryhub.model;

/**
 * A rider job together with its order, as shown on a booking card (screen R1).
 */
public final class Booking {

    private final RiderJob job;
    private final Order order;
    private final String completedAt;

    /**
     * @param job   the job
     * @param order the job's order (with customer name and pin)
     */
    public Booking(RiderJob job, Order order) {
        this(job, order, null);
    }

    /**
     * @param job         the job
     * @param order       the job's order (with customer name and pin)
     * @param completedAt when the job was completed (ISO), may be null
     */
    public Booking(RiderJob job, Order order, String completedAt) {
        this.job = job;
        this.order = order;
        this.completedAt = completedAt;
    }

    /** @return when the job was completed (ISO), or null */
    public String getCompletedAt() {
        return completedAt;
    }

    /** @return the job */
    public RiderJob getJob() {
        return job;
    }

    /** @return the order */
    public Order getOrder() {
        return order;
    }

    /**
     * @param riderPosition the rider's GPS position, may be null
     * @return distance in km to the customer's pin, or -1 when unknown
     */
    public double distanceKmFrom(Location riderPosition) {
        if (riderPosition == null || order.getLocation() == null) {
            return -1;
        }
        return riderPosition.distanceKmTo(order.getLocation());
    }
}
