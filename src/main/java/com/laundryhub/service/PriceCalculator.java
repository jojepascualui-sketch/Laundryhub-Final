package com.laundryhub.service;

import com.laundryhub.exception.ValidationException;
import com.laundryhub.model.LaundryLoad;
import com.laundryhub.model.PriceBreakdown;
import com.laundryhub.model.ServiceType;
import com.laundryhub.model.ShopSettings;

/**
 * Live price estimate for the order wizard:
 * price = charged kg × rate + charged kg × soap add-on + service fee.
 *
 * <p>The database (rpc quote_order / place_order) recalculates the real price; this class only
 * makes the "Estimated total" update instantly while the customer taps.
 */
public final class PriceCalculator {

    private final ShopSettings settings;

    /**
     * @param settings current shop settings, required
     */
    public PriceCalculator(ShopSettings settings) {
        if (settings == null) {
            throw new ValidationException("Shop settings are not loaded yet.");
        }
        this.settings = settings;
    }

    /** @return the settings used for prices */
    public ShopSettings getSettings() {
        return settings;
    }

    /**
     * @param service service type, required
     * @param load    weight and soap, required
     * @return the price lines
     */
    public PriceBreakdown quote(ServiceType service, LaundryLoad load) {
        if (service == null) {
            throw new ValidationException("Choose a service.");
        }
        if (load == null) {
            throw new ValidationException("Enter the laundry weight.");
        }
        int kg = load.getChargedKg(settings.getMinKg());
        long rate = settings.getRatePerKg();
        long addOn = load.getSoap().getAddOnPerKg();
        // OOP: Polymorphism — each ServiceType decides its own fee.
        long fee = service.getServiceFee(settings);
        return new PriceBreakdown(kg, rate, addOn, kg * rate, kg * addOn, fee);
    }

    /**
     * Estimate for a partly filled wizard: missing parts count as zero.
     *
     * @param service service type, may be null
     * @param load    weight and soap, may be null
     * @param kg      weight when no load is built yet
     * @return estimated total in centavos
     */
    public long estimateTotal(ServiceType service, LaundryLoad load, int kg) {
        if (load != null && service != null) {
            return quote(service, load).getTotal();
        }
        int charged = Math.max(kg, settings.getMinKg());
        long total = charged * settings.getRatePerKg();
        if (load != null) {
            total = load.getChargedKg(settings.getMinKg()) * (settings.getRatePerKg() + load.getSoap().getAddOnPerKg());
        }
        if (service != null) {
            total += service.getServiceFee(settings);
        }
        return total;
    }
}
