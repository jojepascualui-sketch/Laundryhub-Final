package com.laundryhub.model;

import com.laundryhub.exception.ValidationException;

/**
 * A placed laundry order as stored in the database. Created with {@link Builder}.
 * Prices are the server's numbers (centavos), never recalculated in the browser.
 */
public final class Order {

    private final long id;
    private final String orderNo;
    private final String customerId;
    private final String customerName;
    private final String customerMobile;
    private final ServiceType serviceType;
    private final int kg;
    private final long soapId;
    private final String soapName;
    private final String timeSlot;
    private final Location location;
    private final String addressDetails;
    private final String paymentMethod;
    private final PaymentStatus paymentStatus;
    private final String gcashReference;
    private final String gcashProofPath;
    private final long ratePerKg;
    private final long soapAddOnPerKg;
    private final long laundryCost;
    private final long soapCost;
    private final long deliveryFee;
    private final long total;
    private final OrderStatus status;
    private final String riderId;
    private final String createdAt;

    private Order(Builder b) {
        if (b.orderNo == null || b.orderNo.isEmpty()) {
            throw new ValidationException("An order needs an order number.");
        }
        if (b.serviceType == null) {
            throw new ValidationException("An order needs a service type.");
        }
        this.id = b.id;
        this.orderNo = b.orderNo;
        this.customerId = b.customerId;
        this.customerName = b.customerName == null ? "" : b.customerName;
        this.customerMobile = b.customerMobile;
        this.serviceType = b.serviceType;
        this.kg = b.kg;
        this.soapId = b.soapId;
        this.soapName = b.soapName == null ? "" : b.soapName;
        this.timeSlot = b.timeSlot == null ? "" : b.timeSlot;
        this.location = b.location;
        this.addressDetails = b.addressDetails;
        this.paymentMethod = b.paymentMethod;
        this.paymentStatus = b.paymentStatus == null ? PaymentStatus.UNPAID : b.paymentStatus;
        this.gcashReference = b.gcashReference;
        this.gcashProofPath = b.gcashProofPath;
        this.ratePerKg = b.ratePerKg;
        this.soapAddOnPerKg = b.soapAddOnPerKg;
        this.laundryCost = b.laundryCost;
        this.soapCost = b.soapCost;
        this.deliveryFee = b.deliveryFee;
        this.total = b.total;
        this.status = b.status == null ? OrderStatus.PENDING : b.status;
        this.riderId = b.riderId;
        this.createdAt = b.createdAt;
    }

    /** @return database id */
    public long getId() {
        return id;
    }

    /** @return order number, e.g. LH-0001 */
    public String getOrderNo() {
        return orderNo;
    }

    /** @return customer profile id */
    public String getCustomerId() {
        return customerId;
    }

    /** @return customer name (when loaded with the order), never null */
    public String getCustomerName() {
        return customerName;
    }

    /** @return customer mobile, may be null */
    public String getCustomerMobile() {
        return customerMobile;
    }

    /** @return service type */
    public ServiceType getServiceType() {
        return serviceType;
    }

    /** @return charged weight in kg */
    public int getKg() {
        return kg;
    }

    /** @return soap id */
    public long getSoapId() {
        return soapId;
    }

    /** @return soap name at order time */
    public String getSoapName() {
        return soapName;
    }

    /** @return time slot label */
    public String getTimeSlot() {
        return timeSlot;
    }

    /** @return customer pin, null for walk-in */
    public Location getLocation() {
        return location;
    }

    /** @return extra address details typed by the customer, may be null */
    public String getAddressDetails() {
        return addressDetails;
    }

    /** @return CASH or GCASH */
    public String getPaymentMethod() {
        return paymentMethod;
    }

    /** @return true when paid with cash */
    public boolean isCash() {
        return CashPayment.CODE.equals(paymentMethod);
    }

    /** @return "Cash" or "GCash" */
    public String getPaymentLabel() {
        return isCash() ? "Cash" : "GCash";
    }

    /**
     * Payment line for the customer's order list, e.g. "GCash · Waiting for verification",
     * "Cash · Pay the rider upon delivery" or "GCash · Paid".
     *
     * @return the label
     */
    public String getCustomerPaymentLabel() {
        if (paymentStatus == PaymentStatus.PAID) {
            return getPaymentLabel() + " · Paid";
        }
        if (isCash()) {
            return "Cash · " + serviceType.getCashDescription();
        }
        return "GCash · " + paymentStatus.getCustomerLabel();
    }

    /** @return payment status */
    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    /** @return GCash reference, may be null */
    public String getGcashReference() {
        return gcashReference;
    }

    /** @return storage path of the GCash screenshot, may be null */
    public String getGcashProofPath() {
        return gcashProofPath;
    }

    /** @return price per kg used */
    public long getRatePerKg() {
        return ratePerKg;
    }

    /** @return soap add-on per kg used */
    public long getSoapAddOnPerKg() {
        return soapAddOnPerKg;
    }

    /** @return laundry line in centavos */
    public long getLaundryCost() {
        return laundryCost;
    }

    /** @return soap line in centavos */
    public long getSoapCost() {
        return soapCost;
    }

    /** @return delivery fee in centavos */
    public long getDeliveryFee() {
        return deliveryFee;
    }

    /** @return total in centavos */
    public long getTotal() {
        return total;
    }

    /** @return the server's price lines as a breakdown */
    public PriceBreakdown getPriceBreakdown() {
        return new PriceBreakdown(kg, ratePerKg, soapAddOnPerKg, laundryCost, soapCost, deliveryFee);
    }

    /** @return order status */
    public OrderStatus getStatus() {
        return status;
    }

    /** @return assigned rider profile id, may be null */
    public String getRiderId() {
        return riderId;
    }

    /** @return creation time (ISO text from the database), may be null */
    public String getCreatedAt() {
        return createdAt;
    }

    /** @return true for pickup &amp; delivery orders */
    public boolean isDelivery() {
        return serviceType.requiresLocation();
    }

    /**
     * Fluent builder, because an order has many fields.
     */
    public static final class Builder {
        private long id;
        private String orderNo;
        private String customerId;
        private String customerName;
        private String customerMobile;
        private ServiceType serviceType;
        private int kg;
        private long soapId;
        private String soapName;
        private String timeSlot;
        private Location location;
        private String addressDetails;
        private String paymentMethod;
        private PaymentStatus paymentStatus;
        private String gcashReference;
        private String gcashProofPath;
        private long ratePerKg;
        private long soapAddOnPerKg;
        private long laundryCost;
        private long soapCost;
        private long deliveryFee;
        private long total;
        private OrderStatus status;
        private String riderId;
        private String createdAt;

        /** @param v id @return this */
        public Builder id(long v) { id = v; return this; }
        /** @param v order number @return this */
        public Builder orderNo(String v) { orderNo = v; return this; }
        /** @param v customer id @return this */
        public Builder customerId(String v) { customerId = v; return this; }
        /** @param v customer name @return this */
        public Builder customerName(String v) { customerName = v; return this; }
        /** @param v customer mobile @return this */
        public Builder customerMobile(String v) { customerMobile = v; return this; }
        /** @param v service type @return this */
        public Builder serviceType(ServiceType v) { serviceType = v; return this; }
        /** @param v charged kg @return this */
        public Builder kg(int v) { kg = v; return this; }
        /** @param v soap id @return this */
        public Builder soapId(long v) { soapId = v; return this; }
        /** @param v soap name @return this */
        public Builder soapName(String v) { soapName = v; return this; }
        /** @param v time slot @return this */
        public Builder timeSlot(String v) { timeSlot = v; return this; }
        /** @param v pin @return this */
        public Builder location(Location v) { location = v; return this; }
        /** @param v address details @return this */
        public Builder addressDetails(String v) { addressDetails = v; return this; }
        /** @param v CASH or GCASH @return this */
        public Builder paymentMethod(String v) { paymentMethod = v; return this; }
        /** @param v payment status @return this */
        public Builder paymentStatus(PaymentStatus v) { paymentStatus = v; return this; }
        /** @param v GCash reference @return this */
        public Builder gcashReference(String v) { gcashReference = v; return this; }
        /** @param v proof path @return this */
        public Builder gcashProofPath(String v) { gcashProofPath = v; return this; }
        /** @param v rate per kg @return this */
        public Builder ratePerKg(long v) { ratePerKg = v; return this; }
        /** @param v soap add-on per kg @return this */
        public Builder soapAddOnPerKg(long v) { soapAddOnPerKg = v; return this; }
        /** @param v laundry cost @return this */
        public Builder laundryCost(long v) { laundryCost = v; return this; }
        /** @param v soap cost @return this */
        public Builder soapCost(long v) { soapCost = v; return this; }
        /** @param v delivery fee @return this */
        public Builder deliveryFee(long v) { deliveryFee = v; return this; }
        /** @param v total @return this */
        public Builder total(long v) { total = v; return this; }
        /** @param v status @return this */
        public Builder status(OrderStatus v) { status = v; return this; }
        /** @param v rider id @return this */
        public Builder riderId(String v) { riderId = v; return this; }
        /** @param v created at @return this */
        public Builder createdAt(String v) { createdAt = v; return this; }

        /** @return the order */
        public Order build() {
            return new Order(this);
        }
    }
}
