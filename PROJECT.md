# Inventory POS Project

## Current Status

Part 1 and Part 2 are complete and tested.

- Java 21
- Maven
- JavaFX
- Login system works (screen navigation only; no real auth yet)
- Admin and Cashier roles implemented
- Role-based access implemented
- File I/O for users implemented
- Product model with Category enum implemented
- Product data persists to data/products.txt (auto-created with sample data)
- ProductFileRepository handles all product File I/O
- ProductService handles product validation, search, and filtering
- InventoryService handles stock in/out and low-stock detection
- Custom exceptions: ProductNotFoundException, DuplicateProductException,
  InsufficientStockException, InvalidProductException

## Current Phase

Part 2 completed (Product and Inventory backend only — no UI screens yet).

Next phase: Part 3 — Inventory JavaFX screen (list/add/edit/delete products
in the Dashboard), using ProductService and InventoryService from the
controller layer. POS/cart/payment/checkout/reports come after that.

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
