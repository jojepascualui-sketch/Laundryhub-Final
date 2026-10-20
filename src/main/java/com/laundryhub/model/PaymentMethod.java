package com.laundryhub.model;

/**
 * Shared parts of every payment method: the badge and tint on the payment card.
 *
 * <p>OOP: Abstraction + Inheritance — {@link CashPayment} and {@link GCashPayment} extend this
 * class and implement the remaining {@link Payable} methods.
 */
public abstract class PaymentMethod implements Payable {

    private final String badge;

    /**
     * @param badge one-character badge on the payment card ("₱" or "G")
     */
    protected PaymentMethod(String badge) {
        this.badge = badge;
    }

    /** @return badge text on the payment card */
    public String getBadge() {
        return badge;
    }

    /**
     * @param code CASH or GCASH
     * @return an empty payment of that kind (GCash without a reference yet), or null
     */
    public static PaymentMethod fromCode(String code) {
        if (CashPayment.CODE.equals(code)) {
            return new CashPayment();
        }
        if (GCashPayment.CODE.equals(code)) {
            return new GCashPayment(null, null);
        }
        return null;
    }

    @Override
    public String toString() {
        return getCode();
    }
}
