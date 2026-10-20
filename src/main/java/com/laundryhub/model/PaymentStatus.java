package com.laundryhub.model;

/**
 * Payment state of an order (orders.payment_status).
 */
public enum PaymentStatus {
    UNPAID("Unpaid", "unpaid"),
    PENDING_VERIFICATION("Waiting for verification", "unpaid"),
    PAID("Paid", "paid"),
    REJECTED("Rejected — please resend", "unpaid");

    private final String customerLabel;
    private final String shortLabel;

    PaymentStatus(String customerLabel, String shortLabel) {
        this.customerLabel = customerLabel;
        this.shortLabel = shortLabel;
    }

    /** @return text for the customer's order list */
    public String getCustomerLabel() {
        return customerLabel;
    }

    /** @return "paid" or "unpaid", as in "Total · GCash (paid)" */
    public String getShortLabel() {
        return shortLabel;
    }

    /**
     * @param code database value
     * @return the status, UNPAID when unknown
     */
    public static PaymentStatus fromCode(String code) {
        for (PaymentStatus s : values()) {
            if (s.name().equals(code)) {
                return s;
            }
        }
        return UNPAID;
    }
}
