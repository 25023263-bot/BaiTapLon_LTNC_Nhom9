package com.nhom9.auction.baitaplon_ltnc_nhom9.config;

/**
 * Global application configuration.
 * Supports:
 *  - SQLite (local development)
 *  - Aiven MySQL (cloud)
 */
public final class AppConfig {

    private AppConfig() {}

    // ─────────────────────────────────────────────────────────
    // Database selection
    // ─────────────────────────────────────────────────────────
    public static final boolean USE_MYSQL = false;

    // ─────────────────────────────────────────────────────────
    // SQLite fallback
    // ─────────────────────────────────────────────────────────
    public static final String SQLITE_PATH = "auction.db";

    public static final String SQLITE_URL =
            "jdbc:sqlite:" + SQLITE_PATH;

    // ─────────────────────────────────────────────────────────
    // Aiven MySQL
    // ─────────────────────────────────────────────────────────
    public static final String MYSQL_HOST =
            "mysql-1e9dfdbb-ltnc-n9.i.aivencloud.com";

    public static final int MYSQL_PORT =
            18507;

    public static final String MYSQL_DATABASE =
            "defaultdb";

    public static final String MYSQL_USER =
            "avnadmin";

    /*
     * ⚠️ KHÔNG hardcode password thật khi push GitHub.
     *
     * Tốt nhất:
     *   đặt biến môi trường:
     *
     *   MYSQL_PASSWORD=xxxxx
     *
     * rồi đọc bằng:
     *   System.getenv(...)
     */
    public static final String MYSQL_PASSWORD =
            "AVNS_xddz0jIrydnKZLKYSpt";

    // ─────────────────────────────────────────────────────────
    // JDBC URL
    // ─────────────────────────────────────────────────────────
    public static final String MYSQL_URL =
            "jdbc:mysql://" +
                    MYSQL_HOST + ":" +
                    MYSQL_PORT + "/" +
                    MYSQL_DATABASE +
                    "?useUnicode=true" +
                    "&characterEncoding=UTF-8" +
                    "&serverTimezone=Asia/Ho_Chi_Minh" +
                    "&allowPublicKeyRetrieval=true" +
                    "&sslMode=REQUIRED";

    // ─────────────────────────────────────────────────────────
    // Active database URL
    // ─────────────────────────────────────────────────────────
    public static final String DB_URL =
            USE_MYSQL
                    ? MYSQL_URL
                    : SQLITE_URL;

    // ─────────────────────────────────────────────────────────
    // Connection Pool
    // ─────────────────────────────────────────────────────────
    public static final int DB_POOL_SIZE =
            USE_MYSQL ? 10 : 1;

    public static final int DB_MIN_IDLE =
            USE_MYSQL ? 2 : 1;

    // ─────────────────────────────────────────────────────────
    // Schema files
    // ─────────────────────────────────────────────────────────
    public static final String SCHEMA_FILE =
            USE_MYSQL
                    ? "/db/docs/mysql-schema.sql"
                    : "/db/schema.sql";

    public static final String SEED_FILE =
            "/db/seed.sql";

    // ─────────────────────────────────────────────────────────
    // Auction Rules
    // ─────────────────────────────────────────────────────────
    public static final double PLATFORM_FEE_RATE = 0.02;

    public static final int MIN_AUCTION_DURATION_MINUTES = 10;

    public static final int MAX_AUCTION_DURATION_DAYS = 30;

    public static final int LAST_MINUTE_EXTENSION_SECONDS = 60;

    // ─────────────────────────────────────────────────────────
    // Pagination
    // ─────────────────────────────────────────────────────────
    public static final int DEFAULT_PAGE_SIZE = 12;

    // ─────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────
    public static final String APP_TITLE =
            "Auction House";

    public static final String APP_VERSION =
            "1.0.0";

    public static final String CURRENCY_SYMBOL =
            "đ";
}

