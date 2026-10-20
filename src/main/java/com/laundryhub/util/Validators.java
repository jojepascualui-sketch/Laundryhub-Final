package com.laundryhub.util;

/**
 * Input rules shared by forms and models. Pure Java, so they are unit tested.
 */
public final class Validators {

    /** Length of a GCash reference number. */
    public static final int GCASH_REFERENCE_LENGTH = 13;
    /** Minimum password length (matches the "At least 8 characters" hint). */
    public static final int MIN_PASSWORD_LENGTH = 8;

    private static final int MOBILE_LENGTH = 11;

    private Validators() {
    }

    /**
     * Removes spaces and dashes, so "0917 123 4567" becomes "09171234567".
     *
     * @param value raw input
     * @return the digits and other characters without spaces/dashes, never null
     */
    public static String compact(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != ' ' && c != '-') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * @param value raw input
     * @return true for a Philippine mobile number 09XXXXXXXXX (spaces allowed)
     */
    public static boolean isMobile(String value) {
        String v = compact(value);
        return v.length() == MOBILE_LENGTH && v.startsWith("09") && allDigits(v);
    }

    /**
     * @param value raw input
     * @return true for a 13-digit GCash reference number (spaces allowed)
     */
    public static boolean isGcashReference(String value) {
        String v = compact(value);
        return v.length() == GCASH_REFERENCE_LENGTH && allDigits(v);
    }

    /**
     * @param value raw input
     * @return true for a plausible e-mail address
     */
    public static boolean isEmail(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim();
        int at = v.indexOf('@');
        int dot = v.lastIndexOf('.');
        return at > 0 && at == v.lastIndexOf('@') && dot > at + 1 && dot < v.length() - 1
                && v.indexOf(' ') < 0;
    }

    /**
     * @param value raw input
     * @return true when the password is long enough
     */
    public static boolean isPassword(String value) {
        return value != null && value.length() >= MIN_PASSWORD_LENGTH;
    }

    /**
     * @param value raw input
     * @return true when the value has visible text
     */
    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean allDigits(String v) {
        for (int i = 0; i < v.length(); i++) {
            if (!Character.isDigit(v.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
