package com.inventory.controller;

import com.inventory.Main;
import com.inventory.exception.DuplicateUsernameException;
import com.inventory.exception.InvalidUserException;
import com.inventory.model.Cashier;
import com.inventory.model.User;
import com.inventory.repository.UserFileRepository;
import com.inventory.service.UserService;
import com.inventory.util.Session;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.List;

/**
 * Controller for users.fxml - the ADMIN-only "Manage Users" screen.
 *
 * OOP concept: LAYERED ARCHITECTURE.
 * Exactly like every other controller in this project, this class only
 * knows about JavaFX controls and *what* the admin wants to do (see the
 * list of accounts, create a new Cashier account). It never opens
 * data/users.txt itself - it calls UserService and lets that (and
 * UserFileRepository underneath it) deal with the actual file.
 *
 * Permissions: this whole screen is ADMIN-only. The Dashboard already
 * hides/disables the "Manage Users" button for a CASHIER, but this
 * controller checks Session.isAdmin() again when the screen loads (the
 * same "defense in depth" pattern InventoryController and
 * ReportsController already use), in case this screen is ever reached
 * another way.
 */
public class UserController {

    @FXML
    private Label roleLabel;

    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, String> usernameColumn;
    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TextField newUsernameField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button createCashierButton;

    @FXML
    private Label statusMessageLabel;

    // The controller only talks to a service - never straight to a
    // Repository or straight to a file, same pattern as every other
    // controller in this project.
    private final UserFileRepository userFileRepository = new UserFileRepository();
    private final UserService userService = new UserService(userFileRepository);

    @FXML
    private void initialize() {
        if (!Session.isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Managing users is only available to ADMIN users.", ButtonType.OK);
            alert.setTitle("Access Denied");
            alert.setHeaderText(null);
            alert.showAndWait();
            handleBack();
            return;
        }

        setupTableColumns();
        roleLabel.setText("Logged in as: " + Session.getCurrentUsername() + " (" + Session.getCurrentRole() + ")");
        refreshUsers();
    }

    private void setupTableColumns() {
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole().toString()));
    }

    @FXML
    private void handleRefresh() {
        refreshUsers();
    }

    private void refreshUsers() {
        List<User> users = userService.getAllUsers();
        usersTable.setItems(FXCollections.observableArrayList(users));
    }

    /**
     * Creates a new Cashier account from whatever is currently typed in
     * the form. UserService does the real validation (empty fields,
     * mismatched passwords, a username already taken) - this method just
     * reacts to success or failure.
     */
    @FXML
    private void handleCreateCashier() {
        try {
            Cashier cashier = userService.createCashierAccount(
                    newUsernameField.getText(), newPasswordField.getText(), confirmPasswordField.getText());
            showSuccess("Cashier account \"" + cashier.getUsername() + "\" created successfully.");
            clearForm();
            refreshUsers();
        } catch (InvalidUserException | DuplicateUsernameException e) {
            showError(e.getMessage());
        }
    }

    private void clearForm() {
        newUsernameField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    @FXML
    private void handleBack() {
        try {
            Main.switchScene("view/dashboard.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        statusMessageLabel.setStyle("-fx-text-fill: #2e7d32;");
        statusMessageLabel.setText(message);
    }
}
