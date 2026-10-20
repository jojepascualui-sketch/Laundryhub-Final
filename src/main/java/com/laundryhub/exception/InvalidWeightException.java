package com.laundryhub.exception;

/**
 * Thrown when a laundry weight is not a usable number of kilograms.
 */
public class InvalidWeightException extends ValidationException {

    /**
     * @param message user-readable message
     */
    public InvalidWeightException(String message) {
        super(message);
    }
}
