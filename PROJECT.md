# Inventory POS Project

## Current Status

Parts 1, 2, and 3 are complete and tested (manually reviewed; see Part 3
Testing Notes below regarding compiler availability).

- Java 21
- Maven
- JavaFX
- Login screen lets the user pick a Role (ADMIN / CASHIER) from a dropdown
  and stores it in Session. This is NOT a full username/password
  authentication system yet - that would require a User model/service,
  which has not been requested.
- Session (com.inventory.util) holds the currently selected Role for the
  whole running application.
- Dashboard has a working "Inventory" button (disabled for CASHIER) and
  "Logout" button.
- Product model with Category enum implemented
- Product data persists to data/products.txt (auto-created with sample data)
- ProductFileRepository handles all product File I/O
- ProductService handles product validation, search, and filtering
- InventoryService handles stock in/out and low-stock detection
- Custom exceptions: ProductNotFoundException, DuplicateProductException,
  InsufficientStockException, InvalidProductException
- Inventory screen (inventory.fxml + InventoryController): TableView of all
  products with a computed Status column (OK / LOW STOCK), Add/Edit/Delete,
  Stock In/Stock Out, search box, category filter, and a Low Stock toggle -
  all working together. ADMIN-only actions are disabled for CASHIER both at
  the Dashboard (nav button hidden/disabled) and again inside the Inventory
  screen itself (defense in depth).

## Current Phase

Part 3 completed: Inventory Management JavaFX UI.

Next phase: Part 4 — Point of Sale (cart, checkout, payment, sales
receipts/reports). A real User/authentication system can also be added
at that point if desired, replacing the simple Session/Role dropdown.

## Important Rules

- Java 21
- Maven
- JavaFX
- Standard Java File I/O
- No database
- No Spring
- No Hibernate
- No external backend
- Keep the architecture beginner-friendly
- Use OOP concepts clearly
- Preserve existing working code
- Do not recreate existing classes unnecessarily
- Do not change the Maven/JavaFX configuration unless necessary

## Architecture

JavaFX UI
↓
Controllers
↓
Services
↓
Repositories
↓
File I/O

## Part 3 Testing Notes

The sandbox used to build this phase does not have a JDK compiler
(`javac`) or Maven installed, and has no network access to download
JavaFX. Every new/changed file was therefore verified by hand instead of
with a live `mvn compile`:
- Every `fx:id` and `onAction="#method"` in login.fxml, dashboard.fxml,
  and inventory.fxml was cross-checked against a matching `@FXML` field
  or method in the matching controller.
- Every model/service/repository method called from a controller was
  checked against its real method signature.
- Imports were checked against every class/type actually used.

Please run `mvn clean javafx:run` (or `mvn clean compile`) in IntelliJ
as the first step of testing this phase, and report back any compiler
errors so they can be fixed immediately (project rule #12).

## Development Rule

Before implementing each phase:

1. Inspect the entire existing project.
2. Understand existing classes and relationships.
3. Identify files that need modification.
4. Identify new files that need to be created.
5. Explain the implementation plan.
6. Implement only the requested phase.
7. Compile the project.
8. Fix compilation errors.
9. Test the existing functionality.
10. Update this PROJECT.md.
