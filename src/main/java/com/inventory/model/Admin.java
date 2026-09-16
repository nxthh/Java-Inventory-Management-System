package com.inventory.model;

/**
 * Admin represents a user with FULL access: managing inventory, viewing
 * every transaction, and viewing the Reports screen.
 *
 * OOP concept: INHERITANCE.
 * Admin "extends" User, so it automatically has username/password and
 * checkPassword()/toFileLine() without redeclaring them, and it must
 * fill in the one abstract method (getRole()) that User promised existed.
 * This mirrors CashPayment/CardPayment/QRPayment extending Payment.
 */
public class Admin extends User {

    public Admin(String username, String password) {
        super(username, password);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}
