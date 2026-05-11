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
 * Singleton quản lý kết nối database cho toàn ứng dụng.
 * Hỗ trợ cả SQLite (development) và MySQL (production).
 * Thread-safe với double-checked locking.
 *
 * ─── CÁCH HOẠT ĐỘNG ────────────────────────────────────────────────────────
 * Lớp này đọc AppConfig.USE_MYSQL để quyết định kết nối tới database nào.
 *  - USE_MYSQL = false → kết nối SQLite, tự động tạo schema nếu chưa có
 *  - USE_MYSQL = true  → kết nối MySQL, KHÔNG tự tạo schema
 *                        (vì schema MySQL phải được chạy tay một lần trước)
 * ──────────────────────────────────────────────────────────────────────────
 */
public class DatabaseConnection {

    private static final Logger LOG = Logger.getLogger(DatabaseConnection.class.getName());

    private static volatile DatabaseConnection instance;
    private Connection connection;

    // ─── Singleton ────────────────────────────────────────────────────────────

    /**
     * Constructor private: chỉ được gọi 1 lần duy nhất bởi getInstance().
     *
     * ✅ THAY ĐỔI: Tách logic SQLite và MySQL ra rõ ràng.
     * Trước đây luôn gọi applyPragmas() và runSchema() dù dùng MySQL
     * → gây lỗi vì PRAGMA là cú pháp riêng của SQLite, MySQL không hiểu.
     */
    private DatabaseConnection() {
        initConnection();

        if (!AppConfig.USE_MYSQL) {
            // PRAGMA và auto-schema chỉ áp dụng cho SQLite
            applyPragmas();
            runSchema();
        } else {
            // Với MySQL: schema đã được chạy tay bằng mysql-schema.sql
            // Chỉ kiểm tra kết nối có thành công không
            verifyMySQLConnection();
        }
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

    /**
     * Khởi tạo kết nối dựa vào AppConfig.USE_MYSQL.
     *
     * ✅ THAY ĐỔI CHÍNH: Thêm nhánh MySQL.
     * Trước đây hàm này hardcode SQLite → dù USE_MYSQL = true vẫn kết nối SQLite.
     *
     * Giải thích Class.forName():
     *   Dòng này "đăng ký" JDBC driver với Java runtime.
     *   MySQL driver (com.mysql.cj.jdbc.Driver) và SQLite driver khác nhau
     *   nên phải load đúng driver tương ứng.
     */
    private void initConnection() {
        try {
            if (AppConfig.USE_MYSQL) {
                // ── MySQL ──────────────────────────────────────────────────
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(
                        AppConfig.MYSQL_URL,
                        AppConfig.MYSQL_USER,
                        AppConfig.MYSQL_PASSWORD
                );
                LOG.info("✅ Kết nối MySQL thành công: "
                        + AppConfig.MYSQL_HOST + "/" + AppConfig.MYSQL_DATABASE);

            } else {
                // ── SQLite ─────────────────────────────────────────────────
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(AppConfig.SQLITE_URL);
                LOG.info("✅ Kết nối SQLite thành công: " + AppConfig.SQLITE_PATH);
            }

        } catch (ClassNotFoundException e) {
            // Driver không tìm thấy → thường do quên thêm dependency trong pom.xml
            // hoặc quên khai báo "requires" trong module-info.java
            throw new RuntimeException(
                    "Không tìm thấy JDBC driver cho " + (AppConfig.USE_MYSQL ? "MySQL" : "SQLite") + ".\n"
                            + (AppConfig.USE_MYSQL
                            ? "Kiểm tra: mysql-connector-j đã được bỏ comment trong pom.xml chưa?\n"
                            + "         'requires com.mysql.cj;' trong module-info.java chưa?"
                            : "Kiểm tra: sqlite-jdbc có trong pom.xml không?"), e);

        } catch (SQLException e) {
            // Kết nối thất bại → sai host/port, sai password, MySQL chưa chạy, v.v.
            String hint = AppConfig.USE_MYSQL
                    ? "\nGợi ý kiểm tra:\n"
                    + "  1. MySQL server đang chạy chưa?\n"
                    + "  2. MYSQL_PASSWORD trong AppConfig có đúng không?\n"
                    + "  3. Database '" + AppConfig.MYSQL_DATABASE + "' đã được tạo chưa?\n"
                    + "  4. mysql-schema.sql đã được chạy chưa?"
                    : "\nGợi ý: Kiểm tra đường dẫn file SQLite: " + AppConfig.SQLITE_PATH;

            throw new RuntimeException(
                    "Không thể kết nối " + (AppConfig.USE_MYSQL ? "MySQL" : "SQLite")
                            + ": " + e.getMessage() + hint, e);
        }
    }

    /**
     * Kiểm tra kết nối MySQL bằng cách thực thi một câu query đơn giản.
     * Nếu schema chưa được chạy → cảnh báo rõ ràng thay vì crash âm thầm.
     */
    private void verifyMySQLConnection() {
        // Kiểm tra bảng users có tồn tại không
        if (!tableExists("users")) {
            LOG.severe(
                    "⚠️  CẢNH BÁO: Bảng 'users' không tìm thấy trong database MySQL!\n"
                            + "   Bạn cần chạy file schema trước:\n"
                            + "   mysql -u " + AppConfig.MYSQL_USER + " -p "
                            + AppConfig.MYSQL_DATABASE + " < src/main/resources/db/docs/mysql-schema.sql"
            );
            // Không throw exception ở đây để app vẫn khởi động được
            // Lỗi thật sẽ xảy ra khi query đến bảng không tồn tại
        } else {
            LOG.info("✅ Schema MySQL hợp lệ – bảng 'users' đã tồn tại.");
        }
    }

    /**
     * Áp dụng các PRAGMA tối ưu hiệu năng cho SQLite.
     * PRAGMA là cú pháp riêng của SQLite, KHÔNG chạy được trên MySQL.
     *
     * Giải thích từng PRAGMA:
     *  - journal_mode=WAL : Write-Ahead Logging – cho phép đọc/ghi đồng thời
     *  - foreign_keys=ON  : Bật kiểm tra khóa ngoại (mặc định SQLite tắt)
     *  - synchronous=NORMAL: Cân bằng giữa tốc độ và an toàn dữ liệu
     */
    private void applyPragmas() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");
            stmt.execute("PRAGMA synchronous=NORMAL");
            LOG.info("SQLite PRAGMAs đã được áp dụng.");
        } catch (SQLException e) {
            LOG.warning("Không áp dụng được PRAGMA: " + e.getMessage());
        }
    }

    /**
     * Tự động chạy schema.sql để tạo bảng cho SQLite nếu chưa có.
     * Chỉ chạy khi USE_MYSQL = false.
     *
     * Tại sao MySQL không tự chạy schema?
     * → MySQL là server dùng chung, schema chỉ cần tạo một lần duy nhất.
     *   Nếu tự động chạy mỗi lần khởi động app, có thể vô tình DROP TABLE
     *   (vì mysql-schema.sql có lệnh DROP TABLE IF EXISTS ở đầu).
     */
    private void runSchema() {
        if (tableExists("users")) {
            LOG.info("Schema SQLite đã tồn tại – bỏ qua runSchema().");
            return;
        }

        LOG.info("Bảng chưa tồn tại – đang tạo schema SQLite...");

        String sql = readSchemaFile();
        if (sql == null || sql.isBlank()) {
            throw new RuntimeException(
                    "KHÔNG ĐỌC ĐƯỢC schema.sql!\n"
                            + "Hãy kiểm tra:\n"
                            + "  1. File tồn tại tại: src/main/resources/db/schema.sql\n"
                            + "  2. Đã chạy 'mvn clean compile' hoặc 'Reload Maven Project' trong IDE\n"
                            + "  3. Thư mục 'src/main/resources' được đánh dấu là Resources Root"
            );
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys=OFF");

            int tableCount = 0;
            for (String s : sql.split(";")) {
                String trimmed = s.strip();
                if (trimmed.isEmpty() || isOnlyComments(trimmed)) continue;
                if (trimmed.toUpperCase().startsWith("PRAGMA")) continue;

                stmt.execute(trimmed);
                if (trimmed.toUpperCase().contains("CREATE TABLE")) tableCount++;
            }

            stmt.execute("PRAGMA foreign_keys=ON");
            LOG.info("Schema SQLite khởi tạo thành công – " + tableCount + " bảng được tạo.");

        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Lỗi khi chạy schema.sql: " + e.getMessage(), e);
            throw new RuntimeException("Không thể tạo schema database: " + e.getMessage(), e);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Kiểm tra bảng có tồn tại không.
     *
     * ✅ THAY ĐỔI: Hỗ trợ cả SQLite và MySQL.
     * SQLite dùng bảng hệ thống "sqlite_master".
     * MySQL dùng INFORMATION_SCHEMA.TABLES.
     * Hai câu query khác nhau hoàn toàn → phải tách ra.
     */
    private boolean tableExists(String tableName) {
        try {
            if (AppConfig.USE_MYSQL) {
                // MySQL: kiểm tra qua INFORMATION_SCHEMA
                String sql = "SELECT 1 FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
                try (var ps = connection.prepareStatement(sql)) {
                    ps.setString(1, AppConfig.MYSQL_DATABASE);
                    ps.setString(2, tableName);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next();
                    }
                }
            } else {
                // SQLite: kiểm tra qua sqlite_master
                String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
                try (var ps = connection.prepareStatement(sql)) {
                    ps.setString(1, tableName);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next();
                    }
                }
            }
        } catch (SQLException e) {
            LOG.warning("Không thể kiểm tra bảng '" + tableName + "': " + e.getMessage());
            return false;
        }
    }

    private String readSchemaFile() {
        String path = AppConfig.SCHEMA_FILE;

        String content = readFromStream(getClass().getResourceAsStream(path));
        if (content != null) {
            LOG.info("Đọc schema từ class resource: " + path);
            return content;
        }

        String pathNoSlash = path.startsWith("/") ? path.substring(1) : path;
        content = readFromStream(getClass().getClassLoader().getResourceAsStream(pathNoSlash));
        if (content != null) {
            LOG.info("Đọc schema từ classloader: " + pathNoSlash);
            return content;
        }

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

    private boolean isOnlyComments(String sql) {
        for (String line : sql.split("\n")) {
            String trimmedLine = line.strip();
            if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("--")) {
                return false;
            }
        }
        return true;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Trả về Connection hiện tại, tự động reconnect nếu bị đứt.
     *
     * ✅ THAY ĐỔI: Khi reconnect với MySQL, không gọi applyPragmas()
     * (vì PRAGMA là SQLite-only, gọi trên MySQL sẽ lỗi).
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                LOG.warning("Kết nối bị đứt – đang reconnect...");
                initConnection();
                if (!AppConfig.USE_MYSQL) {
                    applyPragmas();
                }
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
                LOG.info("Đã đóng kết nối "
                        + (AppConfig.USE_MYSQL ? "MySQL" : "SQLite") + ".");
            }
        } catch (SQLException e) {
            LOG.warning("Lỗi đóng kết nối: " + e.getMessage());
        }
    }
}