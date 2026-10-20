package com.laundryhub.exception;

/**
 * Thrown when payment details (e.g. a GCash reference number) are invalid.
 */
public class InvalidPaymentException extends ValidationException {

    /**
     * @param message user-readable message
     */
    public InvalidPaymentException(String message) {
        super(message);
    }
}
