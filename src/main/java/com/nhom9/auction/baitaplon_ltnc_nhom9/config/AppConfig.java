package com.nhom9.auction.baitaplon_ltnc_nhom9.config;

/**
 * Cấu hình toàn cục cho ứng dụng.
 * Thay đổi DB_PATH nếu muốn lưu file SQLite ở vị trí khác.
 */
public final class AppConfig {

    private AppConfig() {}

    // ─── Database ─────────────────────────────────────────────────────────────

    /** Đường dẫn file SQLite (tương đối với thư mục chạy app) */
    public static final String DB_PATH     = "auction.db";
    public static final String DB_URL      = "jdbc:sqlite:" + DB_PATH;

    /** File SQL khởi tạo schema */
    public static final String SCHEMA_FILE = "/db/schema.sql";
    public static final String SEED_FILE   = "/db/seed.sql";

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