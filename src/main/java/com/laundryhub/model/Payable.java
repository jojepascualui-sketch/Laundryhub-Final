package com.laundryhub.model;

/**
 * Something the customer can pay with.
 *
 * <p>OOP: Abstraction — the order wizard and the server request only need this contract.
 */
public interface Payable {

    /** @return database code, CASH or GCASH */
    String getCode();

    /** @return display name, e.g. "GCash" */
    String getDisplayName();

    /**
     * @param service the chosen service
     * @return one-line description on the payment card
     */
    String getDescription(ServiceType service);

    /** @return payment status right after the order is placed */
    PaymentStatus getInitialStatus();

    /** @return true when the admin must check the payment before it counts as paid */
    boolean requiresVerification();
}
