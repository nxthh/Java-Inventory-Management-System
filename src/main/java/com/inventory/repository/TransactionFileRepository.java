package com.inventory.repository;

import com.inventory.model.Transaction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TransactionFileRepository is responsible for ALL reading and writing of
 * transaction data to and from the text file data/transactions.txt.
 *
 * OOP concept: SEPARATION OF RESPONSIBILITIES.
 * Just like ProductFileRepository, this class is the ONLY place in the
 * whole application that knows the transaction file's format. Services
 * and controllers never open this file themselves - they call
 * saveTransaction()/loadTransactions()/findTransaction() and let this
 * class deal with the messy details (missing files, corrupt lines, etc).
 *
 * Storage format: transactions are stored as an APPEND-ONLY log. Each
 * completed sale is one "block" of lines (see Transaction.toFileLines())
 * written to the end of the file. Old transactions are never rewritten,
 * so a huge transaction history never has to be reloaded and rewritten
 * just to add one more sale - unlike ProductFileRepository, which
 * rewrites the whole file because products can be edited/deleted.
 */
public class TransactionFileRepository {

    private static final String FILE_PATH = "data/transactions.txt";

    /**
     * Creates the repository and makes sure data/transactions.txt (and
     * the data folder) exists, so every other method can assume the file
     * is at least present.
     */
    public TransactionFileRepository() {
        createFileIfMissing();
    }

    /**
     * Reads data/transactions.txt from top to bottom and rebuilds every
     * valid transaction "block" (TRANSACTION line, its ITEM lines, and
     * an END line) into a Transaction object.
     *
     * Error handling: if one block is corrupt or incomplete (bad
     * numbers, missing END, an unexpected line, etc.), that ONE block is
     * skipped with a warning printed to the console - loading continues
     * with the rest of the file instead of crashing the application.
     *
     * @return an ArrayList of every transaction that loaded successfully,
     *         in the order they were saved (oldest first)
     */
    public List<Transaction> loadTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        Path path = Path.of(FILE_PATH);

        if (!Files.exists(path)) {
            return transactions; // nothing saved yet - not an error
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            List<String> currentBlock = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue; // skip blank lines
                }

                if (line.startsWith("TRANSACTION,")) {
                    // A new block is starting. If a previous block was
                    // left open without an END line, it is simply
                    // discarded here - fromFileLines() would have
                    // rejected it anyway.
                    currentBlock = new ArrayList<>();
                    currentBlock.add(line);
                } else if (line.startsWith("ITEM,")) {
                    currentBlock.add(line);
                } else if (line.equals("END")) {
                    currentBlock.add(line);
                    addParsedTransaction(transactions, currentBlock);
                    currentBlock = new ArrayList<>();
                } else {
                    System.out.println("Skipping unrecognized line in transaction file: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read transaction file: " + e.getMessage());
        }

        return transactions;
    }

    /**
     * Tries to parse one collected block of lines into a Transaction and
     * add it to the list. Any problem with the block (thrown by
     * Transaction.fromFileLines as an IllegalArgumentException) is
     * caught here so ONE malformed transaction never stops the rest of
     * the file from loading.
     */
    private void addParsedTransaction(List<Transaction> transactions, List<String> block) {
        try {
            transactions.add(Transaction.fromFileLines(block));
        } catch (IllegalArgumentException e) {
            System.out.println("Skipping invalid transaction record: " + e.getMessage());
        }
    }

    /**
     * Appends one completed transaction to the end of data/transactions.txt.
     * Existing transactions already in the file are left untouched -
     * this only adds new lines, it never rewrites the whole file.
     */
    public void saveTransaction(Transaction transaction) {
        try {
            Path path = Path.of(FILE_PATH);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            // "true" means append: keep everything already in the file
            // and write the new block after it.
            try (FileWriter writer = new FileWriter(FILE_PATH, true)) {
                for (String line : transaction.toFileLines()) {
                    writer.write(line);
                    writer.write(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            System.out.println("Could not save transaction " + transaction.getTransactionId()
                    + ": " + e.getMessage());
        }
    }

    /**
     * Removes ONE transaction (matched by ID) from data/transactions.txt
     * and saves the change. Unlike saveTransaction() (which only ever
     * appends), this has to rewrite the whole file - there is no way to
     * remove a block in the middle of a text file without rewriting
     * everything after it. This follows the exact same
     * "load everything, change the list, save everything" idea
     * ProductFileRepository.delete() already uses for products.
     */
    public void deleteTransaction(String transactionId) {
        List<Transaction> transactions = loadTransactions();
        transactions.removeIf(transaction -> transaction.getTransactionId().equalsIgnoreCase(transactionId));
        rewriteFile(transactions);
    }

    /**
     * Deletes EVERY saved transaction, leaving data/transactions.txt
     * completely empty. Used by the Admin-only "Clear All History"
     * action.
     */
    public void deleteAllTransactions() {
        rewriteFile(new ArrayList<>());
    }

    /**
     * Rewrites data/transactions.txt from scratch using the given list of
     * transactions - replacing whatever was there before. Both
     * deleteTransaction() and deleteAllTransactions() end by calling
     * this, the same way ProductFileRepository.saveAll() is the one
     * method every product add/update/delete funnels through.
     */
    private void rewriteFile(List<Transaction> transactions) {
        try {
            Path path = Path.of(FILE_PATH);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (FileWriter writer = new FileWriter(FILE_PATH)) {
                for (Transaction transaction : transactions) {
                    for (String line : transaction.toFileLines()) {
                        writer.write(line);
                        writer.write(System.lineSeparator());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Could not save transaction file: " + e.getMessage());
        }
    }

    /**
     * Searches every saved transaction for one matching the given ID.
     *
     * OOP concept: Optional is used instead of returning null, the same
     * way ProductFileRepository.findById() does - it clearly tells the
     * caller "this transaction might not exist" instead of risking a
     * NullPointerException.
     */
    public Optional<Transaction> findTransaction(String transactionId) {
        if (transactionId == null) {
            return Optional.empty();
        }
        for (Transaction transaction : loadTransactions()) {
            if (transaction.getTransactionId().equalsIgnoreCase(transactionId)) {
                return Optional.of(transaction);
            }
        }
        return Optional.empty();
    }

    /**
     * Works out the next transaction ID to use, e.g. "T0001", "T0002",
     * "T0003"... by looking at the highest numbered ID already saved in
     * the file and adding one. Because this always re-reads the file
     * instead of counting in memory, IDs stay unique even after the
     * application is restarted.
     */
    public String generateNextTransactionId() {
        int highestNumber = 0;

        for (Transaction transaction : loadTransactions()) {
            int number = extractNumber(transaction.getTransactionId());
            if (number > highestNumber) {
                highestNumber = number;
            }
        }

        int nextNumber = highestNumber + 1;
        return String.format("T%04d", nextNumber);
    }

    /**
     * Pulls the numeric part out of an ID like "T0007" -> 7.
     * Returns 0 for any ID that does not follow the expected "T####"
     * pattern, so one oddly-formatted ID cannot break ID generation.
     */
    private int extractNumber(String transactionId) {
        if (transactionId == null || !transactionId.startsWith("T")) {
            return 0;
        }
        try {
            return Integer.parseInt(transactionId.substring(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Creates the data folder and an empty transactions.txt file if they
     * do not already exist. Unlike products, transactions have no
     * "starter sample data" - the file simply starts empty and fills up
     * as real sales happen.
     */
    private void createFileIfMissing() {
        try {
            Path path = Path.of(FILE_PATH);
            if (Files.exists(path)) {
                return;
            }
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.createFile(path);
        } catch (IOException e) {
            System.out.println("Could not create transaction file: " + e.getMessage());
        }
    }
}
