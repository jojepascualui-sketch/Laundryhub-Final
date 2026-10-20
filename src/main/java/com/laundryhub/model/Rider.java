package com.laundryhub.model;

/**
 * A rider who accepts pickup and return-delivery jobs.
 *
 * <p>OOP: Inheritance — a Rider is a User with a vehicle and a duty switch.
 */
public class Rider extends User {

    /** Hash route of the rider home page. */
    public static final String HOME_ROUTE = "#/rider";

    private final String plateNo;
    private final boolean onDuty;

    /**
     * @param id       profile id
     * @param fullName full name
     * @param email    e-mail
     * @param mobile   mobile number
     * @param active   account status
     * @param plateNo  vehicle plate number, may be null
     * @param onDuty   true when receiving bookings
     */
    public Rider(String id, String fullName, String email, String mobile, boolean active,
                 String plateNo, boolean onDuty) {
        super(id, fullName, email, mobile, active);
        this.plateNo = plateNo;
        this.onDuty = onDuty;
    }

    /** @return plate number, may be null */
    public String getPlateNo() {
        return plateNo;
    }

    /** @return true when on duty */
    public boolean isOnDuty() {
        return onDuty;
    }

    /**
     * @param duty new duty state
     * @return a copy with the new duty state
     */
    public Rider withOnDuty(boolean duty) {
        return new Rider(getId(), getFullName(), getEmail(), getMobile(), isActive(), plateNo, duty);
    }

    /**
     * @param plate plate number from the riders table
     * @param duty  duty state from the riders table
     * @return a copy with the vehicle details filled in
     */
    public Rider withVehicle(String plate, boolean duty) {
        return new Rider(getId(), getFullName(), getEmail(), getMobile(), isActive(), plate, duty);
    }

    @Override
    public Role getRole() {
        return Role.RIDER;
    }

    @Override
    public String getHomeRoute() {
        return HOME_ROUTE;
    }

    @Override
    public String getRoleLabel() {
        return "Rider";
    }
}
