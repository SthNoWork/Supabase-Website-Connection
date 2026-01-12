import connection.Database;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    
    private static Database db;
    private static Scanner scanner;
    
    public static void main(String[] args) {
        db = new Database();
        scanner = new Scanner(System.in);
        
        try {
            // Connect to database
            db.connect();
            db.testConnection();
            
            // Main menu loop
            boolean running = true;
            while (running) {
                System.out.println("\n╔════════════════════════════════════════╗");
                System.out.println("║     DATABASE MANAGEMENT SYSTEM         ║");
                System.out.println("╚════════════════════════════════════════╝");
                System.out.println("1. View All Records");
                System.out.println("2. View Records with Filter");
                System.out.println("3. Insert New Record");
                System.out.println("4. Update Record");
                System.out.println("5. Delete Record");
                System.out.println("0. Exit");
                System.out.print("\nEnter your choice: ");
                
                int choice = getIntInput();
                
                switch (choice) {
                    case 1 -> viewAllRecords();
                    case 2 -> viewFilteredRecords();
                    case 3 -> insertRecord();
                    case 4 -> updateRecord();
                    case 5 -> deleteRecord();
                    case 0 -> {
                        running = false;
                        System.out.println("\n👋 Goodbye!");
                    }
                    default -> System.out.println("❌ Invalid choice. Please try again.");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
            db.close();
        }
    }
    
    private static void viewAllRecords() {
        try {
            System.out.println("\n📋 Fetching all records...\n");
            db.printTable();
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    private static void viewFilteredRecords() {
        try {
            System.out.println("\n🔍 Filter Records");
            System.out.print("How many filters? ");
            int filterCount = getIntInput();
            
            if (filterCount == 0) {
                db.printTable();
                return;
            }
            
            Map<String, Object> filters = new HashMap<>();
            for (int i = 0; i < filterCount; i++) {
                System.out.print("Filter #" + (i + 1) + " - Column name: ");
                String colName = scanner.nextLine().trim();
                
                System.out.print("Filter #" + (i + 1) + " - Value: ");
                String value = scanner.nextLine().trim();
                
                filters.put(colName, value);
            }
            
            db.printFiltered(filters);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    private static void insertRecord() {
        try {
            System.out.println("\n➕ Insert New Record");
            System.out.print("How many columns to insert? ");
            int colCount = getIntInput();
            
            Map<String, Object> data = new HashMap<>();
            for (int i = 0; i < colCount; i++) {
                System.out.print("Column #" + (i + 1) + " - Name: ");
                String colName = scanner.nextLine().trim();
                
                System.out.print("Column #" + (i + 1) + " - Value: ");
                String value = scanner.nextLine().trim();
                
                data.put(colName, value);
            }
            
            ResultSet rs = db.insert(data);
            
            System.out.println("\n✅ Successfully inserted record!");
            System.out.println("Returned data:");
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            if (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    String columnValue = rs.getString(i);
                    System.out.println("  " + columnName + " = " + columnValue);
                }
            }
            
            rs.close();
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void updateRecord() {
        try {
            System.out.println("\n✏️  Update Record");
            
            // Get WHERE clause
            System.out.println("\n--- WHERE Clause (which records to update) ---");
            System.out.print("How many filter conditions? ");
            int filterCount = getIntInput();
            
            Map<String, Object> filters = new HashMap<>();
            for (int i = 0; i < filterCount; i++) {
                System.out.print("Filter #" + (i + 1) + " - Column name: ");
                String colName = scanner.nextLine().trim();
                
                System.out.print("Filter #" + (i + 1) + " - Value: ");
                String value = scanner.nextLine().trim();
                
                filters.put(colName, value);
            }
            
            // Get SET clause
            System.out.println("\n--- SET Clause (what to update) ---");
            System.out.print("How many columns to update? ");
            int colCount = getIntInput();
            
            Map<String, Object> data = new HashMap<>();
            for (int i = 0; i < colCount; i++) {
                System.out.print("Column #" + (i + 1) + " - Name: ");
                String colName = scanner.nextLine().trim();
                
                System.out.print("Column #" + (i + 1) + " - New Value: ");
                String value = scanner.nextLine().trim();
                
                data.put(colName, value);
            }
            
            int rowsUpdated = db.update(data, filters);
            System.out.println("\n✅ Successfully updated " + rowsUpdated + " record(s)");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    private static void deleteRecord() {
        try {
            System.out.println("\n🗑️  Delete Record");
            
            System.out.print("How many filter conditions? ");
            int filterCount = getIntInput();
            
            Map<String, Object> filters = new HashMap<>();
            for (int i = 0; i < filterCount; i++) {
                System.out.print("Filter #" + (i + 1) + " - Column name: ");
                String colName = scanner.nextLine().trim();
                
                System.out.print("Filter #" + (i + 1) + " - Value: ");
                String value = scanner.nextLine().trim();
                
                filters.put(colName, value);
            }
            
            System.out.print("\n⚠️  Are you sure you want to delete these records? (yes/no): ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            
            if (confirm.equals("yes") || confirm.equals("y")) {
                int rowsDeleted = db.delete(filters);
                System.out.println("\n✅ Successfully deleted " + rowsDeleted + " record(s)");
            } else {
                System.out.println("❌ Delete cancelled");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    private static int getIntInput() {
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.print("❌ Invalid input. Please enter a number: ");
            }
        }
    }
}