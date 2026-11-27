package pocketledger;
import java.util.Scanner;import java.util.Map;
import java.util.LinkedHashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
public class ExpenseTracker {

	public static void main(String[] args) {
		System.out.println("PocketLedger");
		Path file = Path.of("expenses.tsv");
		if (Files.exists(file)) {
		    try {
		        System.out.println("Previous expenses:");
		        for (String line : Files.readAllLines(file)) {
		            System.out.println(line.replace("\t", ": ") + " KRW");
		        }
		    } catch (IOException error) {
		        System.out.println("Could not read expenses: " + error.getMessage());
		    }
		}
		Scanner input = new Scanner(System.in);
		int total = 0;Map<String, Integer> categoryTotals = new LinkedHashMap<>();
		ArrayList<String> expenseLines = new ArrayList<>();
		System.out.print("Number of expenses: ");
		while (!input.hasNextInt()) {
		    input.nextLine();
		    System.out.print("Please enter a whole number: ");
		}int count = input.nextInt();
		input.nextLine();
		for (int entry = 1; entry <= count; entry++) {
		System.out.print("Category: ");
		String category = input.nextLine();

		System.out.print("Amount (KRW): ");
		while (!input.hasNextInt()) {
		    input.nextLine();
		    System.out.print("Please enter a whole number: ");
		}int amount = input.nextInt();
		input.nextLine();System.out.println(category + ": " + amount + " KRW");
	    total += amount;int previous = categoryTotals.getOrDefault(category, 0);
	    categoryTotals.put(category, previous + amount);expenseLines.add(category + "\t" + amount);
	}
		System.out.println("Spending by category:");
		for (String name : categoryTotals.keySet()) {
		    System.out.println(name + ": " + categoryTotals.get(name) + " KRW");
		}System.out.println("Total: " + total + " KRW");

		System.out.print("Budget (KRW): ");
		while (!input.hasNextInt()) {
		    input.nextLine();
		    System.out.print("Please enter a whole number: ");
		}
		int budget = input.nextInt();
		if (total > budget) {
		    System.out.println("Over budget: " + (total - budget) + " KRW");
		} else {
		    System.out.println("Remaining budget: " + (budget - total) + " KRW");
		}

		

		try {
		    ExpenseHistory.appendRecords(file, expenseLines);
		    System.out.println("Saved to: " + file.toAbsolutePath());
		} catch (IOException error) {
		    System.out.println("Could not save expenses: " + error.getMessage());
		}input.close();
	}

}

