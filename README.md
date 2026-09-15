# Inventory Management + POS System

## Phase 1 (current)

JavaFX application skeleton. `Main` starts the app and switches between two screens:
- **Login** (`view/login.fxml` + `LoginController`)
- **Dashboard** (`view/dashboard.fxml` + `DashboardController`)

No real authentication yet — clicking "Login" just navigates to the Dashboard.
Real username/password checking will be added in Phase 2, once `User`, `Admin`,
and `Cashier` classes exist.

## How to open and run in IntelliJ IDEA

1. Open IntelliJ IDEA → **File > Open** → select the `inventory-pos` folder.
2. IntelliJ will detect `pom.xml` and import it as a Maven project automatically
   (this downloads the JavaFX libraries the first time — needs internet access).
3. Make sure IntelliJ is using a **Java 21 SDK**
   (File > Project Structure > Project > SDK).
4. Run the app one of two ways:
   - Open the Maven tool window (right sidebar) → `inventory-pos > Plugins > javafx > javafx:run`, or
   - Open a terminal in the project folder and run:
     ```
     mvn javafx:run
     ```

You should see the Login screen appear. Click **Login** to go to the Dashboard,
and **Logout** to go back.

## Project structure

```
inventory-pos/
├── pom.xml
├── data/                          # will hold .txt data files (Phase 2+)
└── src/main/
    ├── java/com/inventory/
    │   ├── Main.java              # JavaFX entry point, scene switching
    │   └── controller/
    │       ├── LoginController.java
    │       └── DashboardController.java
    └── resources/com/inventory/view/
        ├── login.fxml
        └── dashboard.fxml
```
