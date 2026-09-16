package com.inventory.repository;

import com.inventory.exception.ReceiptException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReceiptFileRepository is responsible for ALL reading and writing of
 * receipt text files inside the data/receipts/ folder.
 *
 * OOP concept: SEPARATION OF RESPONSIBILITIES.
 * Just like ProductFileRepository and TransactionFileRepository, this is
 * the ONLY class in the whole application that knows where receipts live
 * on disk and what their file names look like. ReceiptService never opens
 * a file itself - it asks this class to save or load a receipt and lets
 * this class deal with the folder, the file name, and any I/O errors.
 *
 * Storage format: unlike products or transactions, a receipt is not saved
 * as structured data (comma-separated fields) - it is saved as ONE plain
 * text file per receipt, already formatted exactly the way it should be
 * printed/shown to the cashier. This keeps things simple: there is nothing
 * to "parse" back, the saved text IS the receipt.
 *
 * Each receipt is saved as: data/receipts/R0001.txt, data/receipts/R0002.txt, ...
 */
public class ReceiptFileRepository {

    private static final String RECEIPTS_FOLDER = "data/receipts";
    private static final Pattern RECEIPT_ID_PATTERN = Pattern.compile("R(\\d+)\\.txt", Pattern.CASE_INSENSITIVE);

    /**
     * Creates the repository and makes sure the data/receipts/ folder
     * exists, so every other method can assume the folder is at least
     * present.
     */
    public ReceiptFileRepository() {
        createFolderIfMissing();
    }

    /**
     * Works out the next receipt ID to use, e.g. "R0001", "R0002",
     * "R0003"... by looking at the highest numbered receipt file already
     * saved in data/receipts/ and adding one. Because this always
     * re-reads the folder instead of counting in memory, IDs stay unique
     * even after the application is restarted.
     */
    public String generateNextReceiptId() {
        int highestNumber = 0;
        Path folder = Path.of(RECEIPTS_FOLDER);

        if (Files.isDirectory(folder)) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.txt")) {
                for (Path file : files) {
                    int number = extractNumber(file.getFileName().toString());
                    if (number > highestNumber) {
                        highestNumber = number;
                    }
                }
            } catch (IOException e) {
                throw new ReceiptException("Could not read the receipts folder to generate a new receipt ID.", e);
            }
        }

        int nextNumber = highestNumber + 1;
        return String.format("R%04d", nextNumber);
    }

    /**
     * Saves the given receipt text to data/receipts/{receiptId}.txt.
     * If a file with that ID somehow already exists, it is overwritten -
     * this should not normally happen since generateNextReceiptId()
     * always returns a fresh, unused ID.
     *
     * @throws ReceiptException if the file cannot be written
     */
    public void saveReceipt(String receiptId, String content) {
        if (receiptId == null || receiptId.isBlank()) {
            throw new ReceiptException("Cannot save a receipt without a receipt ID.");
        }
        try {
            createFolderIfMissing();
            Path path = Path.of(RECEIPTS_FOLDER, receiptId + ".txt");
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ReceiptException("Could not save receipt " + receiptId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Reads a previously saved receipt back from disk.
     *
     * @throws ReceiptException if the receipt file does not exist or
     *                          cannot be read
     */
    public String loadReceipt(String receiptId) {
        if (receiptId == null || receiptId.isBlank()) {
            throw new ReceiptException("No receipt ID was provided.");
        }
        Path path = Path.of(RECEIPTS_FOLDER, receiptId + ".txt");
        if (!Files.exists(path)) {
            throw new ReceiptException("Receipt " + receiptId + " was not found.");
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ReceiptException("Could not read receipt " + receiptId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Checks whether a receipt with the given ID exists on disk, without
     * throwing if it does not. Useful for callers (like the Transaction
     * History screen) that want to disable a "View Receipt" button
     * instead of showing an error.
     */
    public boolean receiptExists(String receiptId) {
        if (receiptId == null || receiptId.isBlank()) {
            return false;
        }
        return Files.exists(Path.of(RECEIPTS_FOLDER, receiptId + ".txt"));
    }

    /**
     * Pulls the numeric part out of a file name like "R0007.txt" -> 7.
     * Returns 0 for any file name that does not follow the expected
     * pattern, so one oddly-named file cannot break ID generation.
     */
    private int extractNumber(String fileName) {
        Matcher matcher = RECEIPT_ID_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Creates the data/receipts/ folder (and data/ itself, if needed) if
     * it does not already exist.
     */
    private void createFolderIfMissing() {
        try {
            Files.createDirectories(Path.of(RECEIPTS_FOLDER));
        } catch (IOException e) {
            throw new ReceiptException("Could not create the data/receipts folder: " + e.getMessage(), e);
        }
    }
}
