package com.nhom9.auction.baitaplon_ltnc_nhom9.service;

import com.nhom9.auction.baitaplon_ltnc_nhom9.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Singleton quản lý connection pool cho toàn ứng dụng.
 *
 * <h3>Tại sao cần Connection Pool?</h3>
 * Trước đây dùng một {@link Connection} duy nhất dùng chung cho tất cả thread.
 * SQLite không thread-safe khi nhiều thread cùng truy cập một Connection —
 * kết quả là mỗi thread tưởng connection "bị đứt" và liên tục tạo mới,
 * gây ra vòng lặp WARNING "Kết nối bị đứt – đang reconnect" trong log.
 *
 * <h3>HikariCP là gì?</h3>
 * HikariCP là thư viện connection pool nhanh nhất cho Java.
 * Thay vì một connection dùng chung, nó quản lý một "bể" (pool) nhiều connection:
 * <pre>
 *   Thread A ──▶ lấy connection #1 từ pool ──▶ dùng xong ──▶ trả lại pool
 *   Thread B ──▶ lấy connection #2 từ pool ──▶ dùng xong ──▶ trả lại pool
 *   Thread C ──▶ chờ nếu pool hết ──▶ lấy connection vừa được trả lại
 * </pre>
 * Không còn ai tranh nhau một connection nữa → không còn "bị đứt" giả.
 */
public class DatabaseConnection {

    private static final Logger LOG = Logger.getLogger(DatabaseConnection.class.getName());

    private static volatile DatabaseConnection instance;

    // Pool thay thế cho connection đơn lẻ
    private final HikariDataSource pool;

    // ─── Singleton ────────────────────────────────────────────────────────────

    private DatabaseConnection() {
        this.pool = buildPool();

        // Với SQLite: chạy schema tự động nếu chưa có bảng
        if (!AppConfig.USE_MYSQL) {
            runSchemaIfNeeded();
        } else {
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

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Lấy connection từ pool.
     *
     * <b>QUAN TRỌNG:</b> Luôn dùng trong try-with-resources để tự động trả lại pool:
     * <pre>{@code
     *   try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
     *       // dùng conn ở đây
     *   } // conn tự động được trả về pool khi ra khỏi block này
     * }</pre>
     *
     * Tại sao không cần reconnect nữa? Vì HikariCP tự kiểm tra và làm mới
     * connection khi cần — caller không cần quan tâm đến việc này.
     */
    public Connection getConnection() throws SQLException {
        return pool.getConnection();
    }

    /**
     * Đóng toàn bộ pool khi app tắt.
     * Gọi từ {@code HelloApplication.stop()} qua {@code ServiceLocator.shutdown()}.
     */
    public void close() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
            LOG.info("Connection pool đã đóng.");
        }
    }

    // ─── Pool setup ───────────────────────────────────────────────────────────

    /**
     * Tạo và cấu hình HikariCP pool phù hợp với database đang dùng.
     */
    private HikariDataSource buildPool() {
        HikariConfig config = new HikariConfig();

        if (AppConfig.USE_MYSQL) {
            config.setJdbcUrl(AppConfig.MYSQL_URL);
            config.setUsername(AppConfig.MYSQL_USER);
            config.setPassword(AppConfig.MYSQL_PASSWORD);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // Pool size cho MySQL: tối đa 10 connection đồng thời
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);

            // Timeout: chờ tối đa 30 giây nếu pool đang hết connection
            config.setConnectionTimeout(30_000);

            // Kiểm tra connection còn sống không trước khi dùng
            config.setConnectionTestQuery("SELECT 1");

            LOG.info("Khởi tạo HikariCP pool cho MySQL: "
                    + AppConfig.MYSQL_HOST + "/" + AppConfig.MYSQL_DATABASE);

        } else {
            config.setJdbcUrl(AppConfig.SQLITE_URL);
            config.setDriverClassName("org.sqlite.JDBC");

            // SQLite chỉ nên dùng 1 writer connection để tránh lock file
            // nhưng có thể dùng nhiều reader — WAL mode hỗ trợ điều này
            config.setMaximumPoolSize(1);
            config.setMinimumIdle(1);

            // Bật WAL và foreign keys cho mọi connection trong pool
            // (SQLite reset PRAGMA mỗi khi mở connection mới)
            config.setConnectionInitSql(
                    "PRAGMA journal_mode=WAL; " +
                            "PRAGMA foreign_keys=ON; " +
                            "PRAGMA synchronous=NORMAL;"
            );

            config.setConnectionTimeout(10_000);

            LOG.info("Khởi tạo HikariCP pool cho SQLite: " + AppConfig.SQLITE_PATH);
        }

        config.setPoolName("UBid-Pool");

        // Tắt log thừa của HikariCP (nó rất verbose mặc định)
        config.setLeakDetectionThreshold(60_000); // cảnh báo nếu connection bị giữ > 60s

        try {
            HikariDataSource ds = new HikariDataSource(config);
            LOG.info("✅ HikariCP pool khởi động thành công.");
            return ds;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Không thể khởi động connection pool.\n" +
                            (AppConfig.USE_MYSQL
                                    ? "Kiểm tra MySQL đang chạy và thông tin kết nối trong AppConfig."
                                    : "Kiểm tra đường dẫn SQLite: " + AppConfig.SQLITE_PATH),
                    e
            );
        }
    }

    // ─── Schema setup ─────────────────────────────────────────────────────────

    private void runSchemaIfNeeded() {
        try (Connection conn = pool.getConnection()) {
            if (tableExists(conn, "users")) {
                LOG.info("Schema SQLite đã tồn tại – bỏ qua runSchema().");
                return;
            }

            LOG.info("Bảng chưa tồn tại – đang tạo schema SQLite...");
            String sql = readSchemaFile();
            if (sql == null || sql.isBlank()) {
                throw new RuntimeException(
                        "Không đọc được schema.sql!\n" +
                                "Kiểm tra file tồn tại tại: src/main/resources/db/schema.sql"
                );
            }

            try (Statement stmt = conn.createStatement()) {
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
            }

        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Lỗi khi chạy schema: " + e.getMessage(), e);
            throw new RuntimeException("Không thể tạo schema database: " + e.getMessage(), e);
        }
    }

    private void verifyMySQLConnection() {
        try (Connection conn = pool.getConnection()) {
            if (!tableExists(conn, "users")) {
                LOG.severe(
                        "⚠️ Bảng 'users' không tìm thấy trong MySQL!\n" +
                                "Chạy: mysql -u " + AppConfig.MYSQL_USER + " -p " +
                                AppConfig.MYSQL_DATABASE + " < src/main/resources/db/docs/mysql-schema.sql"
                );
            } else {
                LOG.info("✅ Schema MySQL hợp lệ – bảng 'users' đã tồn tại.");
            }
        } catch (SQLException e) {
            LOG.severe("Không thể kiểm tra schema MySQL: " + e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean tableExists(Connection conn, String tableName) {
        try {
            String sql = AppConfig.USE_MYSQL
                    ? "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?"
                    : "SELECT name FROM sqlite_master WHERE type='table' AND name=?";

            try (var ps = conn.prepareStatement(sql)) {
                if (AppConfig.USE_MYSQL) {
                    ps.setString(1, AppConfig.MYSQL_DATABASE);
                    ps.setString(2, tableName);
                } else {
                    ps.setString(1, tableName);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            LOG.warning("Không thể kiểm tra bảng '" + tableName + "': " + e.getMessage());
            return false;
        }
    }

    private String readSchemaFile() {
        String[] paths = {"/db/schema.sql", "db/schema.sql"};
        for (String path : paths) {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) is = getClass().getClassLoader().getResourceAsStream(
                    path.startsWith("/") ? path.substring(1) : path);
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                } catch (IOException e) {
                    LOG.warning("Lỗi đọc schema: " + e.getMessage());
                }
            }
        }
        LOG.severe("Không tìm thấy schema.sql!");
        return null;
    }

    private boolean isOnlyComments(String sql) {
        for (String line : sql.split("\n")) {
            String t = line.strip();
            if (!t.isEmpty() && !t.startsWith("--")) return false;
        }
        return true;
    }
}