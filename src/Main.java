import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

import connection.DBConfig;
import connection.DatabaseConnector;

/**
 * Simple CLI to interact with the `hospital_records` table.
 * Edit `DBConfig` static values at the top of `DBConfig.java` and then run this.
 * Requires the PostgreSQL JDBC driver on the classpath.
 */
public class Main {
    private static final String SCHEMA = "public";
    private static final String TABLE = "hospital_records";

    public static void main(String[] args) {
        DBConfig cfg;
        try {
            cfg = DBConfig.fromStatics();
        } catch (Exception e) {
            System.err.println("Error building DBConfig from statics: " + e.getMessage());
            return;
        }

        DatabaseConnector dc = new DatabaseConnector(cfg);

        // Quick test mode: run `java -cp "out;lib/postgresql.jar" connection.Main test`
        if (args != null && args.length > 0 && ("test".equals(args[0]) || "--test".equals(args[0]))) {
            System.out.println("Testing DB connection...");
            try (Connection c = dc.getConnection()) {
                System.out.println("Connection successful: " + (c.getMetaData() != null ? c.getMetaData().getURL() : "(no metadata)"));
            } catch (Exception e) {
                System.err.println("Connection failed: " + e.getMessage());
                e.printStackTrace(System.err);
            }
            return;
        }

        Scanner sc = new Scanner(System.in, "UTF-8");

        while (true) {
            System.out.println("\nChoose an action: \n1 = Select all\n2 = Insert\n3 = Update\n0 = Exit");
            System.out.print("> ");
            String choice = sc.nextLine().trim();
            if (choice.equals("0")) break;

            try {
                if (choice.equals("1")) {
                    List<Map<String, Object>> rows = dc.fetchAll(SCHEMA, TABLE);
                    for (Map<String, Object> r : rows) {
                        System.out.println(r);
                    }
                    System.out.println("Total: " + rows.size());

                } else if (choice.equals("2")) {
                    System.out.print("patient_id_hash (64 chars): ");
                    String pid = sc.nextLine().trim();
                    if (pid.length() != 64) {
                        System.out.println("Patient ID Hash must be exactly 64 characters");
                        continue;
                    }
                    System.out.print("patient_name: ");
                    String pname = sc.nextLine().trim();
                    System.out.print("patient_dob (YYYY-MM-DD) or empty: ");
                    String pdob = sc.nextLine().trim();
                    System.out.print("doctor_name: ");
                    String dname = sc.nextLine().trim();
                    System.out.print("nurse_name: ");
                    String nname = sc.nextLine().trim();

                    Map<String, Object> vals = new LinkedHashMap<>();
                    vals.put("patient_id_hash", pid);
                    vals.put("patient_name", pname.isEmpty() ? null : pname);
                    vals.put("patient_dob", pdob.isEmpty() ? null : pdob);
                    vals.put("doctor_name", dname.isEmpty() ? null : dname);
                    vals.put("nurse_name", nname.isEmpty() ? null : nname);

                    int inserted = dc.insert(SCHEMA, TABLE, vals);
                    System.out.println("Inserted rows: " + inserted);

                } else if (choice.equals("3")) {
                    System.out.print("record_index to update: ");
                    String idxs = sc.nextLine().trim();
                    int idx = Integer.parseInt(idxs);

                    List<String> allowed = Arrays.asList("patient_name", "patient_dob", "doctor_name", "nurse_name");
                    System.out.println("Allowed fields: " + allowed);
                    System.out.print("field to update: ");
                    String field = sc.nextLine().trim();
                    if (!allowed.contains(field)) {
                        System.out.println("Field not allowed.");
                        continue;
                    }

                    System.out.print("new value (empty to set NULL): ");
                    String newVal = sc.nextLine();

                    String sql = String.format("UPDATE %s.%s SET %s = ? WHERE record_index = %d", SCHEMA, TABLE, field, idx);
                    try (Connection c = dc.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                        if (newVal == null || newVal.isEmpty()) ps.setObject(1, null);
                        else ps.setObject(1, newVal);
                        int updated = ps.executeUpdate();
                        System.out.println("Updated rows: " + updated);
                    }

                } else {
                    System.out.println("Unknown choice");
                }
            } catch (Exception e) {
                System.err.println("Operation failed: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }

        sc.close();
        System.out.println("Goodbye.");
    }
}
