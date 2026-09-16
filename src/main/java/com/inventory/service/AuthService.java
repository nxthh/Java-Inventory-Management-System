package com.inventory.service;

import com.inventory.exception.InvalidLoginException;
import com.inventory.model.User;
import com.inventory.repository.UserFileRepository;

import java.util.Optional;

/**
 * AuthService contains the BUSINESS RULES for logging in: checking that
 * the username/password fields were filled in, looking up the account,
 * and verifying the password.
 *
 * OOP concept: SEPARATION OF RESPONSIBILITIES / LAYERED ARCHITECTURE.
 * Just like ProductService sits between InventoryController and
 * ProductFileRepository, AuthService sits between LoginController and
 * UserFileRepository. LoginController never touches data/users.txt or
 * compares passwords itself - it only calls login() and reacts to
 * whether it succeeds or throws.
 */
public class AuthService {

    private final UserFileRepository userFileRepository;

    public AuthService(UserFileRepository userFileRepository) {
        this.userFileRepository = userFileRepository;
    }

    /**
     * Attempts to log in with the given username and password.
     *
     * @return the matching User (an Admin or a Cashier) if the login is valid
     * @throws InvalidLoginException if either field is empty, the username
     *                                does not exist, or the password does
     *                                not match
     */
    public User login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new InvalidLoginException("Please enter a username.");
        }
        if (password == null || password.isBlank()) {
            throw new InvalidLoginException("Please enter a password.");
        }

        Optional<User> foundUser = userFileRepository.findByUsername(username.trim());
        if (foundUser.isEmpty()) {
            throw new InvalidLoginException("Invalid username or password.");
        }

        User user = foundUser.get();
        if (!user.checkPassword(password)) {
            // Deliberately the SAME message as "username not found" above,
            // so a failed login never reveals whether the username itself
            // was correct.
            throw new InvalidLoginException("Invalid username or password.");
        }

        return user;
    }
}
