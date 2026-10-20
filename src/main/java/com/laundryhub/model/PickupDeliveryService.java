package com.laundryhub.model;

import com.laundryhub.util.MoneyFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A rider picks up the laundry at the customer's pin and returns it. Adds the delivery fee.
 *
 * <p>OOP: Inheritance — a concrete ServiceType.
 */
public final class PickupDeliveryService extends ServiceType {

    /** Database code. */
    public static final String CODE = "PICKUP_DELIVERY";
    /** The single instance (services have no state). */
    public static final PickupDeliveryService INSTANCE = new PickupDeliveryService();

    private static final List<String> SLOTS = Collections.unmodifiableList(Arrays.asList(
            "Today, 2–4 PM", "Today, 4–6 PM", "Tomorrow, 9–11 AM", "Tomorrow, 1–3 PM"));

    private PickupDeliveryService() {
    }

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getDisplayName() {
        return "Pickup & Delivery";
    }

    @Override
    public String getDescription() {
        return "Our rider collects it at your pinned location and returns it clean.";
    }

    @Override
    public long getServiceFee(ShopSettings settings) {
        return settings.getDeliveryFee();
    }

    @Override
    public String getFeeLabel(ShopSettings settings) {
        return "+ " + MoneyFormatter.formatShort(settings.getDeliveryFee()) + " delivery fee";
    }

    @Override
    public boolean requiresLocation() {
        return true;
    }

    @Override
    public String getScheduleStepTitle() {
        return "Pin your location";
    }

    @Override
    public String getSlotTitle() {
        return "Pickup time";
    }

    @Override
    public List<String> getTimeSlots() {
        return SLOTS;
    }

    @Override
    public String getCashDescription() {
        return "Pay the rider upon delivery";
    }
}
