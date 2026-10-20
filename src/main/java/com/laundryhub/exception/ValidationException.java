package com.laundryhub.exception;

/**
 * Thrown when user input or model data breaks a rule. The message is shown to the user.
 */
public class ValidationException extends LaundryHubException {

    /**
     * @param message user-readable message
     */
    public ValidationException(String message) {
        super(message);
    }
}
