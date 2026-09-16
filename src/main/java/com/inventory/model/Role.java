package com.inventory.model;

/**
 * Role represents who is currently using the application: an ADMIN
 * (full access) or a CASHIER (limited access).
 *
 * OOP concept: ENUM.
 * Just like Category, a Role can only ever be one of a fixed set of
 * values. This makes it impossible to accidentally type a typo'd role
 * like "Admn" - the compiler simply will not allow it.
 *
 * Role is used in two places: each User subclass (Admin/Cashier) reports
 * its own Role via getRole(), and Session remembers the Role of whoever
 * is currently logged in so every screen can check
 * Session.isAdmin() to enforce Admin-only actions.
 */
public enum Role {
    ADMIN,
    CASHIER
}
