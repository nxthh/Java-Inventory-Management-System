package com.inventory.service;

import com.inventory.exception.UnauthorizedActionException;
import com.inventory.model.Transaction;
import com.inventory.repository.TransactionFileRepository;
import com.inventory.util.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TransactionService sits between TransactionController and
 * TransactionFileRepository, the same way ProductService sits between
 * InventoryController and ProductFileRepository.
 *
 * OOP concept: LAYERED ARCHITECTURE.
 * TransactionController never talks to TransactionFileRepository (or a
 * file) directly - it only calls this service. This keeps the controller
 * focused on JavaFX and lets the "who is allowed to see which
 * transactions" rule live in ONE place instead of inside the controller.
 */
public class TransactionService {

    private final TransactionFileRepository transactionFileRepository;

    public TransactionService(TransactionFileRepository transactionFileRepository) {
        this.transactionFileRepository = transactionFileRepository;
    }

    /**
     * Returns every saved transaction the CURRENTLY LOGGED IN user is
     * allowed to see:
     *   - ADMIN sees every transaction.
     *   - CASHIER sees only transactions where they were the cashier.
     */
    public List<Transaction> getVisibleTransactions() {
        List<Transaction> all = transactionFileRepository.loadTransactions();

        if (Session.isAdmin()) {
            return all;
        }

        String currentUser = Session.getCurrentUsername();
        List<Transaction> ownTransactions = new ArrayList<>();
        for (Transaction transaction : all) {
            if (transaction.getCashier().equalsIgnoreCase(currentUser)) {
                ownTransactions.add(transaction);
            }
        }
        return ownTransactions;
    }

    /**
     * Looks up one transaction by ID, regardless of who is logged in.
     * Used when a row is already selected in the (already-permission-
     * filtered) table, so no extra permission check is needed here.
     */
    public Optional<Transaction> findTransaction(String transactionId) {
        return transactionFileRepository.findTransaction(transactionId);
    }

    /**
     * Permanently deletes ONE saved transaction. ADMIN-only: the "Delete
     * Transaction" button on the Transaction History screen is already
     * disabled for a CASHIER, and this check is the same "defense in
     * depth" idea used everywhere else in the project (e.g.
     * ReportsController re-checking Session.isAdmin() when its screen
     * loads) - the rule is enforced here too, in case this method is
     * ever called another way.
     *
     * @throws UnauthorizedActionException if the current user is not an ADMIN
     */
    public void deleteTransaction(String transactionId) {
        requireAdmin("delete transaction history");
        transactionFileRepository.deleteTransaction(transactionId);
    }

    /**
     * Permanently deletes EVERY saved transaction. ADMIN-only, for the
     * same reason as deleteTransaction() above.
     *
     * @throws UnauthorizedActionException if the current user is not an ADMIN
     */
    public void clearAllHistory() {
        requireAdmin("clear transaction history");
        transactionFileRepository.deleteAllTransactions();
    }

    /**
     * Small shared helper: throws UnauthorizedActionException with a
     * clear message unless the current session belongs to an ADMIN.
     */
    private void requireAdmin(String actionDescription) {
        if (!Session.isAdmin()) {
            throw new UnauthorizedActionException("Only ADMIN users can " + actionDescription + ".");
        }
    }
}
