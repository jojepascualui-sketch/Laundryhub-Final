package com.laundryhub.model;

/**
 * Lines of the order summary (screen 05), all in centavos.
 */
public final class PriceBreakdown {

    private final int chargedKg;
    private final long ratePerKg;
    private final long soapAddOnPerKg;
    private final long laundryCost;
    private final long soapCost;
    private final long deliveryFee;

    /**
     * @param chargedKg      charged weight
     * @param ratePerKg      price per kg
     * @param soapAddOnPerKg soap add-on per kg
     * @param laundryCost    chargedKg × ratePerKg
     * @param soapCost       chargedKg × soapAddOnPerKg
     * @param deliveryFee    delivery fee (0 for walk-in)
     */
    public PriceBreakdown(int chargedKg, long ratePerKg, long soapAddOnPerKg,
                          long laundryCost, long soapCost, long deliveryFee) {
        this.chargedKg = chargedKg;
        this.ratePerKg = ratePerKg;
        this.soapAddOnPerKg = soapAddOnPerKg;
        this.laundryCost = laundryCost;
        this.soapCost = soapCost;
        this.deliveryFee = deliveryFee;
    }

    /** @return charged weight in kg */
    public int getChargedKg() {
        return chargedKg;
    }

    /** @return price per kg */
    public long getRatePerKg() {
        return ratePerKg;
    }

    /** @return soap add-on per kg */
    public long getSoapAddOnPerKg() {
        return soapAddOnPerKg;
    }

    /** @return laundry line */
    public long getLaundryCost() {
        return laundryCost;
    }

    /** @return soap line */
    public long getSoapCost() {
        return soapCost;
    }

    /** @return delivery fee line */
    public long getDeliveryFee() {
        return deliveryFee;
    }

    /** @return sum of all lines */
    public long getTotal() {
        return laundryCost + soapCost + deliveryFee;
    }
}
