package pocketledger;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class ExpenseWindow {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createWindow(Path.of("expenses.tsv")).setVisible(true));
    }

    private static JFrame createWindow(Path file) {
        JFrame window = new JFrame("PocketLedger");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setMinimumSize(new Dimension(640, 420));
        window.setSize(640, 420);
        window.setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.add(new JLabel("Category:"));
        JTextField categoryField = new JTextField(15);
        panel.add(categoryField);
        panel.add(new JLabel("Amount (KRW):"));
        JTextField amountField = new JTextField(8);
        panel.add(amountField);
        JButton addButton = new JButton("Add");
        panel.add(addButton);

        JTextArea historyArea = new JTextArea();
        historyArea.setEditable(false);
        JTextArea analysisArea = new JTextArea();
        analysisArea.setEditable(false);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("History", new JScrollPane(historyArea));
        tabs.addTab("Spending Analysis", new JScrollPane(analysisArea));

        JPanel budgetPanel = new JPanel();
        budgetPanel.add(new JLabel("Budget (KRW):"));
        JTextField budgetField = new JTextField(9);
        budgetPanel.add(budgetField);
        JButton checkBudgetButton = new JButton("Check Budget");
        budgetPanel.add(checkBudgetButton);
        JLabel budgetResult = new JLabel();
        budgetPanel.add(budgetResult);

        window.add(panel, BorderLayout.NORTH);
        window.add(tabs, BorderLayout.CENTER);
        window.add(budgetPanel, BorderLayout.SOUTH);

        // Both tabs and the budget result come from the same saved records.
        Runnable refresh = () -> {
            try {
                ExpenseHistory history = ExpenseHistory.load(file);
                historyArea.setText(history.historyText());
                historyArea.setCaretPosition(0);
                analysisArea.setText(history.analysisText());
                analysisArea.setCaretPosition(0);
                budgetResult.setText(budgetStatus(budgetField.getText(), history.total()));
            } catch (IOException error) {
                historyArea.setText("Could not read expenses: " + error.getMessage());
                analysisArea.setText("Spending analysis is unavailable.");
                budgetResult.setText("Budget check unavailable.");
            }
        };

        addButton.addActionListener(event -> {
            String category = categoryField.getText().trim();
            String amountText = amountField.getText().trim();

            if (category.isEmpty()) {
                JOptionPane.showMessageDialog(window, "Please enter a category.");
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(amountText);
            } catch (NumberFormatException error) {
                JOptionPane.showMessageDialog(window, "Please enter a whole number.");
                return;
            }

            if (amount <= 0) {
                JOptionPane.showMessageDialog(window, "Amount must be greater than zero.");
                return;
            }

            try {
                String savedCategory = ExpenseHistory.append(file, category, amount);
                refresh.run();
                historyArea.setCaretPosition(historyArea.getDocument().getLength());
                categoryField.setText("");
                amountField.setText("");
                JOptionPane.showMessageDialog(window, "Saved: " + savedCategory + ": " + amount + " KRW");
                categoryField.requestFocusInWindow();
            } catch (IOException error) {
                JOptionPane.showMessageDialog(window, "Could not save expense: " + error.getMessage());
            }
        });

        checkBudgetButton.addActionListener(event -> refresh.run());
        budgetField.addActionListener(event -> checkBudgetButton.doClick());
        refresh.run();
        return window;
    }

    private static String budgetStatus(String input, long total) {
        String text = input.trim();
        if (text.isEmpty()) {
            return "Enter a budget.";
        }

        try {
            int budget = Integer.parseInt(text);
            if (budget >= 0) {
                if (total > budget) {
                    return "Over budget: " + (total - budget) + " KRW";
                }
                return "Remaining budget: " + (budget - total) + " KRW";
            }
        } catch (NumberFormatException invalidBudget) {
            // Fall through to the same message as negative input.
        }
        return "Use a non-negative whole number.";
    }
}
