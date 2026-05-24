package com.nhom9.auction.baitaplon_ltnc_nhom9.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Các hàm tiện ích cho database SQLite.
 */
public final class DbUtil {

    private DbUtil() {}

    /**
     * Format chuẩn để lưu timestamp vào SQLite.
     *
     * Tại sao không dùng LocalDateTime.toString()?
     * → toString() tạo ra "2025-05-10T14:30:00" (có chữ T)
     * → Nhưng SQLite datetime() trả về  "2025-05-10 14:30:00" (dấu cách)
     */
    public static final DateTimeFormatter DB_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ─── Thời gian ────────────────────────────────────────────────────────────

    /**
     * SQL fragment trả về thời gian hiện tại (SQLite).
     * Dùng trong các câu WHERE so sánh với cột datetime trong DB.
     *
     * Ví dụ: "WHERE end_time <= " + DbUtil.nowSql()
     */
    public static String nowSql() {
        return "datetime('now','localtime')";
    }

    /**
     * Chuyển LocalDateTime → String để lưu vào DB qua PreparedStatement.
     * Luôn dùng hàm này thay vì gọi LocalDateTime.toString() trực tiếp.
     */
    public static String toDbString(LocalDateTime t) {
        return t != null ? t.format(DB_FMT) : null;
    }

    /**
     * Chuyển String từ DB → LocalDateTime.
     * Thử cả hai format: dấu cách (từ SQLite) và chữ T (từ Java toString cũ).
     */
    public static LocalDateTime fromDbString(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s, DB_FMT);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(s);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    // ─── Kiểm tra tồn tại ─────────────────────────────────────────────────────

    /**
     * Kiểm tra một row có tồn tại trong bảng không.
     *
     * Dùng để quyết định INSERT hay UPDATE.
     *
     * Ví dụ: rowExists(conn, "buyers", "user_id", 5)
     * → "SELECT 1 FROM buyers WHERE user_id = 5"
     *
     * @param table  tên bảng
     * @param pkCol  tên cột primary key
     * @param id     giá trị cần kiểm tra
     */
    public static boolean rowExists(Connection conn, String table, String pkCol, int id)
            throws SQLException {
        String sql = "SELECT 1 FROM " + table + " WHERE " + pkCol + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
