package com.nhom9.auction.baitaplon_ltnc_nhom9.repository;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.PhysicalItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DatabaseConnection;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DbUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository cho auction listings.
 * Bảng: auctions (table duy nhất — physical_items đã được xoá vì UI
 * hiện tại không thu thập các trường physical-only như condition,
 * weight, shipping...).
 *
 * ─── GHI CHÚ TRIỂN KHAI ──────────────────────────────────────────────────
 * 1. Bỏ hoàn toàn physical_items: insertTypeExtension, loadPhysicalExtension.
 * 2. Connection được đóng đúng cách (try-with-resources).
 * 3. UPSERT dùng INSERT OR REPLACE (SQLite) hoặc check-then-insert/update.
 * 4. Thời gian hiện tại dùng DbUtil.nowSql() → datetime('now','localtime').
 * 5. Timestamp dùng DbUtil.toDbString/fromDbString.
 * ──────────────────────────────────────────────────────────────────────────
 */
public class AuctionRepository {

    private Connection db() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    public AuctionItem save(AuctionItem item) throws SQLException {
        try (Connection conn = db()) {
            conn.setAutoCommit(false);
            try {
                insertAuction(conn, item);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return item;
    }

    private void insertAuction(Connection conn, AuctionItem item) throws SQLException {
        String sql = """
                INSERT INTO auctions
                  (seller_id, title, description, category, image_url, item_type,
                   starting_price, min_bid_increment, current_price,
                   leading_bidder_id, status, start_time, end_time, created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1,  item.getSellerId());
            ps.setString(2,  item.getTitle());
            ps.setString(3,  item.getDescription());
            ps.setString(4,  item.getCategory());
            ps.setString(5,  item.getImageUrl());
            ps.setString(6,  item.getItemType());
            ps.setDouble(7,  item.getStartingPrice().doubleValue());
            ps.setDouble(8,  item.getMinBidIncrement().doubleValue());
            ps.setDouble(9,  item.getCurrentPrice().doubleValue());
            setNullableInt(ps, 10, item.getLeadingBidderId() == 0 ? null : item.getLeadingBidderId());
            ps.setString(11, item.getStatus().name());
            ps.setString(12, DbUtil.toDbString(item.getStartTime()));
            ps.setString(13, DbUtil.toDbString(item.getEndTime()));
            ps.setString(14, DbUtil.toDbString(
                    item.getCreatedAt() != null ? item.getCreatedAt() : LocalDateTime.now()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) item.setId(rs.getInt(1));
            }
        }
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    public Optional<AuctionItem> findById(int id) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<AuctionItem> findAll() throws SQLException {
        List<AuctionItem> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions ORDER BY created_at DESC";
        try (Connection conn = db();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<AuctionItem> findBySellerId(int sellerId) throws SQLException {
        List<AuctionItem> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE seller_id=? ORDER BY created_at DESC";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<AuctionItem> findByStatus(AuctionStatus status) throws SQLException {
        List<AuctionItem> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status=? ORDER BY end_time ASC";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * ACTIVE auctions đã quá end_time — dùng trong scheduler.
     * DbUtil.nowSql() → datetime('now','localtime') cho SQLite.
     */
    public List<AuctionItem> findExpiredActive() throws SQLException {
        List<AuctionItem> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status='ACTIVE' AND end_time <= " + DbUtil.nowSql();
        try (Connection conn = db();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * PENDING auctions đến giờ bắt đầu — dùng trong scheduler.
     */
    public List<AuctionItem> findDueToStart() throws SQLException {
        List<AuctionItem> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status='PENDING' AND start_time <= " + DbUtil.nowSql();
        try (Connection conn = db();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public void update(AuctionItem item) throws SQLException {
        String sql = """
                UPDATE auctions SET
                  title=?, description=?, category=?, image_url=?,
                  min_bid_increment=?, current_price=?,
                  leading_bidder_id=?, status=?, start_time=?, end_time=?
                WHERE id=?
                """;
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,  item.getTitle());
            ps.setString(2,  item.getDescription());
            ps.setString(3,  item.getCategory());
            ps.setString(4,  item.getImageUrl());
            ps.setDouble(5,  item.getMinBidIncrement().doubleValue());
            ps.setDouble(6,  item.getCurrentPrice().doubleValue());
            setNullableInt(ps, 7, item.getLeadingBidderId() == 0 ? null : item.getLeadingBidderId());
            ps.setString(8,  item.getStatus().name());
            ps.setString(9,  DbUtil.toDbString(item.getStartTime()));
            ps.setString(10, DbUtil.toDbString(item.getEndTime()));
            ps.setInt   (11, item.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Tự động đóng các phiên ACTIVE đã quá end_time → CLOSED.
     */
    public void closeExpiredAuctions() throws SQLException {
        String sql = "UPDATE auctions SET status='CLOSED' WHERE status='ACTIVE' AND end_time <= " + DbUtil.nowSql();
        try (Connection conn = db();
             Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    public void updateStatus(int auctionId, AuctionStatus status) throws SQLException {
        String sql = "UPDATE auctions SET status=? WHERE id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt   (2, auctionId);
            ps.executeUpdate();
        }
    }

    public void updateCurrentBid(int auctionId, BigDecimal price, int leadingBidderId) throws SQLException {
        String sql = "UPDATE auctions SET current_price=?, leading_bidder_id=? WHERE id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, price.doubleValue());
            ps.setInt   (2, leadingBidderId);
            ps.setInt   (3, auctionId);
            ps.executeUpdate();
        }
    }

    /**
     * Cập nhật chỉ cột end_time — dùng riêng cho anti-snipe extension.
     */
    public void updateEndTime(int auctionId, LocalDateTime newEndTime) throws SQLException {
        String sql = "UPDATE auctions SET end_time=? WHERE id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DbUtil.toDbString(newEndTime));
            ps.setInt   (2, auctionId);
            ps.executeUpdate();
        }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public void deleteById(int id) throws SQLException {
        String sql = "DELETE FROM auctions WHERE id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private AuctionItem mapRow(ResultSet rs) throws SQLException {
        PhysicalItem item = new PhysicalItem();
        item.setId(rs.getInt("id"));
        item.setSellerId(rs.getInt("seller_id"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setCategory(rs.getString("category"));
        item.setImageUrl(rs.getString("image_url"));
        item.setStartingPrice(BigDecimal.valueOf(rs.getDouble("starting_price")));
        item.setMinBidIncrement(BigDecimal.valueOf(rs.getDouble("min_bid_increment")));
        item.setCurrentPrice(BigDecimal.valueOf(rs.getDouble("current_price")));
        int lbId = rs.getInt("leading_bidder_id");
        item.setLeadingBidderId(rs.wasNull() ? 0 : lbId);
        item.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        item.setStartTime(DbUtil.fromDbString(rs.getString("start_time")));
        item.setEndTime(DbUtil.fromDbString(rs.getString("end_time")));
        item.setCreatedAt(DbUtil.fromDbString(rs.getString("created_at")));
        return item;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void setNullableInt(PreparedStatement ps, int idx, Integer val) throws SQLException {
        if (val == null) ps.setNull(idx, Types.INTEGER);
        else ps.setInt(idx, val);
    }
}
