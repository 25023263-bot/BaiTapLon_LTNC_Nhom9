package com.nhom9.auction.baitaplon_ltnc_nhom9.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final String DB_HOST = readValue("auction.db.host", "AUCTION_DB_HOST", "mysql-1c7856e0-nguyenducminhtl27-feca.b.aivencloud.com");
    private static final String DB_PORT = readValue("auction.db.port", "AUCTION_DB_PORT", "14372");
    private static final String DB_NAME = readValue("auction.db.name", "AUCTION_DB_NAME", "defaultdb");
    private static final String DB_USER = readValue("auction.db.user", "AUCTION_DB_USER", "avnadmin");
    private static final String DB_PASSWORD = readValue("auction.db.password", "AUCTION_DB_PASSWORD", "AVNS_6OE5hIWpRyWc3wjjWFo");

    private static final String URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
                    + "?sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static Connection connection = null;
    private static boolean schemaInitialized = false;

    private DatabaseConnection() {
    }

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
            }
        } catch (SQLException e) {
            System.err.println("DB connection error: " + e.getMessage());
            connection = null;
        }
        return connection;
    }

    public static synchronized boolean ensureSchemaInitialized() {
        if (schemaInitialized) {
            return true;
        }

        Connection conn = getConnection();
        if (conn == null) {
            return false;
        }

        try {
            createUsersTableIfMissing(conn);
            migrateUsersTableIfNeeded(conn);
            seedDefaultUsersIfEmpty(conn);
            schemaInitialized = true;
            return true;
        } catch (SQLException e) {
            System.err.println("DB schema init error: " + e.getMessage());
            return false;
        }
    }

    private static void createUsersTableIfMissing(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users ("
                + "id INT NOT NULL AUTO_INCREMENT, "
                + "full_name VARCHAR(100) NOT NULL, "
                + "username VARCHAR(50) NOT NULL, "
                + "password VARCHAR(255) NOT NULL, "
                + "balance DECIMAL(15,2) DEFAULT 0.00, "
                + "created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP, "
                + "PRIMARY KEY (id), "
                + "UNIQUE KEY username (username)"
                + ")";
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        }
    }

    private static void migrateUsersTableIfNeeded(Connection conn) throws SQLException {
        if (!columnExists(conn, "users", "email")) {
            return;
        }

        try (Statement statement = conn.createStatement()) {
            if (indexExists(conn, "users", "email")) {
                statement.execute("ALTER TABLE users DROP INDEX email");
            }
            statement.execute("ALTER TABLE users DROP COLUMN email");
        }
    }

    private static void seedDefaultUsersIfEmpty(Connection conn) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM users";
        try (Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        String insertSql = "INSERT INTO users (full_name, username, password, balance) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            insertUser(stmt, "Administrator", "admin", "1234", 0.0);
            insertUser(stmt, "Admin One", "admin1", "1234", 0.0);
            insertUser(stmt, "Ly", "ly123", "1234", 0.0);
        }
    }

    private static void insertUser(PreparedStatement stmt, String fullName, String username,
                                   String password, double balance) throws SQLException {
        stmt.setString(1, fullName);
        stmt.setString(2, username);
        stmt.setString(3, password);
        stmt.setDouble(4, balance);
        stmt.executeUpdate();
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            stmt.setString(2, columnName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static boolean indexExists(Connection conn, String tableName, String indexName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.statistics "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            stmt.setString(2, indexName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static String readValue(String systemProperty, String envName, String defaultValue) {
        String fromProperty = System.getProperty(systemProperty);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }

        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return defaultValue;
    }
}
