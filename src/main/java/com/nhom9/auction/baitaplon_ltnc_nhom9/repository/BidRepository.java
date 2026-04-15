package com.nhom9.auction.baitaplon_ltnc_nhom9.repository;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Truy cập dữ liệu cho Bid.
 */
public class BidRepository {

    private Connection conn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    public Bid save(Bid bid) throws SQLException {
        String sql = """
                INSERT INTO bids (item_id, bidder_id, amount, bid_time, auto_bid, auto_bid_limit)
                VALUES (?,?,?,?,?,?)
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, bid.getItemId());
            ps.setInt   (2, bid.getBidderId());
            ps.setDouble(3, bid.getAmount().doubleValue());
            ps.setString(4, toStr(bid.getBidTime() != null ? bid.getBidTime() : LocalDateTime.now()));
            ps.setInt   (5, bid.isAutoBid() ? 1 : 0);
            if (bid.getAutoBidLimit() != null) ps.setDouble(6, bid.getAutoBidLimit().doubleValue());
            else                               ps.setNull  (6, Types.REAL);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) bid.setId(rs.getInt(1));
            }
        }
        return bid;
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    public Optional<Bid> findById(int id) throws SQLException {
        String sql = """
                SELECT b.*, u.username AS bidder_username
                FROM bids b JOIN users u ON b.bidder_id = u.id
                WHERE b.id=?
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Lấy toàn bộ lịch sử bid của một vật phẩm, mới nhất lên đầu.
     */
    public List<Bid> findByItemId(int itemId) throws SQLException {
        List<Bid> list = new ArrayList<>();
        String sql = """
                SELECT b.*, u.username AS bidder_username
                FROM bids b JOIN users u ON b.bidder_id = u.id
                WHERE b.item_id=?
                ORDER BY b.amount DESC, b.bid_time DESC
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Tổng số bid của một vật phẩm.
     */
    public int countByItemId(int itemId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bids WHERE item_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Bid cao nhất của một phiên (leading bid).
     */
    public Optional<Bid> findLeadingBid(int itemId) throws SQLException {
        String sql = """
                SELECT b.*, u.username AS bidder_username
                FROM bids b JOIN users u ON b.bidder_id = u.id
                WHERE b.item_id=?
                ORDER BY b.amount DESC LIMIT 1
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Lịch sử bid của một buyer (dùng ở màn hình "Bid của tôi").
     */
    public List<Bid> findByBidderId(int bidderId) throws SQLException {
        List<Bid> list = new ArrayList<>();
        String sql = """
                SELECT b.*, u.username AS bidder_username
                FROM bids b JOIN users u ON b.bidder_id = u.id
                WHERE b.bidder_id=?
                ORDER BY b.bid_time DESC
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, bidderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Auto-bid cao nhất của một buyer trên một item.
     */
    public Optional<Bid> findAutoBid(int itemId, int bidderId) throws SQLException {
        String sql = """
                SELECT b.*, u.username AS bidder_username
                FROM bids b JOIN users u ON b.bidder_id = u.id
                WHERE b.item_id=? AND b.bidder_id=? AND b.auto_bid=1
                ORDER BY b.auto_bid_limit DESC LIMIT 1
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, itemId);
            ps.setInt(2, bidderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private Bid mapRow(ResultSet rs) throws SQLException {
        Bid bid = new Bid();
        bid.setId(rs.getInt("id"));
        bid.setItemId(rs.getInt("item_id"));
        bid.setBidderId(rs.getInt("bidder_id"));
        bid.setBidderUsername(rs.getString("bidder_username"));
        bid.setAmount(BigDecimal.valueOf(rs.getDouble("amount")));
        bid.setBidTime(fromStr(rs.getString("bid_time")));
        bid.setAutoBid(rs.getInt("auto_bid") == 1);
        double abl = rs.getDouble("auto_bid_limit");
        bid.setAutoBidLimit(rs.wasNull() ? null : BigDecimal.valueOf(abl));
        return bid;
    }

    private String toStr(LocalDateTime t)  { return t != null ? t.toString() : null; }
    private LocalDateTime fromStr(String s) {
        try { return s != null ? LocalDateTime.parse(s) : null; }
        catch (Exception e) { return null; }
    }
}