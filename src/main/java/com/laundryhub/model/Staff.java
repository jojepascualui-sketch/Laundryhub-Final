package com.laundryhub.model;

/**
 * Counter staff: handles orders in the admin console but cannot change settings.
 *
 * <p>OOP: Inheritance + Polymorphism — overrides {@link #canManageSettings()} and the role.
 */
public class Staff extends Admin {

    /**
     * @param id       profile id
     * @param fullName full name
     * @param email    e-mail
     * @param mobile   mobile number
     * @param active   account status
     * @param jobTitle job title, e.g. "Counter staff"; may be null
     */
    public Staff(String id, String fullName, String email, String mobile, boolean active, String jobTitle) {
        super(id, fullName, email, mobile, active, jobTitle);
    }

    @Override
    public boolean canManageSettings() {
        return false;
    }

    @Override
    public Role getRole() {
        return Role.STAFF;
    }

    @Override
    public String getRoleLabel() {
        return getJobTitle() == null || getJobTitle().isEmpty() ? "Staff" : getJobTitle();
    }
}
