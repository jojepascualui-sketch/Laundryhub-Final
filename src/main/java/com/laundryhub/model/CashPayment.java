package com.laundryhub.model;

/**
 * Cash, paid to the rider on delivery or at the counter.
 *
 * <p>OOP: Inheritance + Polymorphism — its description depends on the service.
 */
public final class CashPayment extends PaymentMethod {

    /** Database code. */
    public static final String CODE = "CASH";

    /** Creates a cash payment. */
    public CashPayment() {
        super("₱");
    }

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getDisplayName() {
        return "Cash";
    }

    @Override
    public String getDescription(ServiceType service) {
        return service == null ? "Pay at the counter" : service.getCashDescription();
    }

    @Override
    public PaymentStatus getInitialStatus() {
        return PaymentStatus.UNPAID;
    }

    @Override
    public boolean requiresVerification() {
        return false;
    }
}
