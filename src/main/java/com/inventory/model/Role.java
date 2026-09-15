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
 * Note: There is no full login/authentication system yet (that is a
 * later phase). For now, the Login screen lets the user pick a Role
 * from a dropdown so the Inventory screen has something real to check
 * permissions against.
 */
public enum Role {
    ADMIN,
    CASHIER
}
