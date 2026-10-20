package com.laundryhub.model;

/**
 * One row of order_status_history (used for the tracking timeline).
 */
public final class StatusEvent {

    private final String event;
    private final OrderStatus status;
    private final String note;
    private final String createdAt;

    /**
     * @param event     event code, e.g. PLACED, PICKED_UP, WASHING
     * @param status    order status after the event
     * @param note      optional note
     * @param createdAt ISO time
     */
    public StatusEvent(String event, OrderStatus status, String note, String createdAt) {
        this.event = event == null ? "" : event;
        this.status = status;
        this.note = note;
        this.createdAt = createdAt;
    }

    /** @return event code */
    public String getEvent() {
        return event;
    }

    /** @return order status after the event */
    public OrderStatus getStatus() {
        return status;
    }

    /** @return note, may be null */
    public String getNote() {
        return note;
    }

    /** @return ISO time */
    public String getCreatedAt() {
        return createdAt;
    }
}
