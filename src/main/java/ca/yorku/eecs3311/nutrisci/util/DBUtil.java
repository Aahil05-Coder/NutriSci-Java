package ca.yorku.eecs3311.nutrisci.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public class DBUtil {
    private static final String HOST_URL =
        "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC"
      + "&allowLoadLocalInfile=true&rewriteBatchedStatements=true";

    private static final String DB_URL =
        "jdbc:mysql://localhost:3306/nutrisci_db?useSSL=false&serverTimezone=UTC"
      + "&allowLoadLocalInfile=true&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true";

    private static final String DB_NAME  = "nutrisci_db";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private static Connection conn = null;

    public static Connection getConnection() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            return conn;
        }

        try {
            conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1049 || e.getMessage().contains("Unknown database")) {
                try (Connection tmp = DriverManager.getConnection(HOST_URL, USER, PASSWORD);
                     Statement stmt = tmp.createStatement()) {
                    stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
                }
                conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            } else {
                throw e;
            }
        }
        return conn;
    }
}
