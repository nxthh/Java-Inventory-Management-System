package com.inventory.exception;

/**
 * Thrown when a payment cannot be processed, for example:
 *   - the amount paid in cash is less than the total due
 *   - the amount paid is missing, not a number, or negative
 */
public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
