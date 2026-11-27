package pocketledger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the shared expense file and calculates totals from its saved records. */
final class ExpenseHistory {
    private final StringBuilder records = new StringBuilder();
    private final Map<String, Long> categoryTotals = new LinkedHashMap<>();
    private long total;
    private int skippedRows;

    static ExpenseHistory load(Path file) throws IOException {
        ExpenseHistory history = new ExpenseHistory();
        try {
            for (String line : Files.readAllLines(file)) {
                if (!line.isBlank()) {
                    history.readRecord(line);
                }
            }
        } catch (NoSuchFileException missingFile) {
            // A new ledger has no saved expenses yet.
        }
        return history;
    }

    static String append(Path file, String category, int amount) throws IOException {
        String savedCategory = category.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ').trim();
        appendRecords(file, List.of(savedCategory + "\t" + amount));
        return savedCategory;
    }

    static void appendRecords(Path file, Iterable<String> records) throws IOException {
        String lineBreak = System.lineSeparator();
        StringBuilder text = new StringBuilder();
        for (String record : records) {
            text.append(record).append(lineBreak);
        }

        // Separate the first new record if an existing file ends without a line break.
        if (text.length() > 0 && endsWithoutLineBreak(file)) {
            text.insert(0, lineBreak);
        }
        Files.writeString(file, text, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static boolean endsWithoutLineBreak(Path file) throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(file)) {
            long size = channel.size();
            if (size == 0) {
                return false;
            }
            channel.position(size - 1);
            ByteBuffer last = ByteBuffer.allocate(1);
            if (channel.read(last) != 1) {
                return false;
            }
            byte value = last.get(0);
            return value != '\n' && value != '\r';
        } catch (NoSuchFileException missingFile) {
            return false;
        }
    }

    private void readRecord(String line) {
        // The final tab separates the amount, even in older category names containing tabs.
        int separator = line.lastIndexOf('\t');
        if (separator == -1) {
            skippedRows++;
            return;
        }

        try {
            int amount = Integer.parseInt(line.substring(separator + 1).trim());
            String category = line.substring(0, separator).replace('\t', ' ').trim();
            if (category.isEmpty()) {
                category = "Uncategorized";
            }
            records.append(category).append(": ").append(amount).append(" KRW\n");
            categoryTotals.merge(category, (long) amount, Long::sum);
            total += amount;
        } catch (NumberFormatException invalidAmount) {
            skippedRows++;
        }
    }

    String historyText() {
        StringBuilder text = new StringBuilder("Previous expenses:\n");
        if (records.length() == 0) {
            text.append("No expenses saved yet.\n");
        } else {
            text.append(records);
        }
        if (skippedRows > 0) {
            text.append("Unreadable rows skipped: ").append(skippedRows).append('\n');
        }
        return text.toString();
    }

    String analysisText() {
        StringBuilder text = new StringBuilder("Spending by category:\n");
        for (Map.Entry<String, Long> entry : categoryTotals.entrySet()) {
            text.append(entry.getKey()).append(": ").append(entry.getValue()).append(" KRW\n");
        }
        text.append("\nTotal saved spending: ").append(total).append(" KRW\n");
        if (skippedRows > 0) {
            text.append("Totals exclude ").append(skippedRows).append(" unreadable row(s).\n");
        }
        return text.toString();
    }

    long total() {
        return total;
    }
}
