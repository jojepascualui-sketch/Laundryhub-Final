package com.laundryhub.model;

import com.laundryhub.exception.InvalidPaymentException;
import com.laundryhub.util.Validators;

/**
 * GCash: the customer sends the amount to the shop's GCash number and enters the
 * 13-digit reference number; the admin verifies it.
 *
 * <p>OOP: Inheritance + Encapsulation — the reference is validated in the constructor.
 */
public final class GCashPayment extends PaymentMethod {

    /** Database code. */
    public static final String CODE = "GCASH";

    private final String reference;
    private final String senderNumber;

    /**
     * @param reference    13-digit reference (spaces allowed), or null while not entered yet
     * @param senderNumber customer's GCash number 09XXXXXXXXX, may be null
     * @throws InvalidPaymentException when a given value has the wrong format
     */
    public GCashPayment(String reference, String senderNumber) {
        super("G");
        if (reference != null && !Validators.isGcashReference(reference)) {
            throw new InvalidPaymentException("Enter the 13-digit GCash reference number.");
        }
        if (Validators.hasText(senderNumber) && !Validators.isMobile(senderNumber)) {
            throw new InvalidPaymentException("Enter your GCash number as 09XXXXXXXXX.");
        }
        this.reference = reference == null ? null : Validators.compact(reference);
        this.senderNumber = Validators.hasText(senderNumber) ? Validators.compact(senderNumber) : null;
    }

    /** @return the 13-digit reference without spaces, or null */
    public String getReference() {
        return reference;
    }

    /** @return the customer's GCash number, or null */
    public String getSenderNumber() {
        return senderNumber;
    }

    /** @return true when the reference has been entered (required to place the order) */
    public boolean isComplete() {
        return reference != null;
    }

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getDisplayName() {
        return "GCash";
    }

    @Override
    public String getDescription(ServiceType service) {
        return "Pay instantly from your e-wallet";
    }

    @Override
    public PaymentStatus getInitialStatus() {
        return PaymentStatus.PENDING_VERIFICATION;
    }

    @Override
    public boolean requiresVerification() {
        return true;
    }
}
