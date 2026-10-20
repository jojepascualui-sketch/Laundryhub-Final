package com.laundryhub.model;

/**
 * The shop owner/administrator: manages orders, prices, soaps and accounts.
 *
 * <p>OOP: Inheritance — an Admin is a User; {@link Staff} extends Admin with fewer rights.
 */
public class Admin extends User {

    /** Hash route of the admin console. */
    public static final String HOME_ROUTE = "#/admin";

    private final String jobTitle;

    /**
     * @param id       profile id
     * @param fullName full name
     * @param email    e-mail
     * @param mobile   mobile number
     * @param active   account status
     * @param jobTitle job title shown in the console, e.g. "Owner"; may be null
     */
    public Admin(String id, String fullName, String email, String mobile, boolean active, String jobTitle) {
        super(id, fullName, email, mobile, active);
        this.jobTitle = jobTitle;
    }

    /** @return job title, may be null */
    public String getJobTitle() {
        return jobTitle;
    }

    /** @return true when this account may change prices, soaps and roles */
    public boolean canManageSettings() {
        return true;
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public String getHomeRoute() {
        return HOME_ROUTE;
    }

    @Override
    public String getRoleLabel() {
        return jobTitle == null || jobTitle.isEmpty() ? "Admin" : jobTitle;
    }
}
