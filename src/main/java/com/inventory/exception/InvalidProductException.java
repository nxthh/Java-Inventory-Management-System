package com.inventory.exception;

/**
 * Thrown when product data fails validation rules, for example an empty
 * name, a negative price, or a negative quantity.
 */
public class InvalidProductException extends RuntimeException {
    public InvalidProductException(String message) {
        super(message);
    }
}
