package com.inventory.controller;

import com.inventory.Main;
import com.inventory.model.Role;
import com.inventory.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * Controller for login.fxml.
 *
 * A "Controller" in the MVC (Model-View-Controller) pattern is responsible
 * for reacting to UI events (like a button click) and deciding what should
 * happen next. It should NOT contain File I/O or complex business rules -
 * those belong in Service and Repository classes, added in later phases.
 *
 * Phase 1 note: There is no User class yet, so this controller does not
 * validate a username/password. It simply demonstrates navigating from the
 * Login screen to the Dashboard screen. Real authentication will be added
 * once the User/Admin/Cashier classes exist.
 *
 * Phase 3 note: A Role dropdown was added so the rest of the app (starting
 * with the Inventory screen) has a real Role to check permissions against,
 * via Session.setCurrentRole(). This is intentionally simple - it is NOT a
 * full login system, just enough to demonstrate Admin vs Cashier access.
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<Role> roleComboBox;

    @FXML
    private Label statusLabel;

    /**
     * Called automatically when the screen first loads.
     * Fills the Role dropdown with ADMIN and CASHIER, defaulting to ADMIN.
     */
    @FXML
    private void initialize() {
        roleComboBox.getItems().addAll(Role.ADMIN, Role.CASHIER);
        roleComboBox.setValue(Role.ADMIN);
    }

    /**
     * Called automatically when the "Login" button is clicked
     * (linked via onAction="#handleLogin" in login.fxml).
     */
    @FXML
    private void handleLogin() {
        try {
            // Phase 1 placeholder: no credential checking yet.
            // A later phase may replace this with a real login check using
            // a UserService/UserRepository. For now we only remember which
            // Role was selected, so the Dashboard/Inventory screens can
            // enforce Admin vs Cashier permissions.
            Role selectedRole = roleComboBox.getValue();
            if (selectedRole == null) {
                selectedRole = Role.ADMIN;
            }
            Session.setCurrentRole(selectedRole);

            Main.switchScene("view/dashboard.fxml");
        } catch (IOException e) {
            statusLabel.setText("Unable to load dashboard screen.");
        }
    }
}
