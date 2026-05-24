package com.nhom9.auction.baitaplon_ltnc_nhom9.config;

/**
 * Global application configuration – SQLite only.
 */
public final class AppConfig {

    private AppConfig() {}

    // ─────────────────────────────────────────────────────────
    // Database
    // ─────────────────────────────────────────────────────────
    public static final String SQLITE_PATH = "auction.db";

    public static final String SQLITE_URL =
            "jdbc:sqlite:" + SQLITE_PATH;

    public static final String DB_URL = SQLITE_URL;

    // ─────────────────────────────────────────────────────────
    // Connection Pool (SQLite: 1 writer connection)
    // ─────────────────────────────────────────────────────────
    public static final int DB_POOL_SIZE = 1;
    public static final int DB_MIN_IDLE  = 1;

    // ─────────────────────────────────────────────────────────
    // Schema file
    // ─────────────────────────────────────────────────────────
    public static final String SCHEMA_FILE = "/db/schema.sql";

    public static final String SEED_FILE = "/db/seed.sql";

    // ─────────────────────────────────────────────────────────
    // Auction Rules
    // ─────────────────────────────────────────────────────────
    public static final double PLATFORM_FEE_RATE = 0.02;

    public static final int MIN_AUCTION_DURATION_MINUTES = 10;

    public static final int MAX_AUCTION_DURATION_DAYS = 30;

    // ─────────────────────────────────────────────────────────
    // Anti-sniping Algorithm
    //
    // Nếu có bid mới trong khoảng ANTI_SNIPE_WINDOW_SECONDS cuối
    // → tự động gia hạn thêm ANTI_SNIPE_EXTENSION_SECONDS.
    //
    // Ví dụ (giá trị mặc định):
    //   - Kết thúc dự kiến: 20:00:00
    //   - 19:59:10 có bid (còn 50s < window 60s)
    //   → Gia hạn đến 20:01:00 (thêm 60s)
    // ─────────────────────────────────────────────────────────

    /**
     * Cửa sổ kiểm tra (giây): nếu bid vào trong khoảng này trước khi hết → gia hạn.
     * Đặt 60 = gia hạn khi còn dưới 1 phút.
     */
    public static final int ANTI_SNIPE_WINDOW_SECONDS = 60;

    /** Số giây gia hạn thêm mỗi lần anti-snipe kích hoạt. */
    public static final int ANTI_SNIPE_EXTENSION_SECONDS = 60;

    /** @deprecated Dùng {@link #ANTI_SNIPE_EXTENSION_SECONDS} thay thế. */
    @Deprecated
    public static final int LAST_MINUTE_EXTENSION_SECONDS = ANTI_SNIPE_EXTENSION_SECONDS;

    // ─────────────────────────────────────────────────────────
    // Pagination
    // ─────────────────────────────────────────────────────────
    public static final int DEFAULT_PAGE_SIZE = 12;

    // ─────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────
    public static final String APP_TITLE   = "Auction House";
    public static final String APP_VERSION = "1.0.0";
    public static final String CURRENCY_SYMBOL = "đ";
}
