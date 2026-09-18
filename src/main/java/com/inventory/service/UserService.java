package com.inventory.service;

import com.inventory.exception.DuplicateUsernameException;
import com.inventory.exception.InvalidUserException;
import com.inventory.model.Cashier;
import com.inventory.model.User;
import com.inventory.repository.UserFileRepository;

import java.util.List;

/**
 * UserService contains the BUSINESS RULES for managing user accounts:
 * listing existing accounts, and letting an Admin create a new Cashier
 * account.
 *
 * OOP concept: LAYERED ARCHITECTURE.
 * Just like AuthService sits between LoginController and
 * UserFileRepository for logging in, UserService sits between
 * UserController and UserFileRepository for account management.
 * UserController never touches data/users.txt itself - it only calls
 * this service and reacts to whether it succeeds or throws.
 *
 * Scope: on purpose, this only lets an Admin create a CASHIER account
 * (not another Admin account) and does not support editing or deleting
 * accounts - that matches exactly what was asked for. Nothing more was
 * added, per the project's "do not over-engineer" rule.
 */
public class UserService {

    private final UserFileRepository userFileRepository;

    public UserService(UserFileRepository userFileRepository) {
        this.userFileRepository = userFileRepository;
    }

    /**
     * Returns every saved user account (Admins and Cashiers alike), for
     * display on the Manage Users screen.
     */
    public List<User> getAllUsers() {
        return userFileRepository.loadAll();
    }

    /**
     * Validates the given fields and, if everything checks out, creates
     * and saves a brand-new Cashier account.
     *
     * @throws InvalidUserException      if the username/password/confirm
     *                                    password fields are empty or the
     *                                    two password fields do not match
     * @throws DuplicateUsernameException if the username is already used
     *                                    by an existing account
     */
    public Cashier createCashierAccount(String username, String password, String confirmPassword) {
        if (username == null || username.isBlank()) {
            throw new InvalidUserException("Please enter a username.");
        }
        if (password == null || password.isBlank()) {
            throw new InvalidUserException("Please enter a password.");
        }
        if (!password.equals(confirmPassword)) {
            throw new InvalidUserException("Password and Confirm Password do not match.");
        }

        String trimmedUsername = username.trim();
        if (userFileRepository.findByUsername(trimmedUsername).isPresent()) {
            throw new DuplicateUsernameException("Username \"" + trimmedUsername + "\" is already taken.");
        }

        Cashier cashier = new Cashier(trimmedUsername, password);
        userFileRepository.addUser(cashier);
        return cashier;
    }
}
