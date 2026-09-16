package com.inventory.exception;

/**
 * Thrown when a login attempt fails, for example:
 *   - the username or password field was left empty
 *   - the username does not exist in data/users.txt
 *   - the password does not match the stored password for that username
 *
 * Follows the same pattern as every other exception class in this
 * project (e.g. ProductNotFoundException, PaymentException) - a small,
 * clearly named RuntimeException that a controller can catch and show as
 * a friendly Alert instead of letting the application crash.
 */
public class InvalidLoginException extends RuntimeException {
    public InvalidLoginException(String message) {
        super(message);
    }
}
