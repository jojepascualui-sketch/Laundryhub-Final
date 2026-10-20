package com.laundryhub.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * How the laundry reaches the shop. The texts come from the design (screens 02, 04a, 04b, 05).
 *
 * <p>OOP: Abstraction — the wizard asks a ServiceType for its fee, slots and texts without
 * knowing which one it is. OOP: Polymorphism — {@link WalkInService} and
 * {@link PickupDeliveryService} answer differently.
 */
public abstract class ServiceType {

    /** @return database code, e.g. WALK_IN */
    public abstract String getCode();

    /** @return display name, e.g. "Walk-in" */
    public abstract String getDisplayName();

    /** @return description on the service card */
    public abstract String getDescription();

    /**
     * @param settings shop settings
     * @return extra fee for this service in centavos
     */
    public abstract long getServiceFee(ShopSettings settings);

    /**
     * @param settings shop settings
     * @return fee line on the service card, e.g. "No extra fee"
     */
    public abstract String getFeeLabel(ShopSettings settings);

    /** @return true when the customer must pin a location on the map */
    public abstract boolean requiresLocation();

    /** @return title of wizard step 3 */
    public abstract String getScheduleStepTitle();

    /** @return heading above the time chips */
    public abstract String getSlotTitle();

    /** @return the four time slot labels */
    public abstract List<String> getTimeSlots();

    /** @return description of cash payment for this service */
    public abstract String getCashDescription();

    /**
     * @param code database code
     * @return the service, or null when unknown
     */
    public static ServiceType fromCode(String code) {
        if (WalkInService.CODE.equals(code)) {
            return WalkInService.INSTANCE;
        }
        if (PickupDeliveryService.CODE.equals(code)) {
            return PickupDeliveryService.INSTANCE;
        }
        return null;
    }

    /** @return both services in display order */
    public static List<ServiceType> all() {
        return Collections.unmodifiableList(Arrays.asList(WalkInService.INSTANCE, PickupDeliveryService.INSTANCE));
    }

    @Override
    public String toString() {
        return getCode();
    }
}
