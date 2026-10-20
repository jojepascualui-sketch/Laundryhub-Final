package com.laundryhub.model;

/**
 * Order progress (orders.status). Each status has a badge style from DESIGN_SPEC.md §1.
 */
public enum OrderStatus {
    PENDING("Pending", "pending"),
    WASHING("Washing", "washing"),
    READY("Ready", "ready"),
    OUT_FOR_DELIVERY("Out for delivery", "out"),
    COMPLETED("Completed", "done"),
    CANCELLED("Cancelled", "cancelled");

    private final String label;
    private final String badgeKey;

    OrderStatus(String label, String badgeKey) {
        this.label = label;
        this.badgeKey = badgeKey;
    }

    /** @return badge text */
    public String getLabel() {
        return label;
    }

    /** @return CSS modifier for the badge colors, e.g. "washing" */
    public String getBadgeKey() {
        return badgeKey;
    }

    /** @return true when the order is finished (completed or cancelled) */
    public boolean isClosed() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * @param code database value
     * @return the status, PENDING when unknown
     */
    public static OrderStatus fromCode(String code) {
        for (OrderStatus s : values()) {
            if (s.name().equals(code)) {
                return s;
            }
        }
        return PENDING;
    }
}
