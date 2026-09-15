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
}
