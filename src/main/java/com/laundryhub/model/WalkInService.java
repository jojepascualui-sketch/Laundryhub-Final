package com.laundryhub.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The customer brings the laundry to the shop. No extra fee.
 *
 * <p>OOP: Inheritance — a concrete ServiceType.
 */
public final class WalkInService extends ServiceType {

    /** Database code. */
    public static final String CODE = "WALK_IN";
    /** The single instance (services have no state). */
    public static final WalkInService INSTANCE = new WalkInService();

    private static final List<String> SLOTS = Collections.unmodifiableList(Arrays.asList(
            "Today, morning", "Today, afternoon", "Tomorrow, morning", "Tomorrow, afternoon"));

    private WalkInService() {
    }

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getDisplayName() {
        return "Walk-in";
    }

    @Override
    public String getDescription() {
        return "Bring your laundry to the shop and pick it up when it is ready.";
    }

    @Override
    public long getServiceFee(ShopSettings settings) {
        return 0;
    }

    @Override
    public String getFeeLabel(ShopSettings settings) {
        return "No extra fee";
    }

    @Override
    public boolean requiresLocation() {
        return false;
    }

    @Override
    public String getScheduleStepTitle() {
        return "Drop-off schedule";
    }

    @Override
    public String getSlotTitle() {
        return "Drop-off time";
    }

    @Override
    public List<String> getTimeSlots() {
        return SLOTS;
    }

    @Override
    public String getCashDescription() {
        return "Pay at the counter";
    }
}
