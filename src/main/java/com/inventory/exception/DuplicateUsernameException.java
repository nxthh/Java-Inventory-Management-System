package com.inventory.exception;

/**
 * Thrown when code tries to create a new user account using a username
 * that is already used by an existing account. Every username must be
 * unique (case-insensitive), the same rule ProductFileRepository already
 * enforces for product IDs via DuplicateProductException.
 */
public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String message) {
        super(message);
    }
}
