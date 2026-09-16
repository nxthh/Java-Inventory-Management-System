package com.inventory.controller;

import com.inventory.Main;
import com.inventory.exception.ReceiptException;
import com.inventory.model.CartItem;
import com.inventory.model.Transaction;
import com.inventory.repository.ReceiptFileRepository;
import com.inventory.repository.TransactionFileRepository;
import com.inventory.service.ReceiptService;
import com.inventory.service.TransactionService;
import com.inventory.util.ReceiptDialog;
import com.inventory.util.Session;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for transactions.fxml - the Transaction History screen.
 *
 * OOP concept: LAYERED ARCHITECTURE.
 * Just like every other controller in this project, TransactionController
 * only knows about JavaFX controls and *what* the user wants to do (view
 * the list, view one transaction's details, view its saved receipt). It
 * never opens data/transactions.txt or a receipt file itself - it calls
 * TransactionService and ReceiptService and lets them (and the
 * repositories underneath them) deal with the actual files.
 *
 * Permissions: an ADMIN sees every transaction ever made. A CASHIER only
 * sees the transactions where they were the cashier - this filtering
 * happens inside TransactionService, not here, so the rule lives in one
 * place.
 */
public class TransactionController {

    @FXML
    private Label roleLabel;

    @FXML
    private TableView<Transaction> transactionTable;
    @FXML
    private TableColumn<Transaction, String> idColumn;
    @FXML
    private TableColumn<Transaction, String> dateColumn;
    @FXML
    private TableColumn<Transaction, String> cashierColumn;
    @FXML
    private TableColumn<Transaction, String> totalColumn;
    @FXML
    private TableColumn<Transaction, String> paymentMethodColumn;

    @FXML
    private Button viewDetailsButton;
    @FXML
    private Button viewReceiptButton;
    @FXML
    private Label statusLabel;

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // The controller only talks to services - never straight to a
    // Repository or straight to a file.
    private final TransactionFileRepository transactionFileRepository = new TransactionFileRepository();
    private final TransactionService transactionService = new TransactionService(transactionFileRepository);

    private final ReceiptFileRepository receiptFileRepository = new ReceiptFileRepository();
    private final ReceiptService receiptService = new ReceiptService(receiptFileRepository);

    private Transaction selectedTransaction;

    @FXML
    private void initialize() {
        setupTableColumns();
        setupSelectionListener();
        roleLabel.setText("Logged in as: " + Session.getCurrentUsername() + " (" + Session.getCurrentRole() + ")");
        refreshTransactions();
        updateActionButtonsState();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTransactionId()));
        dateColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDateTime().format(DATE_TIME_FORMAT)));
        cashierColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCashier()));
        totalColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("$%.2f", data.getValue().getTotal())));
        paymentMethodColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentMethod()));
    }

    private void setupSelectionListener() {
        transactionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedTransaction = newVal;
            updateActionButtonsState();
        });
    }

    private void updateActionButtonsState() {
        boolean hasSelection = selectedTransaction != null;
        viewDetailsButton.setDisable(!hasSelection);
        viewReceiptButton.setDisable(!hasSelection);
    }

    /**
     * Reloads the transaction list from data/transactions.txt (through
     * TransactionService), respecting the ADMIN/CASHIER visibility rule.
     * If the file is missing or unreadable, TransactionFileRepository
     * already handles that quietly and simply returns an empty list, so
     * this screen never crashes - it just shows "No transactions found."
     */
    @FXML
    private void handleRefresh() {
        refreshTransactions();
    }

    private void refreshTransactions() {
        List<Transaction> transactions = transactionService.getVisibleTransactions();
        transactionTable.setItems(FXCollections.observableArrayList(transactions));
        statusLabel.setText(transactions.isEmpty() ? "No transactions found." : "");
    }

    /**
     * Shows a summary of the selected transaction: every purchased item,
     * quantity, and the money breakdown - similar to the confirmation
     * shown right after checkout, but reloaded from saved data instead of
     * a live sale.
     */
    @FXML
    private void handleViewDetails() {
        if (selectedTransaction == null) {
            showError("Please select a transaction first.");
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append(String.format("Date: %s%n", selectedTransaction.getDateTime().format(DATE_TIME_FORMAT)));
        message.append(String.format("Cashier: %s%n%n", selectedTransaction.getCashier()));

        for (CartItem item : selectedTransaction.getItems()) {
            message.append(String.format("%-20s x%-4d $%.2f%n",
                    item.getProduct().getName(), item.getQuantity(), item.getSubtotal()));
        }
        message.append(System.lineSeparator());

        message.append(String.format("Subtotal: $%.2f%n", selectedTransaction.getSubtotal()));
        message.append(String.format("Discount: $%.2f%n", selectedTransaction.getDiscountAmount()));
        message.append(String.format("Tax: $%.2f%n", selectedTransaction.getTaxAmount()));
        message.append(String.format("Total: $%.2f%n%n", selectedTransaction.getTotal()));

        message.append(String.format("Payment Method: %s%n", selectedTransaction.getPaymentMethod()));
        if ("Cash".equals(selectedTransaction.getPaymentMethod())) {
            message.append(String.format("Amount Paid: $%.2f%n", selectedTransaction.getAmountPaid()));
            message.append(String.format("Change: $%.2f", selectedTransaction.getChange()));
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, message.toString(), ButtonType.OK);
        alert.setTitle("Transaction " + selectedTransaction.getTransactionId());
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Opens the saved receipt for the selected transaction. Handles every
     * error case gracefully instead of crashing:
     *   - the transaction has no receiptId at all (saved before receipts
     *     existed, i.e. old Part 6A data)
     *   - the receiptId is recorded but the .txt file is missing or
     *     unreadable (ReceiptException from ReceiptService)
     */
    @FXML
    private void handleViewReceipt() {
        if (selectedTransaction == null) {
            showError("Please select a transaction first.");
            return;
        }

        if (!selectedTransaction.hasReceipt()) {
            showError("No receipt was saved for this transaction.");
            return;
        }

        try {
            String receiptText = receiptService.loadReceiptText(selectedTransaction.getReceiptId());
            ReceiptDialog.show("Receipt " + selectedTransaction.getReceiptId(), receiptText);
        } catch (ReceiptException e) {
            showError("Could not open the saved receipt: " + e.getMessage());
        }
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
}
