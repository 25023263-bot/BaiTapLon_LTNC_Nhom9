package org.example.baitaplon_ltnc_nhom9.config;

public class AppConfig {
    // Database
    public static final String DB_URL = "JDBC:sqlite:auction.db";
    public static final int DB_POOL_SIZE = 10;
    public static final String SCHEMA_FILE = "path/to/your/schema.sql";
    public static final String DB_PATH = "database.db";

    // Auction
    public static final long SCHEDULER_PERIOD_SECONDS = 60; // kiểm tra mỗi phút
    public static final double DEFAULT_MIN_BID_STEP_PERCENT = 5.0; // 5% of current price

    // Validation
    public static final double MAX_BID_AMOUNT = 1_000_000_000;
    public static final double MIN_BID_STEP_ABSOLUTE = 0.01;

    // UI
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final int PAGE_SIZE = 10;
}
