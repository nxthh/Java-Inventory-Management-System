package com.inventory.util;

import com.inventory.model.Role;

/**
 * Session remembers which Role is currently logged in, so any screen in
 * the application can check "is this user allowed to do this?".
 *
 * OOP concept: STATIC MEMBERS.
 * The current role is stored in a static field, meaning there is only
 * ONE copy of it shared by the whole application (not one per object).
 * This is a simple, beginner-friendly way to pass the logged-in role
 * from the Login screen to every other screen, without needing a full
 * user-account/authentication system yet.
 */
public class Session {

    // Defaults to ADMIN so the app is usable even before a role is chosen.
    private static Role currentRole = Role.ADMIN;

    // Part 6A: also remembers who is logged in, so a saved Transaction
    // can record a real "cashier" name instead of just a Role. Defaults
    // to "Cashier" so the POS screen still works even if it is opened
    // without going through the Login screen first.
    private static String currentUsername = "Cashier";

    // Private constructor: nobody should create a Session object.
    // Every method here is static, so the class is used as
    // Session.getCurrentRole() / Session.setCurrentRole(...).
    private Session() {
    }

    public static Role getCurrentRole() {
        return currentRole;
    }

    public static void setCurrentRole(Role role) {
        currentRole = role;
    }

    public static boolean isAdmin() {
        return currentRole == Role.ADMIN;
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }

    /**
     * Stores the logged-in username. Blank/missing input falls back to
     * "Cashier" instead of saving an empty name into transactions.
     */
    public static void setCurrentUsername(String username) {
        currentUsername = (username == null || username.isBlank()) ? "Cashier" : username.trim();
    }
}
