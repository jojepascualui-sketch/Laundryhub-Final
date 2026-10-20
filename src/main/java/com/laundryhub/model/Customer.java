package com.laundryhub.model;

/**
 * A customer who places laundry orders.
 *
 * <p>OOP: Inheritance — a Customer is a User.
 */
public class Customer extends User {

    /** Hash route of the customer home page. */
    public static final String HOME_ROUTE = "#/home";

    /**
     * @param id       profile id
     * @param fullName full name
     * @param email    e-mail
     * @param mobile   mobile number
     * @param active   account status
     */
    public Customer(String id, String fullName, String email, String mobile, boolean active) {
        super(id, fullName, email, mobile, active);
    }

    @Override
    public Role getRole() {
        return Role.CUSTOMER;
    }

    @Override
    public String getHomeRoute() {
        return HOME_ROUTE;
    }

    @Override
    public String getRoleLabel() {
        return "Customer";
    }
}
