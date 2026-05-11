package com.nhom9.auction.baitaplon_ltnc_nhom9.config;

/**
 * Cấu hình toàn cục cho ứng dụng.
 *
 * ─── CÁCH CHUYỂN SANG MYSQL ────────────────────────────────────────────────
 *  1. Đặt USE_MYSQL = true
 *  2. Điền MYSQL_HOST, MYSQL_DATABASE, MYSQL_USER, MYSQL_PASSWORD
 *  3. Bỏ comment dependency mysql-connector-j trong pom.xml
 *  4. Thêm "requires com.mysql.cj;" vào module-info.java
 *  5. Chạy mysql-schema.sql trên MySQL server của bạn
 * ──────────────────────────────────────────────────────────────────────────
 */
public final class AppConfig {

    private AppConfig() {}

    // ─── Chọn database ────────────────────────────────────────────────────────
    /**
     * false → SQLite (dùng trong development).
     * true  → MySQL  (dùng khi deploy hoặc test với server thật).
     */
    public static final boolean USE_MYSQL = false;

    // ─── SQLite ───────────────────────────────────────────────────────────────
    /** File SQLite nằm ở thư mục chạy app (project root khi dùng Maven). */
    public static final String SQLITE_PATH = "auction.db";
    public static final String SQLITE_URL  = "jdbc:sqlite:" + SQLITE_PATH;

    // ─── MySQL ────────────────────────────────────────────────────────────────
    public static final String MYSQL_HOST     = "localhost";
    public static final int    MYSQL_PORT     = 3306;
    public static final String MYSQL_DATABASE = "auction_db";
    public static final String MYSQL_USER     = "root";
    public static final String MYSQL_PASSWORD = "yourpassword";

    /**
     * JDBC URL cho MySQL. Các tham số quan trọng:
     *  - useUnicode + characterEncoding: đảm bảo tiếng Việt không bị lỗi
     *  - serverTimezone: chỉ định timezone để tránh lệch giờ
     *  - allowPublicKeyRetrieval=true + useSSL=false: cần thiết khi chạy MySQL 8 cục bộ
     */
    public static final String MYSQL_URL =
            "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/" + MYSQL_DATABASE
                    + "?useUnicode=true"
                    + "&characterEncoding=UTF-8"
                    + "&serverTimezone=Asia/Ho_Chi_Minh"
                    + "&allowPublicKeyRetrieval=true"
                    + "&useSSL=false";

    // ─── Connection Pool ──────────────────────────────────────────────────────
    /**
     * SQLite chỉ hỗ trợ 1 writer cùng lúc → pool size = 1.
     * MySQL xử lý nhiều connection song song → tăng lên 10-20 tùy server.
     */
    public static final int DB_POOL_SIZE     = USE_MYSQL ? 10 : 1;
    public static final int DB_MIN_IDLE      = USE_MYSQL ? 2  : 1;

    // ─── Schema files ─────────────────────────────────────────────────────────
    public static final String SCHEMA_FILE = USE_MYSQL
            ? "/db/docs/mysql-schema.sql"   // dùng cho MySQL
            : "/db/schema.sql";             // dùng cho SQLite (auto-run khi khởi động)

    public static final String SEED_FILE = "/db/seed.sql";

    // ─── Auction Rules ────────────────────────────────────────────────────────
    /** Phí nền tảng: 2% trên giá thắng đấu giá */
    public static final double PLATFORM_FEE_RATE = 0.02;

    /** Thời gian tối thiểu một phiên đấu giá (phút) */
    public static final int MIN_AUCTION_DURATION_MINUTES = 10;

    /** Thời gian tối đa một phiên đấu giá (ngày) */
    public static final int MAX_AUCTION_DURATION_DAYS = 30;

    /** Số giây gia hạn khi có bid trong phút cuối */
    public static final int LAST_MINUTE_EXTENSION_SECONDS = 60;

    // ─── Pagination ───────────────────────────────────────────────────────────
    public static final int DEFAULT_PAGE_SIZE = 12;

    // ─── UI ──────────────────────────────────────────────────────────────────
    public static final String APP_TITLE   = "Auction House";
    public static final String APP_VERSION = "1.0.0";

    /** Tiền tệ hiển thị */
    public static final String CURRENCY_SYMBOL = "đ";
}