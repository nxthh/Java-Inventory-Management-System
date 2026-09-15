package com.inventory.controller;

import com.inventory.Main;
import javafx.fxml.FXML;
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
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label statusLabel;

    /**
     * Called automatically when the "Login" button is clicked
     * (linked via onAction="#handleLogin" in login.fxml).
     */
    @FXML
    private void handleLogin() {
        try {
            // Phase 1 placeholder: no credential checking yet.
            // Phase 2 will replace this with a real login check using
            // a UserService/UserRepository.
            Main.switchScene("view/dashboard.fxml");
        } catch (IOException e) {
            statusLabel.setText("Unable to load dashboard screen.");
        }
    }
}
