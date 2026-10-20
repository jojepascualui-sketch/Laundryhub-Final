package com.laundryhub.exception;

/**
 * Base class of every Laundry HUB exception.
 * OOP: Inheritance — all app exceptions share this parent so callers can catch one type.
 */
public class LaundryHubException extends RuntimeException {

    /**
     * @param message user-readable message
     */
    public LaundryHubException(String message) {
        super(message);
    }

    /**
     * @param message user-readable message
     * @param cause   underlying cause
     */
    public LaundryHubException(String message, Throwable cause) {
        super(message, cause);
    }
}
