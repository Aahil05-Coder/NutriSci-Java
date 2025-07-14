package ca.yorku.eecs3311.nutrisci.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import static ca.yorku.eecs3311.nutrisci.util.DBUtil.getConnection;

public class DatabaseInitializer {
    private static final String DB_NAME = "nutrisci_db";

    private static final String[] CSV_FILES = {
        "NUTRIENT AMOUNT.csv",
        "CONVERSION FACTOR.csv",
        "FOOD GROUP.csv",
        "FOOD NAME.csv",
        "FOOD SOURCE.csv",
        "MEASURE NAME.csv",
        "NUTRIENT NAME.csv",
        "NUTRIENT SOURCE.csv",
        "REFUSE NAME.csv",
        "REFUSE AMOUNT.csv",
        "YIELD NAME.csv",
        "YIELD AMOUNT.csv"
    };

    public static void main(String[] args) throws Exception {
        System.out.println("[INIT] Starting DB initialization...");
        initializeDatabase();
        System.out.println("[INIT] Done.");
    }

    public static void initializeDatabase() throws Exception {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create database if not exists
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            stmt.execute("USE " + DB_NAME);

            for (String csv : CSV_FILES) {
                String table = csv
                    .replaceAll("\\.csv$", "").toLowerCase()
                    .replaceAll("[^a-z0-9]", "_")
                    .replaceAll("_+", "_");

                // 1) Ensure table exists (DDL from Resources header)
                ensureTableFromHeader(conn, csv, table);

                if (!isTableEmpty(conn, table)) {
                    System.out.println("[SKIP] Table " + table + " already has data.");
                    continue;
                }
                System.out.println("[LOAD] Importing " + csv + " into " + table);
                loadCsvViaInfile(conn, csv, table);
                System.out.println("[OK] Imported " + table);
            }

            System.out.println("[SCHEMA] Creating application tables...");
            SchemaInitializer.createApplicationTables(conn);
        }
    }

    private static void ensureTableFromHeader(Connection conn,
            String resource,
            String table)
            throws IOException, SQLException {

        InputStream in = DatabaseInitializer.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) throw new IOException("Resource not found: " + resource);

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        if (lines.isEmpty()) throw new IOException("Empty CSV resource: " + resource);

        String headerLine = lines.get(0);
        String[] cols = headerLine.split(",", -1);

        boolean hasDuplicate = false;
        Set<String> seen = new HashSet<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",", -1);
            if (parts.length == 0) continue;
            String key = parts[0].trim();
            if (!seen.add(key)) {
                hasDuplicate = true;
                break;
            }
        }

        StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
            .append(table).append(" (");
        for (int i = 0; i < cols.length; i++) {
            String raw = cols[i].trim();
            if (raw.isEmpty()) continue;
            String colName = raw.toLowerCase().replaceAll("[^a-z0-9]", "_").replaceAll("_+", "_");

            if (i == 0) {
                if (!hasDuplicate) {
                    ddl.append(colName).append(" INT PRIMARY KEY,");
                } else {
                    ddl.append(colName).append(" INT,");  // 不加主键
                }
            } else {
                ddl.append(colName).append(" VARCHAR(255),");
            }
        }
        ddl.setLength(ddl.length() - 1);
        ddl.append(") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

        try (Statement st = conn.createStatement()) {
            st.executeUpdate(ddl.toString());
        }

        if (hasDuplicate) {
            System.out.println("[WARN] " + table + ": duplicate in first column, skipped PRIMARY KEY");
        }
    }


    private static boolean isTableEmpty(Connection conn, String table) {
        String sql = "SELECT COUNT(*) FROM " + table;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next() && rs.getInt(1) > 0) return false;
        } catch (SQLException e) {
            return true;
        }
        return true;
    }

    private static void loadCsvViaInfile(Connection conn, String resource, String table)
            throws IOException, SQLException {
        Path tmp = Files.createTempFile("csv_import_" + UUID.randomUUID(), ".csv");
        try (InputStream in = DatabaseInitializer.class.getClassLoader()
                 .getResourceAsStream(resource)) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        String path = tmp.toAbsolutePath().toString().replace("\\", "/");

        String sql = String.join(" ",
            "LOAD DATA LOCAL INFILE '" + path + "'",
            "INTO TABLE", table,
            "CHARACTER SET utf8mb4",
            "FIELDS TERMINATED BY ','",
            "ENCLOSED BY '\"'",
            "LINES TERMINATED BY '\n'",
            "IGNORE 1 LINES"
        );

        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
