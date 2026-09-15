package com.inventory.exception;

/**
 * Thrown when code tries to look up, update, or delete a product using an
 * ID that does not exist in the system.
 *
 * OOP concept: EXCEPTION HANDLING / INHERITANCE.
 * This class extends RuntimeException, so it "is-a" RuntimeException and
 * inherits all of its behavior. Creating our own small exception class
 * lets calling code catch this exact problem (a missing product) and show
 * a friendly message, instead of the whole application crashing.
 */
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}
