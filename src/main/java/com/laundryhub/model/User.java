package com.laundryhub.model;

import com.laundryhub.exception.ValidationException;
import com.laundryhub.util.TextUtils;

/**
 * A signed-in person. Each role is a subclass.
 *
 * <p>OOP: Abstraction — User says what every account has; subclasses say what differs.
 */
public abstract class User {

    // OOP: Encapsulation — private fields, read through getters, validated in the constructor.
    private final String id;
    private final String fullName;
    private final String email;
    private final String mobile;
    private final boolean active;

    /**
     * @param id       profile id (auth user id)
     * @param fullName full name, may be empty
     * @param email    e-mail, may be null
     * @param mobile   mobile number, may be null
     * @param active   false when the admin deactivated the account
     */
    protected User(String id, String fullName, String email, String mobile, boolean active) {
        if (id == null || id.trim().isEmpty()) {
            throw new ValidationException("A user needs an id.");
        }
        this.id = id;
        this.fullName = fullName == null ? "" : fullName.trim();
        this.email = email;
        this.mobile = mobile;
        this.active = active;
    }

    /** @return the role of this account */
    public abstract Role getRole();

    /**
     * Where this user lands after signing in.
     * OOP: Polymorphism — each role overrides this.
     *
     * @return hash route of the role's home page
     */
    public abstract String getHomeRoute();

    /** @return label shown under the name, e.g. "Customer", "Owner" */
    public abstract String getRoleLabel();

    /** @return profile id */
    public String getId() {
        return id;
    }

    /** @return full name, never null */
    public String getFullName() {
        return fullName;
    }

    /** @return name to greet the user with; falls back to the e-mail */
    public String getDisplayName() {
        if (!fullName.isEmpty()) {
            return fullName;
        }
        return email == null ? "" : email;
    }

    /** @return first name for greetings */
    public String getFirstName() {
        String name = getDisplayName();
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }

    /** @return avatar initials */
    public String getInitials() {
        return TextUtils.initials(getDisplayName());
    }

    /** @return e-mail, may be null */
    public String getEmail() {
        return email;
    }

    /** @return mobile number, may be null */
    public String getMobile() {
        return mobile;
    }

    /** @return true when the account may sign in */
    public boolean isActive() {
        return active;
    }

    /**
     * Creates the right subclass for a role.
     * OOP: Polymorphism — callers get a User and never need to know the concrete class.
     *
     * @param role     account role
     * @param id       profile id
     * @param fullName full name
     * @param email    e-mail
     * @param mobile   mobile number
     * @param active   account status
     * @param jobTitle staff job title (ADMIN/STAFF only), may be null
     * @return a Customer, Rider, Admin or Staff
     */
    public static User create(Role role, String id, String fullName, String email, String mobile,
                              boolean active, String jobTitle) {
        switch (role) {
            case RIDER:
                return new Rider(id, fullName, email, mobile, active, null, false);
            case ADMIN:
                return new Admin(id, fullName, email, mobile, active, jobTitle);
            case STAFF:
                return new Staff(id, fullName, email, mobile, active, jobTitle);
            default:
                return new Customer(id, fullName, email, mobile, active);
        }
    }
}
