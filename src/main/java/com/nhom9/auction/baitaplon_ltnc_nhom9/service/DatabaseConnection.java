package com.nhom9.auction.baitaplon_ltnc_nhom9.service;

import com.nhom9.auction.baitaplon_ltnc_nhom9.config.AppConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Singleton quản lý kết nối SQLite duy nhất cho toàn ứng dụng.
 * Thread-safe với double-checked locking.
 */
public class DatabaseConnection {

    private static final Logger LOG = Logger.getLogger(DatabaseConnection.class.getName());

    private static volatile DatabaseConnection instance;
    private Connection connection;

    // ─── Singleton ────────────────────────────────────────────────────────────

    private DatabaseConnection() {
        initConnection();
        applyPragmas();
        runSchema();
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

    private void initConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(AppConfig.DB_URL);
            LOG.info("Kết nối SQLite thành công: " + AppConfig.DB_PATH);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Không tìm thấy SQLite JDBC driver.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kết nối SQLite: " + e.getMessage(), e);
        }
    }

    /**
     * Bật WAL mode và foreign keys cho SQLite.
     */
    private void applyPragmas() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");
            stmt.execute("PRAGMA synchronous=NORMAL");
        } catch (SQLException e) {
            LOG.warning("Không áp dụng được PRAGMA: " + e.getMessage());
        }
    }

    /**
     * Chạy schema.sql nếu bảng chưa tồn tại.
     */
    private void runSchema() {
        String sql = readResourceFile(AppConfig.SCHEMA_FILE);
        if (sql == null || sql.isBlank()) {
            LOG.warning("Không đọc được schema.sql – bỏ qua.");
            return;
        }
        try (Statement stmt = connection.createStatement()) {
            // Tách và chạy từng statement riêng lẻ
            for (String s : sql.split(";")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) stmt.execute(trimmed);
            }
            LOG.info("Schema khởi tạo thành công.");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Lỗi chạy schema.sql: " + e.getMessage(), e);
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Lấy kết nối. Tự reconnect nếu đứt.
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                LOG.warning("Kết nối bị đứt – đang reconnect...");
                initConnection();
                applyPragmas();
            }
        } catch (SQLException e) {
            LOG.severe("Lỗi kiểm tra kết nối: " + e.getMessage());
            initConnection();
        }
        return connection;
    }

    /**
     * Đóng kết nối – gọi khi thoát app.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOG.info("Đã đóng kết nối SQLite.");
            }
        } catch (SQLException e) {
            LOG.warning("Lỗi đóng kết nối: " + e.getMessage());
        }
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    private String readResourceFile(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            LOG.warning("Không đọc được resource: " + path);
            return null;
        }
    }
}