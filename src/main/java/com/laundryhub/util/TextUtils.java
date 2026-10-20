package com.laundryhub.util;

/**
 * Small text helpers for the UI.
 */
public final class TextUtils {

    private TextUtils() {
    }

    /**
     * Initials for avatars: "Rider Mendoza" → "RM", "Admin" → "AD".
     *
     * @param name full name
     * @return one or two upper-case letters, or "?" when empty
     */
    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\s+");
        if (parts.length == 1) {
            String p = parts[0];
            return (p.length() >= 2 ? p.substring(0, 2) : p).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    /**
     * Formats a mobile number like the design: "09171231021" → "0917 123 1021".
     *
     * @param mobile 11-digit number
     * @return grouped number, or the input when it is not 11 digits
     */
    public static String formatMobile(String mobile) {
        String v = Validators.compact(mobile);
        if (!Validators.isMobile(v)) {
            return mobile == null ? "" : mobile;
        }
        return v.substring(0, 4) + " " + v.substring(4, 7) + " " + v.substring(7);
    }

    /**
     * @param value text or null
     * @return the text, or "" when null
     */
    public static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
