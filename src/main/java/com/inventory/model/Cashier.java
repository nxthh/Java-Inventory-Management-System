package com.inventory.model;

/**
 * Cashier represents a user with LIMITED access: using the Point of Sale
 * screen and viewing only their own past transactions. Inventory
 * management and the Reports screen are Admin-only (enforced by
 * Session.isAdmin() checks in the controllers, not here).
 *
 * OOP concept: INHERITANCE.
 * Cashier "extends" User, the same way Admin does, but reports a
 * different Role. Code that only knows about "a User" (like AuthService)
 * never needs to know which subclass it is holding.
 */
public class Cashier extends User {

    public Cashier(String username, String password) {
        super(username, password);
    }

    @Override
    public Role getRole() {
        return Role.CASHIER;
    }
}
