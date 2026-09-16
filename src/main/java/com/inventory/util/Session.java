package com.inventory.util;

import com.inventory.model.Role;
import com.inventory.model.User;

/**
 * Session remembers which Role (and which username) is currently logged
 * in, so any screen in the application can check "is this user allowed
 * to do this?" or "who made this sale?".
 *
 * OOP concept: STATIC MEMBERS.
 * The current role/username are stored in static fields, meaning there is
 * only ONE copy of each shared by the whole application (not one per
 * object). This is how the logged-in user set by LoginController (via
 * AuthService and login()) becomes visible to every other screen without
 * having to pass a User object through every constructor.
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

    /**
     * Records who just logged in, based on a real, authenticated User
     * object (an Admin or a Cashier) returned by AuthService.login().
     * This is just a convenience wrapper around the two setters above -
     * every other class in the project keeps reading the current user
     * through getCurrentRole()/getCurrentUsername()/isAdmin() exactly as
     * before, so nothing else had to change when real login was added.
     */
    public static void login(User user) {
        setCurrentRole(user.getRole());
        setCurrentUsername(user.getUsername());
    }
}
