package com.laundryhub.model;

/**
 * Account roles, stored in profiles.role.
 */
public enum Role {
    CUSTOMER,
    RIDER,
    ADMIN,
    STAFF;

    /**
     * @param code value from the database, may be null
     * @return the matching role, CUSTOMER when unknown
     */
    public static Role fromCode(String code) {
        if (code != null) {
            for (Role r : values()) {
                if (r.name().equals(code)) {
                    return r;
                }
            }
        }
        return CUSTOMER;
    }

    /** @return true for ADMIN and STAFF (the admin console) */
    public boolean isStaff() {
        return this == ADMIN || this == STAFF;
    }
}
