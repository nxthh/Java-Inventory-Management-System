# Inventory POS Project

## Current Status

Parts 1, 2, 3, 4, 5, 6A, and 6B are complete and tested (Parts 4, 5, and
6B were verified with a real compiler and a real JavaFX runtime - see the
Testing Notes below).

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

Part 6B completed: Receipt generation, checkout integration (a receipt is
generated and shown after every successful sale), and a Transaction
History screen with role-based visibility.

Explicitly NOT built yet (by design, no such request so far): sales
reports/analytics (e.g. totals by day or by product), editing or voiding
a past transaction, printing a receipt to an actual printer (it is only
displayed on screen and saved as a `.txt` file), and a real
User/authentication system (Session/Role dropdown is still the simple
stand-in used since Part 3).

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
