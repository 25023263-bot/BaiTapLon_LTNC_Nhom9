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
 * Singleton quản lý connection pool (HikariCP) cho SQLite.
 *
 * <h3>Tại sao cần Connection Pool?</h3>
 * Trước đây dùng một {@link Connection} duy nhất dùng chung cho tất cả thread.
 * SQLite không thread-safe khi nhiều thread cùng truy cập một Connection —
 * kết quả là mỗi thread tưởng connection "bị đứt" và liên tục tạo mới,
 * gây ra vòng lặp WARNING trong log.
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

    private final HikariDataSource pool;

    // ─── Singleton ────────────────────────────────────────────────────────────

    private DatabaseConnection() {
        this.pool = buildPool();
        runSchemaIfNeeded();
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

    private HikariDataSource buildPool() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(AppConfig.SQLITE_URL);
        config.setDriverClassName("org.sqlite.JDBC");

        // SQLite chỉ nên dùng 1 writer connection để tránh lock file.
        // WAL mode hỗ trợ nhiều reader đồng thời nếu cần sau này.
        config.setMaximumPoolSize(AppConfig.DB_POOL_SIZE);
        config.setMinimumIdle(AppConfig.DB_MIN_IDLE);

        // Bật WAL và foreign keys cho mọi connection trong pool.
        // (SQLite reset PRAGMA mỗi khi mở connection mới)
        config.setConnectionInitSql(
                "PRAGMA journal_mode=WAL; " +
                        "PRAGMA foreign_keys=ON; " +
                        "PRAGMA synchronous=NORMAL;"
        );

        config.setConnectionTimeout(10_000);
        config.setPoolName("UBid-Pool");
        config.setLeakDetectionThreshold(60_000); // cảnh báo nếu connection bị giữ > 60s

        LOG.info("Khởi tạo HikariCP pool cho SQLite: " + AppConfig.SQLITE_PATH);

        try {
            HikariDataSource ds = new HikariDataSource(config);
            LOG.info("✅ HikariCP pool khởi động thành công.");
            return ds;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Không thể khởi động connection pool.\n" +
                            "Kiểm tra đường dẫn SQLite: " + AppConfig.SQLITE_PATH,
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

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean tableExists(Connection conn, String tableName) {
        try {
            String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
            try (var ps = conn.prepareStatement(sql)) {
                ps.setString(1, tableName);
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
