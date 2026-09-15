package com.inventory.controller;

import com.inventory.Main;
import com.inventory.exception.DuplicateProductException;
import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.InvalidProductException;
import com.inventory.exception.ProductNotFoundException;
import com.inventory.model.Category;
import com.inventory.model.Product;
import com.inventory.repository.ProductFileRepository;
import com.inventory.service.InventoryService;
import com.inventory.service.ProductService;
import com.inventory.util.Session;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for inventory.fxml.
 *
 * OOP concept: LAYERED ARCHITECTURE.
 * This class only knows about JavaFX controls and *what* the admin wants
 * to do (add, edit, delete, search, stock in/out). It does NOT know how
 * products are validated or how they are saved to disk - that is handled
 * by ProductService, InventoryService, and ProductFileRepository. If we
 * ever changed how products are stored, this class would not need to change.
 */
public class InventoryController {

    // ----- Search / filter bar -----
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryFilterComboBox;
    @FXML
    private ToggleButton lowStockToggleButton;

    // ----- Table -----
    @FXML
    private TableView<Product> productTable;
    @FXML
    private TableColumn<Product, String> idColumn;
    @FXML
    private TableColumn<Product, String> nameColumn;
    @FXML
    private TableColumn<Product, String> categoryColumn;
    @FXML
    private TableColumn<Product, String> priceColumn;
    @FXML
    private TableColumn<Product, Integer> quantityColumn;
    @FXML
    private TableColumn<Product, Integer> minStockColumn;
    @FXML
    private TableColumn<Product, String> statusColumn;

    // ----- Add/Edit form -----
    @FXML
    private TextField idField;
    @FXML
    private TextField nameField;
    @FXML
    private ComboBox<Category> categoryFormComboBox;
    @FXML
    private TextField priceField;
    @FXML
    private TextField quantityField;
    @FXML
    private TextField minStockField;
    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;

    // ----- Stock adjustment -----
    @FXML
    private Label selectedProductLabel;
    @FXML
    private TextField stockAmountField;
    @FXML
    private Button stockInButton;
    @FXML
    private Button stockOutButton;

    // ----- Feedback -----
    @FXML
    private Label statusMessageLabel;

    // The Controller talks only to these two Services - never straight to
    // the Repository or straight to a file.
    private final ProductFileRepository productFileRepository = new ProductFileRepository();
    private final ProductService productService = new ProductService(productFileRepository);
    private final InventoryService inventoryService = new InventoryService(productFileRepository);

    // The full, unfiltered list of products loaded from the service. The
    // table only ever shows a filtered copy of this list (see applyFilters).
    private List<Product> allProducts = new ArrayList<>();

    // The product currently selected in the table (null if none selected).
    private Product selectedProduct;

    /**
     * Called automatically by JavaFX right after inventory.fxml is loaded.
     */
    @FXML
    private void initialize() {
        setupTableColumns();
        setupFilterControls();
        setupSelectionListener();
        applyPermissions();
        refreshData();
    }

    // ===================== Setup helpers =====================

    private void setupTableColumns() {
        idColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        categoryColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory().toDisplayString()));
        priceColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.2f", data.getValue().getPrice())));
        quantityColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getQuantity()).asObject());
        minStockColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getMinimumStock()).asObject());
        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(inventoryService.isLowStock(data.getValue()) ? "LOW STOCK" : "OK"));

        // Color the Status column so LOW STOCK visually stands out.
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    setStyle("LOW STOCK".equals(status)
                            ? "-fx-text-fill: #c62828; -fx-font-weight: bold;"
                            : "-fx-text-fill: #2e7d32;");
                }
            }
        });
    }

    private void setupFilterControls() {
        // Category filter (top bar): plain display strings, including "All".
        categoryFilterComboBox.setItems(FXCollections.observableArrayList("All", "Drink", "Food", "Dairy", "Other"));
        categoryFilterComboBox.setValue("All");
        categoryFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Live search as the admin types.
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        // Category dropdown used inside the Add/Edit form: shows real Category enum values.
        categoryFormComboBox.setItems(FXCollections.observableArrayList(Category.values()));
        categoryFormComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Category category) {
                return category == null ? "" : category.toDisplayString();
            }

            @Override
            public Category fromString(String text) {
                return Category.fromString(text);
            }
        });
    }

    private void setupSelectionListener() {
        productTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedProduct = newVal;
            if (newVal != null) {
                idField.setText(newVal.getId());
                idField.setDisable(true); // the ID of an existing product cannot be changed
                nameField.setText(newVal.getName());
                categoryFormComboBox.setValue(newVal.getCategory());
                priceField.setText(String.valueOf(newVal.getPrice()));
                quantityField.setText(String.valueOf(newVal.getQuantity()));
                minStockField.setText(String.valueOf(newVal.getMinimumStock()));
                selectedProductLabel.setText("Selected Product: " + newVal.getName());
            } else {
                idField.setDisable(!Session.isAdmin());
                selectedProductLabel.setText("Selected Product: (none)");
            }
            updateActionButtonsState();
        });
    }

    /**
     * Enables/disables everything a CASHIER must not be able to use.
     * Called once on load. (Cashiers normally never even reach this screen,
     * since the Dashboard's Inventory button is disabled for them - this is
     * a second layer of protection in case this screen is ever opened another way.)
     */
    private void applyPermissions() {
        boolean admin = Session.isAdmin();
        addButton.setDisable(!admin);
        nameField.setDisable(!admin);
        categoryFormComboBox.setDisable(!admin);
        priceField.setDisable(!admin);
        quantityField.setDisable(!admin);
        minStockField.setDisable(!admin);
        stockAmountField.setDisable(!admin);
        if (!admin) {
            idField.setDisable(true);
        }
        updateActionButtonsState();
    }

    /**
     * Update/Delete/Stock In/Stock Out all require BOTH: an admin user AND
     * a product currently selected in the table.
     */
    private void updateActionButtonsState() {
        boolean allowed = Session.isAdmin() && selectedProduct != null;
        updateButton.setDisable(!allowed);
        deleteButton.setDisable(!allowed);
        stockInButton.setDisable(!allowed);
        stockOutButton.setDisable(!allowed);
    }

    // ===================== Data loading / filtering =====================

    private void refreshData() {
        allProducts = productService.getAllProducts();
        applyFilters();
    }

    /**
     * Rebuilds the table's visible rows by combining the search box, the
     * category filter, and the Low Stock toggle. All three work together.
     */
    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String categoryChoice = categoryFilterComboBox.getValue();
        boolean lowStockOnly = lowStockToggleButton.isSelected();

        List<Product> filtered = new ArrayList<>();
        for (Product product : allProducts) {
            boolean matchesKeyword = keyword.isEmpty()
                    || product.getId().toLowerCase().contains(keyword)
                    || product.getName().toLowerCase().contains(keyword);

            boolean matchesCategory = categoryChoice == null || "All".equals(categoryChoice)
                    || product.getCategory().toDisplayString().equalsIgnoreCase(categoryChoice);

            boolean matchesLowStock = !lowStockOnly || inventoryService.isLowStock(product);

            if (matchesKeyword && matchesCategory && matchesLowStock) {
                filtered.add(product);
            }
        }
        productTable.setItems(FXCollections.observableArrayList(filtered));
    }

    // ===================== Button actions =====================

    @FXML
    private void handleRefresh() {
        handleClearForm();
        refreshData();
        statusMessageLabel.setText("");
    }

    @FXML
    private void handleLowStockToggle() {
        applyFilters();
    }

    @FXML
    private void handleAddProduct() {
        if (!Session.isAdmin()) {
            return;
        }
        try {
            Product product = buildProductFromForm();
            productService.addProduct(product);
            showSuccess("Product added: " + product.getName());
            handleClearForm();
            refreshData();
        } catch (NumberFormatException e) {
            showError("Price, Quantity, and Minimum Stock must be valid numbers.");
        } catch (InvalidProductException | DuplicateProductException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleUpdateProduct() {
        if (!Session.isAdmin()) {
            return;
        }
        if (selectedProduct == null) {
            showError("Please select a product from the table to edit.");
            return;
        }
        try {
            Product product = buildProductFromForm();
            productService.updateProduct(product);
            showSuccess("Product updated: " + product.getName());
            handleClearForm();
            refreshData();
        } catch (NumberFormatException e) {
            showError("Price, Quantity, and Minimum Stock must be valid numbers.");
        } catch (InvalidProductException | ProductNotFoundException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleDeleteProduct() {
        if (!Session.isAdmin()) {
            return;
        }
        if (selectedProduct == null) {
            showError("Please select a product from the table to delete.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete " + selectedProduct.getName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText(null);

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                productService.deleteProduct(selectedProduct.getId());
                showSuccess("Product deleted.");
                handleClearForm();
                refreshData();
            } catch (ProductNotFoundException e) {
                showError(e.getMessage());
            }
        }
    }

    @FXML
    private void handleClearForm() {
        productTable.getSelectionModel().clearSelection();
        selectedProduct = null;
        idField.clear();
        idField.setDisable(!Session.isAdmin());
        nameField.clear();
        categoryFormComboBox.setValue(null);
        priceField.clear();
        quantityField.clear();
        minStockField.clear();
        selectedProductLabel.setText("Selected Product: (none)");
        updateActionButtonsState();
    }

    @FXML
    private void handleStockIn() {
        adjustStock(true);
    }

    @FXML
    private void handleStockOut() {
        adjustStock(false);
    }

    @FXML
    private void handleBack() {
        try {
            Main.switchScene("view/dashboard.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ===================== Shared helpers =====================

    /**
     * Reads the Add/Edit form fields and builds a Product object out of
     * them. Numeric parsing errors are allowed to throw NumberFormatException,
     * which the calling handler catches and turns into a friendly Alert.
     */
    private Product buildProductFromForm() {
        String id = idField.getText() == null ? "" : idField.getText().trim();
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        Category category = categoryFormComboBox.getValue();

        if (category == null) {
            throw new InvalidProductException("Please select a category.");
        }

        double price = Double.parseDouble(priceField.getText().trim());
        int quantity = Integer.parseInt(quantityField.getText().trim());
        int minimumStock = Integer.parseInt(minStockField.getText().trim());

        return new Product(id, name, category, price, quantity, minimumStock);
    }

    /**
     * Shared logic for both Stock In and Stock Out, since they only differ
     * in which InventoryService method gets called.
     */
    private void adjustStock(boolean isStockIn) {
        if (!Session.isAdmin()) {
            return;
        }
        if (selectedProduct == null) {
            showError("Please select a product first.");
            return;
        }

        try {
            int amount = Integer.parseInt(stockAmountField.getText().trim());
            String productId = selectedProduct.getId();

            if (isStockIn) {
                inventoryService.stockIn(productId, amount);
                showSuccess("Stocked in " + amount + " unit(s) of " + selectedProduct.getName());
            } else {
                inventoryService.stockOut(productId, amount);
                showSuccess("Stocked out " + amount + " unit(s) of " + selectedProduct.getName());
            }

            stockAmountField.clear();
            refreshData();
            reselectProductById(productId); // keep the same row selected so the admin sees the new quantity
        } catch (NumberFormatException e) {
            showError("Amount must be a whole number.");
        } catch (InvalidProductException | InsufficientStockException | ProductNotFoundException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Re-selects a product by ID after the table data is reloaded.
     * (Reloading replaces the table's row list, which clears the selection.)
     */
    private void reselectProductById(String productId) {
        for (Product product : productTable.getItems()) {
            if (product.getId().equalsIgnoreCase(productId)) {
                productTable.getSelectionModel().select(product);
                break;
            }
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
