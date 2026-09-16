package com.inventory.exception;

/**
 * Thrown when something is wrong with a cart operation that is NOT about
 * running out of stock, for example:
 *   - trying to add a quantity of zero or less
 *   - trying to update/remove a product that is not actually in the cart
 *
 * (Running out of stock uses the existing InsufficientStockException
 * instead, since that is exactly what it already means.)
 */
public class InvalidCartOperationException extends RuntimeException {
    public InvalidCartOperationException(String message) {
        super(message);
    }
}
