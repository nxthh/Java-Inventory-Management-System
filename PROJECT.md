# Inventory POS Project

## Current Status

Parts 1, 2, 3, 4, 5, 6A, 6B, 7, and 8 (Final Integration) are complete
(Parts 4, 5, and 6B were verified with a real compiler and a real
JavaFX runtime; Parts 7 and 8 could only be verified by hand-review -
see the Testing Notes below for why).

- **Part 8 - Final Integration (real login, File I/O hardening, CSS):**
  - **Real, file-based login with roles** - the single biggest gap found
    during the final audit. The Login screen previously only let the
    user pick a Role from a dropdown; there was no User account, no
    password check, and no `users.txt`. Added:
    - `User` (`model`, abstract) - the shared parent of `Admin` and
      `Cashier`, holding `username`/`password` and an abstract
      `getRole()`. This mirrors the existing `Payment` ->
      `CashPayment`/`CardPayment`/`QRPayment` inheritance pattern
      exactly, just for accounts instead of payments.
    - `Admin` and `Cashier` (`model`) - the two concrete subclasses.
    - `UserFileRepository` (`repository`) - all reading/writing of
      `data/users.txt`, following the exact same shape as
      `ProductFileRepository` (auto-creates the file with two default
      accounts if missing, skips a corrupted line instead of crashing).
    - `AuthService` (`service`) - validates the username/password fields
      aren't empty, looks up the account, and checks the password.
      Deliberately throws the SAME message ("Invalid username or
      password.") whether the username doesn't exist or the password is
      wrong, so a failed login never reveals which half was incorrect.
    - `InvalidLoginException` (`exception`) - follows the same small
      RuntimeException pattern as every other exception in the project.
    - `LoginController` and `login.fxml` were rewritten: the Role
      dropdown is gone; the username/password fields now go through
      `AuthService.login()`, and a successful login calls the new
      `Session.login(User)` helper.
    - `Session` gained ONE new method, `login(User)`, which just calls
      the two setters (`setCurrentRole`/`setCurrentUsername`) that
      already existed. Every other screen (`DashboardController`,
      `POSController`, `TransactionController`,
      `TransactionService`, `ReportService`, `CheckoutService`) keeps
      reading `Session.getCurrentRole()` / `Session.isAdmin()` /
      `Session.getCurrentUsername()` exactly as before - NONE of those
      files needed to change.
  - **Default accounts** (see also section B further down): `admin` /
    `admin123` (Admin role) and `cashier` / `cashier123` (Cashier role).
    Pre-seeded into `data/users.txt`; `UserFileRepository` also
    auto-creates the same two accounts if the file is ever deleted.
  - **File I/O robustness fix**: `ProductFileRepository.loadAll()` used
    to let one damaged line in `data/products.txt` (e.g. a missing
    field after a manual edit) throw an uncaught exception and crash
    the Inventory screen. It now skips that one line with a console
    warning and keeps loading the rest, the same way
    `TransactionFileRepository` already handled a corrupted transaction
    block. `UserFileRepository.loadAll()` was written with this same
    protection from the start.
  - **CSS theme**: `style.css` (`src/main/resources/com/inventory/view/`)
    is new - a single, simple theme (readable buttons, striped/readable
    tables, consistent form field borders, a light background) applied
    to EVERY screen from one place, `Main.switchScene()`, instead of
    being repeated in six different `.fxml` files.
  - No existing feature, file format, or class was removed or renamed.
    Parts 1-7 were re-verified by hand (see Part 8 Testing Notes) to
    confirm nothing broke.

- **Part 7 - Dashboard and Reports:**
  - `ReportService` (`service`): calculates every number shown on the
    Dashboard and the Reports screen. It does NOT read files or
    duplicate rules - it asks `ProductService` for the product list,
    `InventoryService` for which products are low stock, and
    `TransactionService` for which transactions the current user may
    see, then does simple counting/summing on top of that. Because
    both the Dashboard and the Reports screen call the exact same
    `ReportService` methods, their numbers can never disagree with
    each other.
  - The Dashboard (`dashboard.fxml` + `DashboardController`) now shows
    five summary cards - Total Products, Inventory Items, Low Stock,
    Transactions, Total Revenue - calculated live from
    `data/products.txt` and `data/transactions.txt` every time the
    screen loads (and again if "Refresh" is clicked). "Total
    Transactions" and "Total Revenue" follow the SAME
    ADMIN-sees-everything / CASHIER-sees-only-their-own-sales rule as
    the Transaction History screen, since both come from
    `TransactionService` underneath `ReportService`.
  - A new **Reports** screen (`reports.fxml` + `ReportsController`),
    reachable from a new Dashboard button, is **ADMIN-only**: the
    button is disabled for CASHIER on the Dashboard (like the
    Inventory button already was), and `ReportsController` also
    checks `Session.isAdmin()` again when the screen loads and sends
    a CASHIER straight back to the Dashboard with an "Access Denied"
    alert if they ever reach it another way (defense in depth, the
    same pattern `InventoryController` uses).
  - The Reports screen has three tabs:
    - **Inventory Report**: a `TableView` of every product (Product,
      Category, Price, Quantity, Inventory Value = price x quantity),
      plus a "Total Inventory Value" label.
    - **Sales Report**: Total Transactions / Total Revenue / Total
      Discount / Total Tax, calculated by summing
      `Transaction.getTotal()` / `getDiscountAmount()` /
      `getTaxAmount()` over the relevant transactions. Two
      `DatePicker` controls ("From"/"To") let the admin filter to a
      date range via `ReportService.filterByDateRange()`; leaving
      either picker empty leaves that side of the range open, and
      "Clear Filter" goes back to every visible transaction.
    - **Low Stock Report**: a `TableView` of only the products where
      `quantity <= minimumStock`, reusing
      `InventoryService.getLowStockProducts()` (via `ReportService`) -
      the exact same rule already used by the Inventory screen's
      "LOW STOCK" status column.
  - No new file format and no new data file were introduced - reports
    are calculated on the fly from the existing `products.txt` and
    `transactions.txt`, so nothing about how data is saved changed.

- **Part 6A - Transaction persistence:** `Transaction` (`model`) is now
  saved to `data/transactions.txt` by `TransactionFileRepository`
  (`repository`) as an append-only log (one block of lines per sale).
  IDs are generated as `T0001`, `T0002`, ... by scanning the file, so
  they never duplicate after a restart.
- **Part 6B - Receipts, checkout integration, and Transaction History:**
  - `ReceiptFileRepository` (`repository`) saves/loads one plain-text
    file per sale under `data/receipts/` (e.g. `R0001.txt`), and
    generates the next `R####` ID by scanning that folder - independent
    of the transaction ID sequence, and equally restart-safe.
  - `ReceiptService` (`service`) builds the receipt's text layout (store
    name, itemized products, subtotal/discount/tax/total, payment
    method, amount paid, change) and asks the repository to save/load
    it. Controllers never format or save a receipt themselves.
  - `Transaction` now also remembers the `receiptId` of the receipt
    generated for it, saved as one extra field on its file line. Older
    transactions saved before Part 6B (11 fields, no receiptId) still
    load correctly - `receiptId` simply comes back as `""` for those,
    and `Transaction.hasReceipt()` returns `false`.
  - `CheckoutService.checkout()` now also generates and saves a Receipt
    for every successful sale, right after the Transaction itself is
    saved and before stock is deducted (see the updated flow in that
    method's Javadoc). A failed payment still creates no Transaction, no
    Receipt, and touches no stock.
  - `POSController` shows the generated receipt in a simple popup
    (`ReceiptDialog`, a small reusable JavaFX helper using a `TextArea`)
    immediately after a successful checkout.
  - `TransactionService` (`service`) sits between the new
    `TransactionController` and `TransactionFileRepository`, and is
    where the ADMIN-sees-everything / CASHIER-sees-only-their-own-sales
    permission rule lives.
  - `transactions.fxml` + `TransactionController` (`controller`): a new
    Transaction History screen with a `TableView` (Transaction ID, Date,
    Cashier, Total, Payment Method), reachable from the Dashboard by
    both roles. Selecting a row and clicking "View Details" shows an
    itemized breakdown; "View Receipt" re-opens the exact saved receipt
    file for that sale, or shows a friendly message if no receipt was
    ever saved for it (e.g. Part 6A data) or the file is missing.

- Discount abstraction: `Discount` interface (`model`) with one
  implementation, `PercentageDiscount`, which takes a percentage off an
  amount (e.g. 10% of $100 = $10). Validates the percentage is between 0
  and 100.
- Tax: `TaxCalculator` (`service`) holds ONE configurable tax rate
  (currently 10%, set once in `POSController`) instead of a number
  scattered through the code. `finalTotal = subtotal - discount + tax`.
- Payment abstraction: `Payment` (abstract class, `model.payment`) with
  three subclasses - `CashPayment`, `CardPayment`, `QRPayment`. Card and
  QR are simulated (always succeed, no real gateway). Cash requires an
  Amount Paid, computes Change, and rejects an amount less than the
  total.
- `CheckoutTotals` (`model`): a small read-only holder for
  Subtotal/Discount/Tax/Total, used both for the POS screen's live
  preview and inside the real checkout.
- `Transaction` (`model`): a record of one completed sale (items,
  subtotal, discount, tax, total, the Payment used). Built at the end of
  checkout but NOT saved to a file yet - persistent transaction history
  is a later phase.
- `CheckoutService` (`service`): the business logic for checkout -
  validates the cart isn't empty, re-validates stock against the current
  saved data, calculates totals, processes the payment, and ONLY IF the
  payment succeeds deducts stock (via `InventoryService`) and clears the
  cart. If payment fails or is invalid, an exception is thrown before any
  stock is touched.
- POS screen now has a full checkout panel: Discount (%) field, live
  Discount/Tax/Total labels, a Payment Method ComboBox (Cash/Card/QR), an
  Amount Paid field (auto-disabled for Card/QR), a live Change label, and
  a Checkout button (disabled while the cart is empty). All of these
  update live as the cashier types/selects, before Checkout is even
  clicked.
- New exceptions: `PaymentException` (invalid/failed payment) and
  `InvalidDiscountException` (bad discount value), following the same
  pattern as the existing exception classes.
- Stock rule preserved and tested: adding to cart never changes saved
  stock; a failed payment leaves stock and the cart completely untouched;
  only a successful payment deducts stock.

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
- CartItem model (com.inventory.model): one line of a cart - a Product plus
  a quantity, with getSubtotal() = price x quantity.
- Cart model (com.inventory.model): owns a List<CartItem> and is the only
  class allowed to add/remove/update those items. Provides addItem,
  removeItem, updateQuantity, clear, getItems (returns a defensive copy),
  getSubtotal, isEmpty. Enforces that a product's total cart quantity never
  exceeds its current stock (InsufficientStockException) and rejects
  invalid input like a null product or a zero/negative quantity
  (InvalidCartOperationException, a new exception class alongside the
  existing four).
- POS screen (pos.fxml + POSController): search/select a product from a
  TableView (ID, Product, Category, Price, Stock), enter a quantity, and
  Add to Cart. A second TableView shows the current cart (Product,
  Quantity, Price, Subtotal) with Update Quantity / Remove Item / Clear
  Cart actions and a running Subtotal label. POSController never touches
  files or cart math directly - it calls ProductService for product lookup
  and Cart for everything cart-related.
- Dashboard now has a "Point of Sale" button, usable by BOTH Admin and
  Cashier (unlike the Inventory button, it is never disabled).
- Adding a product to the cart does NOT change the product's saved stock
  quantity yet - stock is only meant to be deducted at checkout, which is
  a later phase. The cart only checks stock, it never modifies it.

## Current Phase

Part 8 (Final Integration) completed: a real, file-based username/password
login with Admin/Cashier roles (`User`/`Admin`/`Cashier`, `UserFileRepository`,
`AuthService`), a File I/O robustness fix in `ProductFileRepository`, and a
shared `style.css` theme applied to every screen.

Explicitly NOT built yet (by design, no such request so far): reports
broken down by individual product or by cashier, exporting a report to
a file (e.g. CSV/PDF), charts/graphs of any kind, editing or voiding a
past transaction, printing a receipt to an actual printer (it is only
displayed on screen and saved as a `.txt` file), password hashing
(passwords are stored in plain text in `data/users.txt`, the same simple
style as every other data file in this project - acceptable for a
student project, NOT for a real production system), and an admin screen
for creating/editing/deleting user accounts through the UI (accounts are
managed by editing `data/users.txt` directly, the same way products
started out before the Inventory screen existed).

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

## Part 4 Testing Notes

Unlike Part 3, this sandbox was able to install a real JDK (`javac`) and
JavaFX libraries from Ubuntu's package repositories, so Part 4 was tested
with actual tools instead of by hand-review alone:
- `javac` compiled every source file in the project (Parts 1-4 together)
  against real JavaFX jars with zero errors.
- A standalone test (not part of the app) exercised `Cart` directly and
  covered every case from the Part 4 test list: empty cart, add, add the
  same product twice (quantities combine), change quantity, remove item,
  clear cart, insufficient stock on both add and update, invalid
  quantities, a missing product, and subtotal math. All 21 checks passed.
- A second standalone test loaded `pos.fxml` for real (via `FXMLLoader`,
  under a virtual display) and drove the actual controls - typing in the
  search box, selecting table rows, clicking the real "Add to Cart" /
  "Update Quantity" / "Remove Item" buttons - the same way a person
  clicking through the app would. All 15 checks passed.
- A third test ran the real `Main` class end-to-end and navigated
  Login -> Dashboard -> POS with no exceptions, confirming Part 4 did not
  break Parts 1-3.
- One real bug was caught and fixed this way: the first draft of
  pos.fxml had `<!-- ----- ... ----- -->` style comments, and XML does
  not allow `--` inside a comment. This crashed FXML loading until the
  comments were changed to `<!-- ===== ... ===== -->` (matching the style
  already used in inventory.fxml).
- These standalone tests are throwaway sandbox tools, not part of the
  project - they live outside `src/`, so nothing was added to the actual
  Maven project by this verification step.

Please still run `mvn clean javafx:run` in IntelliJ once, since that is
the real target environment (JavaFX 21 via Maven) and the one place a
learner will actually see the app run, and report back anything odd.

## Part 5 Testing Notes

This sandbox got a real JDK 21 (`javac`) installed again, plus JavaFX
(version 11, the newest available offline in this sandbox - IntelliJ will
use the real JavaFX 21 from Maven, per `pom.xml`, which is unaffected).
Part 5 was verified with real tools, not just by hand-review:

- `javac --release 21` compiled all 31 source files in the project
  (Parts 1-5 together) against the JavaFX jars with zero errors.
- A standalone test (not part of the app, lives outside `src/`) exercised
  `PercentageDiscount`, `TaxCalculator`, `CashPayment`, `CardPayment`,
  `QRPayment`, and `CheckoutService` directly with real objects and a
  real file-backed `ProductFileRepository`. All 27 checks passed,
  including:
  - 10% of $100 = $10; discount percentages outside 0-100 are rejected.
  - Tax on a $90 taxable amount at 10% = $9.
  - Cash payment: $20 paid on a $9.90 total gives $10.10 change; paying
    less than the total throws `PaymentException`.
  - Card and QR payments always succeed (simulated).
  - The SAME loop, using only the `Payment` type, called
    `processPayment()` on a `CashPayment`, a `CardPayment`, and a
    `QRPayment` and all three worked - demonstrating polymorphism.
  - `CheckoutService.calculateTotals()` matches the formula
    `subtotal - discount + tax` exactly ($5.00 subtotal, 10% discount ->
    $0.50, tax on $4.50 at 10% -> $0.45, total $4.95).
  - A FAILED cash checkout (paid less than the total) left stock and the
    cart completely unchanged.
  - A SUCCESSFUL checkout deducted the correct stock for every cart line,
    cleared the cart, and returned a `Transaction` with the correct
    total and change.
  - Adding more to a cart than is in stock is still rejected immediately
    (unchanged Part 4 behavior).
- A second standalone test loaded the real `pos.fxml` via `FXMLLoader`
  (under a virtual display, Xvfb) and drove the actual controls -
  selecting a product, typing a quantity, clicking the real "Add to
  Cart" button, typing into the real Discount and Amount Paid fields,
  and switching the real Payment ComboBox - the same way a cashier
  clicking through the app would. All 13 checks passed, confirming the
  live preview math (Discount/Tax/Total/Change labels) matches
  `CheckoutService`'s math exactly, and that the Amount Paid field
  correctly disables/clears when switching to Card.
- A third test ran the real `Main` class and navigated
  Login -> Dashboard -> POS (with the new checkout panel) -> Inventory
  with no exceptions, confirming Part 5 did not break Parts 1-4.
- These standalone tests are throwaway sandbox tools, not part of the
  project - they live outside `src/`, so nothing was added to the actual
  Maven project by this verification step. The project's own
  `data/products.txt` sample data was restored afterwards (the tests use
  a throwaway copy so the real sample data is never touched).

Please still run `mvn clean javafx:run` in IntelliJ to see the real
checkout panel in action, and report back anything odd.

## Part 6B Testing Notes

This sandbox had a real JDK 21 (`javac`/`java`) and OpenJFX 11 installed
(IntelliJ will use the real JavaFX 21 from Maven, per `pom.xml`, which is
unaffected). Part 6B was verified with real tools, not just by
hand-review:

- `javac --release 21` compiled every source file in the project (Parts
  1-6B together) against the JavaFX jars with zero errors.
- A standalone test (not part of the app, lives outside `src/`) ran
  against a throwaway COPY of the real `data/products.txt` and
  `data/transactions.txt` (the project's real `data/` folder was never
  touched). It exercised `CheckoutService`, `ReceiptService`,
  `ReceiptFileRepository`, `TransactionFileRepository`, and
  `TransactionService` directly with real file I/O. All 28 checks
  passed, including:
  - The existing Part 6A sample transaction (`T0001`, saved with the
    OLD 11-field line format) still loads correctly, and correctly
    reports it has no receipt (`hasReceipt() == false`).
  - A successful CASH checkout (2x Bread + 1x Coca-Cola, 10% discount)
    produced a `Transaction` with a real `receiptId`, deducted stock
    correctly, cleared the cart, and saved a receipt file to
    `data/receipts/R0001.txt` whose text contains the store name,
    receipt ID, cashier name, every purchased product, TOTAL, and
    Change.
  - The saved transaction reloads from `data/transactions.txt` with the
    SAME `receiptId` that was generated at checkout.
  - A FAILED cash payment (not enough cash) threw `PaymentException` and
    left the cart, stock, transaction file, AND receipts folder
    completely unchanged - no partial transaction or orphaned receipt
    was ever created.
  - Receipt IDs (`R0001`, `R0002`, ...) and transaction IDs (`T0001`,
    `T0002`, ...) both kept incrementing correctly from a brand-new
    repository instance, simulating an application restart.
  - A second successful (Card) checkout under a different logged-in
    user, followed by checking `TransactionService.getVisibleTransactions()`:
    an ADMIN session saw every transaction, while a CASHIER session saw
    only the transactions where they were the cashier - and specifically
    did NOT see the other user's sale.
  - Asking `ReceiptService` to load a receipt ID that does not exist
    (`R9999`) threw a `ReceiptException` instead of crashing.
  - Appending one deliberately corrupt transaction block (bad date, bad
    ITEM line) to `data/transactions.txt` and reloading: the corrupt
    block was skipped with a console warning, and every other
    transaction still loaded - confirming one bad record can't take down
    the whole history.
- A second standalone test loaded every real `.fxml` file in the project
  (`login`, `dashboard`, `pos`, `inventory`, and the new `transactions`)
  via `FXMLLoader` under a virtual display (Xvfb), the same way JavaFX
  itself loads them at runtime. All 5 files loaded with no exceptions and
  matched to their correct controller class, confirming every `fx:id`
  and `onAction="#method"` in the new/changed FXML lines up with a real
  `@FXML` field or method (this specifically caught nothing broken in
  `dashboard.fxml`'s new "Transaction History" button or the new
  `transactions.fxml` file).
- These standalone tests are throwaway sandbox tools, not part of the
  project - they live outside `src/`, so nothing was added to the actual
  Maven project by this verification step.

Please still run `mvn clean javafx:run` in IntelliJ to see the receipt
popup and the new Transaction History screen in action, and report back
anything odd.

## Part 7 Testing Notes

Unlike Parts 4, 5, and 6B, this sandbox no longer had network access to
install a JDK compiler (`javac`) - only a JavaFX-less JRE (`java`) was
already present, and `apt-get install openjdk-21-jdk-headless` failed
with `403 Forbidden` (no internet access). So Part 7 could **not** be
verified with a real `javac`/`mvn compile` or a live JavaFX run, unlike
the last three phases. Instead, it was verified the same careful way
Part 3 was, by hand:

- Every new/changed `fx:id` and `onAction="#method"` in `dashboard.fxml`
  and the new `reports.fxml` was cross-checked one by one against a
  matching `@FXML` field or method in `DashboardController` /
  `ReportsController` (names, generic types, and column value types all
  checked to line up).
- Every method `DashboardController` and `ReportsController` call on
  `ReportService`, `ProductService`, `InventoryService`, and
  `TransactionService` was checked against that method's real signature
  and return type.
- Every import was checked against every class/type actually referenced
  in each new/changed file.
- The XML itself (tag nesting/closing, one root content node per `Tab`,
  `<columns>` wrapper on each `TableView`) was checked by hand against
  the same patterns already used successfully in `inventory.fxml` and
  `transactions.fxml`.
- `ReportService`'s math was traced by hand against the project's real
  sample data:
  - `data/products.txt` (6 products) gives Total Products = 6, Total
    Inventory Quantity = 49+29+7+20+35+23 = **163**, Total Inventory
    Value = (1.50x49)+(1.50x29)+(2.00x7)+(2.50x20)+(1.25x35)+(3.00x23)
    = **$293.75**, and (at the current quantities) **0** products are
    low stock (each product's quantity is above its minimum).
  - `data/transactions.txt` (3 sample transactions, T0001-T0003) gives,
    for an ADMIN session: Total Transactions = **3**, Total Revenue =
    1.65+2.20+1.65 = **$5.50**, Total Discount = **$0.00**, Total Tax =
    0.15+0.20+0.15 = **$0.50**.
  - These are the exact numbers the Dashboard and Reports screen should
    show when the app is first run against the unmodified sample data,
    before any new sale is made.

Because this sandbox could not run the app, **please run
`mvn clean javafx:run` in IntelliJ** and check the following before
trusting this phase (all of Parts 1-6B should also still work exactly as
before - nothing about their files, methods, or FXML was removed or
renamed, only added to):

1. **Dashboard totals** - Login as ADMIN, confirm the five cards show
   Total Products = 6, Inventory Items = 163, Low Stock = 0,
   Transactions = 3, Total Revenue = $5.50 (matching the hand-traced
   numbers above, assuming the sample data files are still untouched).
2. **Inventory Report** - Dashboard -> Reports -> Inventory Report tab:
   6 rows, each row's Inventory Value = Price x Quantity, and "Total
   Inventory Value" = $293.75.
3. **Sales Report** - Reports -> Sales Report tab: the four numbers
   above with no filter applied; then pick a narrow "From"/"To" range
   that excludes some sample transactions and confirm the numbers drop
   accordingly; then "Clear Filter" and confirm they go back to the
   totals above.
4. **Low Stock Report** - Reports -> Low Stock Report tab: should be
   empty against the untouched sample data. Go to Inventory, Stock Out
   enough Bread (P003, min stock 5) to bring it to 5 or below, return to
   Reports -> Low Stock Report, and confirm Bread now appears with
   status LOW STOCK - and that the Dashboard's "Low Stock" card also
   goes from 0 to 1 after a refresh/navigation.
5. **Revenue/discount/tax after a new sale** - Make one POS sale with a
   discount, confirm the Dashboard's Total Revenue and Transactions and
   the Sales Report's four numbers all increase by exactly that sale's
   total/discount/tax.
6. **Persistence after restart** - Close and reopen the app; confirm the
   Dashboard and Reports numbers reload from `data/products.txt` and
   `data/transactions.txt` unchanged (no report data is stored
   separately - everything is recalculated fresh every time).
7. **CASHIER permissions** - Log in as CASHIER: the "Reports" button on
   the Dashboard should be disabled/greyed out, same as "Inventory"
   already is.
8. Confirm Parts 1-6B still work: Login, Inventory (add/edit/delete,
   stock in/out, search/filter), POS (cart, checkout, receipt popup),
   and Transaction History (list, View Details, View Receipt) should all
   behave exactly as they did before this phase.

Please report back anything odd so it can be fixed before continuing.

## Part 8 Testing Notes

Like Part 7, this sandbox had no working `javac`/`mvn` and no network
access (`apt-get install openjdk-21-jdk-headless` again failed with
`403 Forbidden`), so Part 8 was verified by careful hand-review instead
of a live compile:

- Every file's `package` declaration was checked against its actual
  folder (e.g. `model/User.java` declares `package com.inventory.model`).
- Every `import com.inventory.*` statement across all 46 source files was
  checked against a real file at the matching path - all resolved.
- Brace balance (`{` vs `}`) was checked in every source file.
- `login.fxml`'s `fx:id`s (`usernameField`, `passwordField`,
  `statusLabel`) and `onAction="#handleLogin"` were checked one-by-one
  against `LoginController`'s `@FXML` fields/methods - the removed
  `roleComboBox` was confirmed gone from BOTH the FXML and the
  controller (a `grep` across the whole `src/` tree found zero
  remaining references).
- The full login round-trip was traced by hand against the real
  `data/users.txt` this phase creates: `User.fromFileLine("admin,admin123,ADMIN")`
  splits into `["admin","admin123","ADMIN"]`, `Role.valueOf("ADMIN")`
  succeeds, and the `switch` returns `new Admin("admin","admin123")`;
  `AuthService.login("admin","admin123")` then finds that user and
  `checkPassword("admin123")` returns true - a successful login. Trying
  `AuthService.login("admin","wrong")` reaches the SAME
  `"Invalid username or password."` `InvalidLoginException` as trying a
  username that does not exist at all, by design (see Part 8 above).
- Confirmed EVERY method already called on `Session` from other files
  (`getCurrentRole`, `isAdmin`, `getCurrentUsername`,
  `setCurrentRole`, `setCurrentUsername`) still exists with the same
  signature - only one new method, `login(User)`, was added, and it is
  called from exactly one place (`LoginController`).
- Re-checked, line by line, that `DashboardController`, `InventoryController`,
  `POSController`, `TransactionController`, `TransactionService`,
  `ReportService`, and `CheckoutService` were NOT modified by this phase
  at all (only `LoginController`, `login.fxml`, `Session`, `Main`, and
  `ProductFileRepository` were touched) - confirming Parts 1-7 could not
  have regressed.
- All brand-new files created purely with `create_file` (not edited from
  existing content) were converted from LF to CRLF line endings to match
  every other file already in the repository - a purely cosmetic
  consistency check, not a compile concern.

Because this sandbox could not run the app, **please run
`mvn clean javafx:run` in IntelliJ** and check the following before
trusting this phase:

1. **Login works** - `admin` / `admin123` logs in as Admin (Inventory
   and Reports buttons enabled on the Dashboard); `cashier` / `cashier123`
   logs in as Cashier (those two buttons disabled/greyed out).
2. **Invalid login is rejected cleanly** - a wrong password, an unknown
   username, and empty fields each show a friendly Alert ("Invalid
   username or password." / "Please enter a username." / "Please enter
   a password.") instead of crashing, and the password field is cleared
   after a SUCCESSFUL login (so it is not left sitting in the field).
3. **CSS is visible** - buttons should be blue with white text (not the
   default grey JavaFX look), table headers should have a light grey
   background, and text fields should have a visible light border. This
   should look consistent across Login, Dashboard, Inventory, POS,
   Transactions, and Reports.
4. **Persistence after restart** - close and reopen the app; confirm
   `data/users.txt` still has exactly the accounts you left it with
   (including any the app auto-created on first run), and that both
   `admin`/`admin123` and `cashier`/`cashier123` still log in correctly.
5. **File I/O hardening** - with the app closed, open `data/products.txt`
   in a text editor and deliberately break one line (e.g. delete a
   comma so it only has 4 fields instead of 6), save, then reopen the
   app and go to Inventory. The screen should still load and show every
   OTHER product normally, with a line like `Skipping invalid product
   record: ...` printed to the IntelliJ console - it should NOT crash.
   Undo your edit afterwards to restore the real sample data.
6. **Everything from Parts 1-7 still works** - Inventory (add/edit/
   delete, stock in/out, search/filter, low stock), POS (cart, checkout
   with Cash/Card/QR, receipt popup), Transaction History (list, View
   Details, View Receipt, ADMIN-sees-all vs CASHIER-sees-own), and the
   Dashboard/Reports numbers should all behave exactly as they did
   before this phase - none of their files were changed.

Please report back anything odd so it can be fixed.

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
