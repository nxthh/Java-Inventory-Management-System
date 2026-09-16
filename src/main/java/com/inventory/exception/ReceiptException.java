package com.inventory.exception;

/**
 * Thrown when something goes wrong while generating, saving, or loading a
 * Receipt, for example:
 *   - the receipts folder/file could not be written to disk
 *   - a requested receipt file does not exist
 *   - a saved receipt file could not be read back
 *
 * Follows the same pattern as the other exception classes in this project
 * (e.g. PaymentException, InvalidCartOperationException) - a small, clearly
 * named RuntimeException that calling code can catch and show as a
 * friendly error message instead of letting the application crash.
 */
public class ReceiptException extends RuntimeException {
    public ReceiptException(String message) {
        super(message);
    }

    public ReceiptException(String message, Throwable cause) {
        super(message, cause);
    }
}
