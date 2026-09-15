package com.inventory.exception;

/**
 * Thrown when code tries to add a new product using an ID that is already
 * used by an existing product. Every product ID must be unique.
 */
public class DuplicateProductException extends RuntimeException {
    public DuplicateProductException(String message) {
        super(message);
    }
}
