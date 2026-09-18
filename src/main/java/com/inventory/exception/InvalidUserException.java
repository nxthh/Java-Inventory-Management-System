package com.inventory.exception;

/**
 * Thrown when the data typed in for a NEW user account is invalid, for
 * example:
 *   - the username or password field was left empty
 *   - the "Confirm Password" field does not match the password field
 *
 * Follows the same pattern as every other exception class in this
 * project (e.g. InvalidLoginException, InvalidProductException) - a
 * small, clearly named RuntimeException that a controller can catch and
 * show as a friendly Alert instead of letting the application crash.
 */
public class InvalidUserException extends RuntimeException {
    public InvalidUserException(String message) {
        super(message);
    }
}
