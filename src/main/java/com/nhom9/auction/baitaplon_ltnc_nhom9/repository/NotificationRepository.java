package com.nhom9.auction.baitaplon_ltnc_nhom9.repository;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Notification;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DatabaseConnection;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DbUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC repository cho bảng notifications.
 *
 * Thiết kế:
 *   - save()         → INSERT một thông báo mới, trả về id được sinh ra
 *   - findByUser()   → lấy N thông báo gần nhất (mặc định 50) ORDER BY created_at DESC
 *   - countUnread()  → đếm is_read=0, dùng để hiển thị badge số đỏ trên bell icon
 *   - markRead()     → đánh dấu 1 thông báo đã đọc
 *   - markAllRead()  → đánh dấu tất cả của user đã đọc (khi user mở panel)
 *   - deleteOld()    → dọn thông báo cũ hơn N ngày (gọi khi app start)
 */
public class NotificationRepository {

    private static final int DEFAULT_LIMIT = 50;

    private Connection db() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    /**
     * Lưu một thông báo mới vào DB.
     * Sau khi save, Notification.getId() sẽ có giá trị từ auto-increment.
     */
    public Notification save(Notification n) throws SQLException {
        String sql = """
                INSERT INTO notifications (user_id, auction_id, type, message, is_read, created_at)
                VALUES (?, ?, ?, ?, 0, ?)
                """;
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, n.getUserId());
            if (n.getAuctionId() != null) ps.setInt(2, n.getAuctionId());
            else                          ps.setNull(2, Types.INTEGER);
            ps.setString(3, n.getType().name());
            ps.setString(4, n.getMessage());
            ps.setString(5, DbUtil.toDbString(n.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) n.setId(rs.getInt(1));
            }
        }
        return n;
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    /**
     * Lấy tối đa {@code DEFAULT_LIMIT} thông báo gần nhất của một user,
     * mới nhất trước — để hiển thị trên notification panel.
     */
    public List<Notification> findByUser(int userId) throws SQLException {
        return findByUser(userId, DEFAULT_LIMIT);
    }

    public List<Notification> findByUser(int userId, int limit) throws SQLException {
        List<Notification> list = new ArrayList<>();
        String sql = """
                SELECT * FROM notifications
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """;
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Số thông báo chưa đọc — dùng để render badge đỏ trên bell icon.
     * Query cực nhẹ: chỉ COUNT(*) với index (user_id, is_read).
     */
    public int countUnread(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    /** Đánh dấu một thông báo đã đọc (user click vào từng item). */
    public void markRead(int notificationId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        }
    }

    /**
     * Đánh dấu TẤT CẢ thông báo của user đã đọc.
     * Gọi khi user mở notification panel — badge về 0.
     */
    public void markAllRead(int userId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    /**
     * Xoá thông báo cũ hơn {@code days} ngày.
     * Nên gọi khi app start để tránh bảng phình to theo thời gian.
     *
     * @param days số ngày giữ lại (ví dụ 30)
     */
    public void deleteOlderThan(int days) throws SQLException {
        // Dùng DbUtil.nowSql() không được vì cần arithmetic → viết riêng
        String sql = DbUtil.nowSql().contains("NOW()")
                ? "DELETE FROM notifications WHERE created_at < DATE_SUB(NOW(), INTERVAL ? DAY)"
                : "DELETE FROM notifications WHERE created_at < datetime('now', '-' || ? || ' days', 'localtime')";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, days);
            ps.executeUpdate();
        }
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId       (rs.getInt   ("id"));
        n.setUserId   (rs.getInt   ("user_id"));
        int aId = rs.getInt("auction_id");
        n.setAuctionId(rs.wasNull() ? null : aId);
        n.setType     (Notification.Type.valueOf(rs.getString("type")));
        n.setMessage  (rs.getString("message"));
        n.setRead     (rs.getInt   ("is_read") == 1);
        n.setCreatedAt(DbUtil.fromDbString(rs.getString("created_at")));
        return n;
    }
}