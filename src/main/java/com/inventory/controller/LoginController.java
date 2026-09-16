package com.inventory.controller;

import com.inventory.Main;
import com.inventory.exception.InvalidLoginException;
import com.inventory.model.User;
import com.inventory.repository.UserFileRepository;
import com.inventory.service.AuthService;
import com.inventory.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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
 * those belong in Service and Repository classes.
 *
 * OOP concept: LAYERED ARCHITECTURE.
 * This controller only knows about JavaFX controls and what the person
 * wants to do (log in). It does not know how accounts are stored (that is
 * UserFileRepository) or how a login attempt is validated (that is
 * AuthService). It simply reads the two text fields, hands them to
 * AuthService.login(), and reacts to whether that succeeds or throws.
 *
 * Real login: the username/password are checked against the accounts
 * saved in data/users.txt via AuthService. A successful login returns a
 * real User (an Admin or a Cashier) whose role is recorded in Session, so
 * every other screen's Admin/Cashier permission checks keep working
 * exactly as they did before real login existed.
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label statusLabel;

    // The controller only talks to a service - never straight to the
    // repository or straight to a file, same pattern as every other
    // controller in this project.
    private final UserFileRepository userFileRepository = new UserFileRepository();
    private final AuthService authService = new AuthService(userFileRepository);

    /**
     * Called automatically when the "Login" button is clicked
     * (linked via onAction="#handleLogin" in login.fxml).
     */
    @FXML
    private void handleLogin() {
        statusLabel.setText("");
        try {
            User user = authService.login(usernameField.getText(), passwordField.getText());
            Session.login(user);
            passwordField.clear();
            Main.switchScene("view/dashboard.fxml");
        } catch (InvalidLoginException e) {
            showError(e.getMessage());
        } catch (IOException e) {
            statusLabel.setText("Unable to load dashboard screen.");
        }
    }

    private void showError(String message) {
        statusLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Login Failed");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
