package com.inventory.model;

/**
 * User represents ONE person who can log in to the application. It is the
 * shared parent of Admin and Cashier.
 *
 * OOP concept: ABSTRACTION + INHERITANCE.
 * User is declared "abstract", which means you can never create a plain
 * "new User(...)" directly - only a real, more specific kind of user
 * (Admin or Cashier). Every subclass "is-a" User and must supply its own
 * getRole(), while sharing the username/password fields and the
 * checkPassword()/toFileLine() logic defined here. This is the exact same
 * pattern already used by Payment (abstract) and its subclasses
 * CashPayment/CardPayment/QRPayment.
 *
 * OOP concept: POLYMORPHISM.
 * Code elsewhere (like AuthService) can hold a variable of type User and
 * call user.getRole() without caring whether the actual object underneath
 * is an Admin or a Cashier.
 *
 * OOP concept: ENCAPSULATION.
 * username and password are private - nothing outside this class can
 * reach in and change them directly. Other classes must use getUsername()
 * or ask checkPassword() whether a given password is correct; they can
 * never simply read the stored password back out.
 *
 * SECURITY NOTE (student project scope): passwords are stored as plain
 * text in data/users.txt, the same simple comma-separated style already
 * used by products.txt. A real production system would hash passwords
 * before saving them - that is intentionally left out here to keep the
 * project focused on core Java OOP and File I/O, not cryptography.
 */
public abstract class User {

    private final String username;
    private final String password;

    protected User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    /**
     * Every subclass says which Role it represents (Admin -> Role.ADMIN,
     * Cashier -> Role.CASHIER). This is how the rest of the application
     * (Session, permission checks) keeps working with the same Role enum
     * it already used before a real login system existed.
     */
    public abstract Role getRole();

    /**
     * Checks whether the given plain-text password matches this user's
     * stored password. Kept as its own method (instead of a public
     * getPassword()) so calling code can ask "is this correct?" without
     * ever being able to read the real password back out.
     */
    public boolean checkPassword(String candidatePassword) {
        return password != null && password.equals(candidatePassword);
    }

    /**
     * Converts this User into one line of text for saving to
     * data/users.txt, using commas to separate the fields.
     * Example: admin,admin123,ADMIN
     */
    public String toFileLine() {
        return username + "," + password + "," + getRole();
    }

    /**
     * Parses one line of text from data/users.txt back into a User
     * object. This is the reverse of toFileLine().
     *
     * OOP concept: FACTORY-STYLE METHOD + POLYMORPHISM. Based on the
     * saved role, this returns either a real Admin or a real Cashier -
     * the caller only ever sees the result as a plain "User" reference,
     * the same way Product.fromFileLine() rebuilds a Product without the
     * caller needing to know the details.
     *
     * @throws IllegalArgumentException if the line does not have exactly
     *                                   3 fields, or the role text is not
     *                                   ADMIN/CASHIER
     */
    public static User fromFileLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Malformed user line: " + line);
        }

        String username = parts[0].trim();
        String password = parts[1].trim();
        String roleText = parts[2].trim().toUpperCase();

        if (username.isEmpty()) {
            throw new IllegalArgumentException("User line is missing a username: " + line);
        }

        Role role;
        try {
            role = Role.valueOf(roleText);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown role \"" + parts[2].trim() + "\" in user line: " + line);
        }

        return switch (role) {
            case ADMIN -> new Admin(username, password);
            case CASHIER -> new Cashier(username, password);
        };
    }

    @Override
    public String toString() {
        return "User{username=" + username + ", role=" + getRole() + "}";
    }
}
