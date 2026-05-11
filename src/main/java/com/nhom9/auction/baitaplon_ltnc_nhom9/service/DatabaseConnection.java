package com.nhom9.auction.baitaplon_ltnc_nhom9.service;

import com.nhom9.auction.baitaplon_ltnc_nhom9.config.AppConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Singleton quản lý kết nối SQLite duy nhất cho toàn ứng dụng.
 * Thread-safe với double-checked locking.
 *
 * Phiên bản này giữ nguyên kiến trúc singleton (không dùng HikariCP)
 * nhưng sửa vấn đề schema không được load.
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
            connection = DriverManager.getConnection(AppConfig.SQLITE_URL);
            LOG.info("Kết nối SQLite thành công: " + AppConfig.SQLITE_PATH);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Không tìm thấy SQLite JDBC driver.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kết nối SQLite: " + e.getMessage(), e);
        }
    }

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
     * Chạy schema.sql để tạo bảng nếu chưa có.
     *
     * FIX: Phiên bản cũ silently bỏ qua nếu không đọc được file.
     * Phiên bản này:
     *  1. Kiểm tra tables đã tồn tại chưa trước khi chạy schema
     *  2. Thử nhiều cách đọc file schema
     *  3. Log rõ ràng nếu có lỗi
     */
    private void runSchema() {
        // Kiểm tra xem bảng users đã tồn tại chưa
        if (tableExists("users")) {
            LOG.info("Schema đã tồn tại – bỏ qua runSchema().");
            return;
        }

        LOG.info("Bảng chưa tồn tại – đang tạo schema...");

        String sql = readSchemaFile();
        if (sql == null || sql.isBlank()) {
            // Lỗi nghiêm trọng: không đọc được schema → app không dùng được
            throw new RuntimeException(
                    "KHÔNG ĐỌC ĐƯỢC schema.sql!\n" +
                            "Hãy kiểm tra:\n" +
                            "  1. File tồn tại tại: src/main/resources/db/schema.sql\n" +
                            "  2. Đã chạy 'mvn clean compile' hoặc 'Reload Maven Project' trong IDE\n" +
                            "  3. Thư mục 'src/main/resources' được đánh dấu là Resources Root"
            );
        }

        try (Statement stmt = connection.createStatement()) {
            // Tắt foreign keys tạm thời khi tạo schema để tránh lỗi thứ tự
            stmt.execute("PRAGMA foreign_keys=OFF");

            int tableCount = 0;
            for (String s : sql.split(";")) {
                String trimmed = s.strip();
                // Bỏ qua dòng trống và comment thuần túy
                if (trimmed.isEmpty() || isOnlyComments(trimmed)) continue;
                // Bỏ qua PRAGMA trong schema (đã xử lý ở applyPragmas)
                if (trimmed.toUpperCase().startsWith("PRAGMA")) continue;

                stmt.execute(trimmed);
                if (trimmed.toUpperCase().contains("CREATE TABLE")) tableCount++;
            }

            // Bật lại foreign keys
            stmt.execute("PRAGMA foreign_keys=ON");
            LOG.info("Schema khởi tạo thành công – " + tableCount + " bảng được tạo.");

        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Lỗi khi chạy schema.sql: " + e.getMessage(), e);
            throw new RuntimeException("Không thể tạo schema database: " + e.getMessage(), e);
        }
    }

    /**
     * Đọc file schema bằng 3 cách khác nhau để đảm bảo tìm được.
     * Cách 1: getResourceAsStream với đường dẫn tuyệt đối (chuẩn)
     * Cách 2: ClassLoader (đôi khi hoạt động khi cách 1 không được)
     * Cách 3: Thread context ClassLoader
     */
    private String readSchemaFile() {
        String path = AppConfig.SCHEMA_FILE; // "/db/schema.sql"

        // Cách 1: Class-level resource (path tuyệt đối bắt đầu bằng /)
        String content = readFromStream(getClass().getResourceAsStream(path));
        if (content != null) {
            LOG.info("Đọc schema từ class resource: " + path);
            return content;
        }

        // Cách 2: ClassLoader resource (không có / ở đầu)
        String pathNoSlash = path.startsWith("/") ? path.substring(1) : path;
        content = readFromStream(getClass().getClassLoader().getResourceAsStream(pathNoSlash));
        if (content != null) {
            LOG.info("Đọc schema từ classloader: " + pathNoSlash);
            return content;
        }

        // Cách 3: Thread context classloader
        content = readFromStream(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(pathNoSlash));
        if (content != null) {
            LOG.info("Đọc schema từ thread classloader: " + pathNoSlash);
            return content;
        }

        LOG.severe("Không tìm thấy schema.sql qua bất kỳ classloader nào!");
        return null;
    }

    private String readFromStream(InputStream is) {
        if (is == null) return null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Kiểm tra bảng có tồn tại trong DB chưa.
     */
    private boolean tableExists(String tableName) {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Kiểm tra một đoạn SQL chỉ chứa comment không (không có lệnh thật).
     */
    private boolean isOnlyComments(String sql) {
        for (String line : sql.split("\n")) {
            String trimmedLine = line.strip();
            if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("--")) {
                return false; // Có ít nhất 1 dòng không phải comment
            }
        }
        return true;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

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
}