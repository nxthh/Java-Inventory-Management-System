package com.inventory.repository;

import com.inventory.model.Admin;
import com.inventory.model.Cashier;
import com.inventory.model.User;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * UserFileRepository is responsible for ALL reading and writing of user
 * account data to and from the text file data/users.txt.
 *
 * OOP concept: SEPARATION OF RESPONSIBILITIES.
 * This follows the exact same pattern as ProductFileRepository: it is the
 * ONLY class in the whole application that knows the file lives at
 * data/users.txt or what its comma-separated format looks like.
 * AuthService never opens this file itself - it asks this repository to
 * find a user and lets this class deal with the file.
 *
 * Error handling: just like ProductFileRepository, one corrupted line in
 * users.txt is skipped (with a console warning) instead of crashing the
 * whole application - a login screen should never fail to load just
 * because one line of a text file got damaged.
 */
public class UserFileRepository {

    private static final String FILE_PATH = "data/users.txt";

    /**
     * Creates the repository and makes sure data/users.txt exists.
     * If the file (or the data folder) is missing, it is created and
     * filled with the two default accounts described in the project's
     * documentation, so the application is always logins-capable.
     */
    public UserFileRepository() {
        createFileWithDefaultAccountsIfMissing();
    }

    /**
     * Reads every line from data/users.txt and converts each line into a
     * User object (really an Admin or a Cashier - see User.fromFileLine()).
     *
     * @return an ArrayList containing every user account currently saved
     */
    public List<User> loadAll() {
        List<User> users = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue; // skip blank lines
                }
                try {
                    users.add(User.fromFileLine(line));
                } catch (IllegalArgumentException e) {
                    // One bad line should not stop every other account
                    // from loading (same idea as TransactionFileRepository).
                    System.out.println("Skipping invalid user record: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read user file: " + e.getMessage());
        }

        return users;
    }

    /**
     * Searches the saved users for one matching the given username
     * (case-insensitive).
     *
     * OOP concept: Optional is used instead of returning null, the same
     * way ProductFileRepository.findById() does.
     */
    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        for (User user : loadAll()) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    /**
     * Appends one new user account to the end of data/users.txt and
     * makes it show up in every future loadAll()/findByUsername() call.
     *
     * This follows the exact same "append a new line" idea as
     * TransactionFileRepository.saveTransaction() - the file is never
     * rewritten from scratch, existing accounts are left completely
     * untouched, only one new line is added.
     */
    public void addUser(User user) {
        try {
            Path filePath = Path.of(FILE_PATH);
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            try (FileWriter writer = new FileWriter(FILE_PATH, true)) {
                writer.write(user.toFileLine());
                writer.write(System.lineSeparator());
            }
        } catch (IOException e) {
            System.out.println("Could not save new user account: " + e.getMessage());
        }
    }

    /**
     * Creates the data folder and users.txt file with the two default
     * accounts (admin/admin123, cashier/cashier123) if they do not
     * already exist. This runs once, the first time the application
     * starts, the same way ProductFileRepository seeds sample products.
     */
    private void createFileWithDefaultAccountsIfMissing() {
        try {
            Path filePath = Path.of(FILE_PATH);

            if (Files.exists(filePath)) {
                return; // file already exists, nothing to do
            }

            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }

            List<User> defaultUsers = new ArrayList<>();
            defaultUsers.add(new Admin("admin", "admin123"));
            defaultUsers.add(new Cashier("cashier", "cashier123"));

            try (FileWriter writer = new FileWriter(FILE_PATH)) {
                for (User user : defaultUsers) {
                    writer.write(user.toFileLine());
                    writer.write(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            System.out.println("Could not create user file: " + e.getMessage());
        }
    }
}
