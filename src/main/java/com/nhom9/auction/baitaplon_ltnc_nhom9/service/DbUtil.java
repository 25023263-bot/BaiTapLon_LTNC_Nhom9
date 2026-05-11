package com.nhom9.auction.baitaplon_ltnc_nhom9.service;

import com.nhom9.auction.baitaplon_ltnc_nhom9.config.AppConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Các hàm tiện ích cho database – xử lý sự khác biệt giữa SQLite và MySQL.
 *
 * ─── TẠI SAO CẦN FILE NÀY? ───────────────────────────────────────────────
 * SQLite và MySQL không hoàn toàn nói cùng "ngôn ngữ SQL":
 *  - Hàm thời gian: SQLite dùng datetime('now'), MySQL dùng NOW()
 *  - Định dạng timestamp: SQLite lưu TEXT, MySQL lưu DATETIME
 *  - Cú pháp UPSERT: SQLite dùng INSERT OR REPLACE, MySQL dùng ON DUPLICATE KEY
 *
 * Bằng cách tập trung các khác biệt vào một chỗ, khi đổi database ta chỉ
 * cần sửa ở đây thay vì tìm từng file repository.
 * ──────────────────────────────────────────────────────────────────────────
 */
public final class DbUtil {

    private DbUtil() {} // Không cho phép tạo instance

    /**
     * Format chuẩn để lưu timestamp vào DB.
     *
     * Tại sao không dùng LocalDateTime.toString()?
     * → toString() tạo ra "2025-05-10T14:30:00" (có chữ T)
     * → Nhưng SQLite datetime() trả về  "2025-05-10 14:30:00" (dấu cách)
     * → MySQL JDBC cũng trả về dấu cách → format này hoạt động nhất quán.
     */
    public static final DateTimeFormatter DB_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ─── Thời gian ────────────────────────────────────────────────────────────

    /**
     * SQL fragment trả về thời gian hiện tại.
     * Dùng trong các câu WHERE so sánh với cột datetime trong DB.
     *
     * Ví dụ: "WHERE end_time <= " + DbUtil.nowSql()
     */
    public static String nowSql() {
        return AppConfig.USE_MYSQL
                ? "NOW()"
                : "datetime('now','localtime')";
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
     * Thử cả hai format: dấu cách (từ DB) và chữ T (từ Java cũ).
     */
    public static LocalDateTime fromDbString(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            // Format chuẩn: "2025-05-10 14:30:00"
            return LocalDateTime.parse(s, DB_FMT);
        } catch (DateTimeParseException e) {
            try {
                // Format cũ: "2025-05-10T14:30:00" (Java toString)
                return LocalDateTime.parse(s);
            } catch (DateTimeParseException e2) {
                return null; // Dữ liệu lỗi → trả về null, không throw
            }
        }
    }

    // ─── Kiểm tra tồn tại ─────────────────────────────────────────────────────

    /**
     * Kiểm tra một row có tồn tại trong bảng không.
     *
     * Dùng để quyết định INSERT hay UPDATE (thay thế INSERT OR REPLACE của SQLite).
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
        // Lưu ý: tên bảng và cột KHÔNG dùng PreparedStatement được
        // vì JDBC không hỗ trợ bind tên bảng/cột.
        // Ở đây an toàn vì table và pkCol đều là hằng số từ code, không từ user input.
        String sql = "SELECT 1 FROM " + table + " WHERE " + pkCol + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}