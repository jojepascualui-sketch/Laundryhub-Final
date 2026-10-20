package com.laundryhub.model;

import com.laundryhub.exception.ValidationException;
import com.laundryhub.util.Validators;

/**
 * Everything the customer chose in the order wizard, checked before it is sent to
 * rpc place_order (which checks again and sets the price).
 */
public final class OrderRequest {

    private final ServiceType service;
    private final LaundryLoad load;
    private final String timeSlot;
    private final Location location;
    private final String addressDetails;
    private final PaymentMethod payment;

    /**
     * @param service        service type
     * @param load           weight and soap
     * @param timeSlot       chosen time slot
     * @param location       pin (required for delivery, ignored for walk-in)
     * @param addressDetails house no., street, landmark (delivery), may be null
     * @param payment        cash or a complete GCash payment
     * @throws ValidationException when something is missing
     */
    public OrderRequest(ServiceType service, LaundryLoad load, String timeSlot, Location location,
                        String addressDetails, PaymentMethod payment) {
        if (service == null) {
            throw new ValidationException("Choose a service.");
        }
        if (load == null) {
            throw new ValidationException("Enter the laundry weight and soap.");
        }
        if (!Validators.hasText(timeSlot)) {
            throw new ValidationException("Choose a time slot.");
        }
        if (service.requiresLocation() && location == null) {
            throw new ValidationException("Pin your location on the map.");
        }
        if (payment == null) {
            throw new ValidationException("Choose a payment method.");
        }
        if (payment instanceof GCashPayment && !((GCashPayment) payment).isComplete()) {
            throw new ValidationException("Enter the 13-digit GCash reference number.");
        }
        this.service = service;
        this.load = load;
        this.timeSlot = timeSlot.trim();
        this.location = service.requiresLocation() ? location : null;
        this.addressDetails = service.requiresLocation() && Validators.hasText(addressDetails) ? addressDetails.trim() : null;
        this.payment = payment;
    }

    /** @return service type */
    public ServiceType getService() {
        return service;
    }

    /** @return weight and soap */
    public LaundryLoad getLoad() {
        return load;
    }

    /** @return time slot */
    public String getTimeSlot() {
        return timeSlot;
    }

    /** @return pin, null for walk-in */
    public Location getLocation() {
        return location;
    }

    /** @return address details, may be null */
    public String getAddressDetails() {
        return addressDetails;
    }

    /** @return payment method */
    public PaymentMethod getPayment() {
        return payment;
    }
}
