package com.inventory.exception;

/**
 * Thrown when code tries to remove more stock (stockOut) than a product
 * currently has available. This stops quantity from ever going negative.
 */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
