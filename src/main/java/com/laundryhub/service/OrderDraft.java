package com.laundryhub.service;

import com.laundryhub.exception.ValidationException;
import com.laundryhub.model.CashPayment;
import com.laundryhub.model.GCashPayment;
import com.laundryhub.model.LaundryLoad;
import com.laundryhub.model.Location;
import com.laundryhub.model.OrderRequest;
import com.laundryhub.model.PaymentMethod;
import com.laundryhub.model.ServiceType;
import com.laundryhub.model.ShopSettings;
import com.laundryhub.model.Soap;
import com.laundryhub.util.Validators;

/**
 * The order wizard's state while the customer taps through the 4 steps.
 * Pure Java, so the step rules ("Continue is disabled until…") are unit tested.
 *
 * <p>OOP: Encapsulation — fields change only through setters that keep the draft consistent
 * (e.g. changing the service clears the time slot, because the slots differ).
 */
public final class OrderDraft {

    /** Step 1: Choose service. */
    public static final int STEP_SERVICE = 1;
    /** Step 2: Weight &amp; fabric soap. */
    public static final int STEP_LOAD = 2;
    /** Step 3: Pin location / Drop-off schedule. */
    public static final int STEP_SCHEDULE = 3;
    /** Step 4: Review &amp; pay. */
    public static final int STEP_REVIEW = 4;
    /** Number of steps. */
    public static final int TOTAL_STEPS = 4;

    private ServiceType service;
    private int kg = LaundryLoad.MIN_KG;
    private Soap soap;
    private String slot;
    private Location pin;
    private String addressDetails = "";
    private String paymentCode = CashPayment.CODE;
    private String gcashReference = "";
    private String gcashNumber = "";

    /** @return chosen service, or null */
    public ServiceType getService() {
        return service;
    }

    /** @param value new service; a different service clears the time slot (and the pin for walk-in) */
    public void setService(ServiceType value) {
        if (value != service) {
            slot = null;
            if (value != null && !value.requiresLocation()) {
                pin = null;
            }
        }
        service = value;
    }

    /** @return weight in kg */
    public int getKg() {
        return kg;
    }

    /**
     * @param value weight; values below 1 are ignored
     */
    public void setKg(int value) {
        if (value > 0 && value <= LaundryLoad.MAX_KG) {
            kg = value;
        }
    }

    /** @return chosen soap, or null */
    public Soap getSoap() {
        return soap;
    }

    /** @param value chosen soap */
    public void setSoap(Soap value) {
        soap = value;
    }

    /** @return chosen time slot, or null */
    public String getSlot() {
        return slot;
    }

    /** @param value time slot label */
    public void setSlot(String value) {
        slot = value;
    }

    /** @return pinned location, or null */
    public Location getPin() {
        return pin;
    }

    /** @param value pinned location */
    public void setPin(Location value) {
        pin = value;
    }

    /** @return extra address details, never null */
    public String getAddressDetails() {
        return addressDetails;
    }

    /** @param value extra address details */
    public void setAddressDetails(String value) {
        addressDetails = value == null ? "" : value;
    }

    /** @return CASH or GCASH */
    public String getPaymentCode() {
        return paymentCode;
    }

    /** @param code CASH or GCASH */
    public void setPaymentCode(String code) {
        paymentCode = GCashPayment.CODE.equals(code) ? GCashPayment.CODE : CashPayment.CODE;
    }

    /** @return true when GCash is chosen */
    public boolean isGcash() {
        return GCashPayment.CODE.equals(paymentCode);
    }

    /** @return GCash reference as typed */
    public String getGcashReference() {
        return gcashReference;
    }

    /** @param value GCash reference as typed */
    public void setGcashReference(String value) {
        gcashReference = value == null ? "" : value;
    }

    /** @return customer's GCash number as typed */
    public String getGcashNumber() {
        return gcashNumber;
    }

    /** @param value customer's GCash number as typed */
    public void setGcashNumber(String value) {
        gcashNumber = value == null ? "" : value;
    }

    /**
     * Whether the pin is inside the delivery area (always true when there is no pin or no shop pin).
     *
     * @param settings shop settings
     * @return true when the pin may be used
     */
    public boolean isPinInArea(ShopSettings settings) {
        return pin == null || settings == null || settings.isWithinDeliveryArea(pin);
    }

    /**
     * The rule behind the disabled Continue button.
     *
     * @param step     1..4
     * @param settings shop settings (for the delivery radius), may be null
     * @return true when the step is complete
     */
    public boolean isStepValid(int step, ShopSettings settings) {
        switch (step) {
            case STEP_SERVICE:
                return service != null;
            case STEP_LOAD:
                return kg > 0 && soap != null;
            case STEP_SCHEDULE:
                if (slot == null) {
                    return false;
                }
                return service == null || !service.requiresLocation() || (pin != null && isPinInArea(settings));
            case STEP_REVIEW:
                if (!isGcash()) {
                    return true;
                }
                return Validators.isGcashReference(gcashReference)
                        && (!Validators.hasText(gcashNumber) || Validators.isMobile(gcashNumber));
            default:
                return false;
        }
    }

    /**
     * @param settings shop settings, may be null
     * @return the first step that is not complete (TOTAL_STEPS + 1 when all are)
     */
    public int firstIncompleteStep(ShopSettings settings) {
        for (int step = STEP_SERVICE; step <= TOTAL_STEPS; step++) {
            if (!isStepValid(step, settings)) {
                return step;
            }
        }
        return TOTAL_STEPS + 1;
    }

    /**
     * @param step 1..4
     * @return the title in the wizard header
     */
    public String stepTitle(int step) {
        switch (step) {
            case STEP_SERVICE:
                return "Choose service";
            case STEP_LOAD:
                return "Weight & fabric soap";
            case STEP_SCHEDULE:
                return service == null ? "Schedule" : service.getScheduleStepTitle();
            default:
                return "Review & pay";
        }
    }

    /**
     * Live "Estimated total": kg × (rate + soap add-on) + fee of the chosen service.
     *
     * @param calculator price calculator with current settings
     * @return total in centavos
     */
    public long estimateTotal(PriceCalculator calculator) {
        LaundryLoad load = soap == null ? null : new LaundryLoad(kg, soap);
        return calculator.estimateTotal(service, load, kg);
    }

    /**
     * Builds the request for rpc place_order.
     *
     * @return the request
     * @throws ValidationException when something is missing or invalid
     */
    public OrderRequest toRequest() {
        PaymentMethod payment = isGcash()
                ? new GCashPayment(gcashReference, gcashNumber)
                : new CashPayment();
        return new OrderRequest(service, new LaundryLoad(kg, soap), slot, pin, addressDetails, payment);
    }

    /** Starts a new order (keeps nothing). */
    public void reset() {
        service = null;
        kg = LaundryLoad.MIN_KG;
        soap = null;
        slot = null;
        pin = null;
        addressDetails = "";
        paymentCode = CashPayment.CODE;
        gcashReference = "";
        gcashNumber = "";
    }
}
