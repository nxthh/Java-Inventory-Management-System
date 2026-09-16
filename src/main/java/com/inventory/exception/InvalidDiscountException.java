package com.inventory.exception;

/**
 * Thrown when a discount value does not make sense, for example a
 * discount percentage that is not a number, or one that is below 0% or
 * above 100%.
 */
public class InvalidDiscountException extends RuntimeException {
    public InvalidDiscountException(String message) {
        super(message);
    }
}
