package com.inventory.exception;

/**
 * Thrown when the currently logged-in user tries to do something that
 * only an ADMIN is allowed to do (e.g. deleting transaction history).
 *
 * This is a "defense in depth" check: the button that triggers the
 * action is already disabled for a CASHIER in the FXML, but the service
 * layer checks Session.isAdmin() again so the rule is enforced even if
 * a screen is ever reached another way - the exact same idea already
 * used for Reports and Inventory access.
 */
public class UnauthorizedActionException extends RuntimeException {
    public UnauthorizedActionException(String message) {
        super(message);
    }
}
