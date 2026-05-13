package com.nhom9.auction.baitaplon_ltnc_nhom9.repository;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DatabaseConnection;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DbUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access for bids (bảng bids).
 *
 * Thay đổi so với phiên bản cũ:
 *  - Mỗi method mở Connection riêng và đóng trong try-with-resources.
 *  - Dùng DbUtil.toDbString/fromDbString thay vì toStr/fromStr local.
 */
public class BidRepository {

    private Connection db() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    public Bid save(Bid bid) throws SQLException {
        String sql = """
                INSERT INTO bids (auction_id, buyer_id, amount, bid_time, auto_bid, auto_bid_limit)
                VALUES (?,?,?,?,?,?)
                """;
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, bid.getAuctionId());
            ps.setInt   (2, bid.getBuyerId());
            ps.setDouble(3, bid.getAmount().doubleValue());
            ps.setString(4, DbUtil.toDbString(bid.getBidTime() != null ? bid.getBidTime() : LocalDateTime.now()));
            ps.setInt   (5, bid.isAutoBid() ? 1 : 0);
            if (bid.getAutoBidLimit() != null) ps.setDouble(6, bid.getAutoBidLimit().doubleValue());
            else ps.setNull(6, Types.DOUBLE);
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
                SELECT b.*, u.username AS buyer_username
                FROM bids b JOIN users u ON b.buyer_id = u.id
                WHERE b.id=?
                """;
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Bid> findAll() throws SQLException {
        List<Bid> list = new ArrayList<>();
        String sql = """
                SELECT b.*, u.username AS buyer_username
                FROM bids b JOIN users u ON b.buyer_id = u.id
                ORDER BY b.bid_time DESC
                """;
        try (Connection conn = db();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Bid> findByAuctionId(int auctionId) throws SQLException {
        List<Bid> list = new ArrayList<>();
        String sql = """
                SELECT b.*, u.username AS buyer_username
                FROM bids b JOIN users u ON b.buyer_id = u.id
                WHERE b.auction_id=?
                ORDER BY b.amount DESC, b.bid_time DESC
                """;
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public int countByAuctionId(int auctionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bids WHERE auction_id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public Optional<Bid> findLeadingBid(int auctionId) throws SQLException {
        String sql = """
                SELECT b.*, u.username AS buyer_username
                FROM bids b JOIN users u ON b.buyer_id = u.id
                WHERE b.auction_id=?
                ORDER BY b.amount DESC LIMIT 1
                """;
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Bid> findByBuyerId(int buyerId) throws SQLException {
        List<Bid> list = new ArrayList<>();
        String sql = """
                SELECT b.*, u.username AS buyer_username
                FROM bids b JOIN users u ON b.buyer_id = u.id
                WHERE b.buyer_id=?
                ORDER BY b.bid_time DESC
                """;
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Trả về tập hợp buyer_id DISTINCT đã từng bid vào một phiên.
     *
     * Dùng trong NotificationService.onNewBid() để tìm người cần notify "OUTBID":
     *   - Lấy tất cả buyer đã bid
     *   - Loại trừ người vừa bid (không tự notify mình)
     *   - Loại trừ seller (đã được notify riêng qua NEW_BID)
     *
     * Tại sao dùng Set<Integer> thay vì List?
     * → Đảm bảo mỗi buyer chỉ nhận 1 thông báo dù đã bid nhiều lần.
     */
    public java.util.Set<Integer> findDistinctBuyerIds(int auctionId) throws SQLException {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        String sql = "SELECT DISTINCT buyer_id FROM bids WHERE auction_id = ?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("buyer_id"));
            }
        }
        return ids;
    }

    public Optional<Bid> findAutoBid(int auctionId, int buyerId) throws SQLException {
        String sql = """
                SELECT b.*, u.username AS buyer_username
                FROM bids b JOIN users u ON b.buyer_id = u.id
                WHERE b.auction_id=? AND b.buyer_id=? AND b.auto_bid=1
                ORDER BY b.auto_bid_limit DESC LIMIT 1
                """;
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public void update(Bid bid) throws SQLException {
        String sql = """
                UPDATE bids SET auction_id=?, buyer_id=?, amount=?, bid_time=?, auto_bid=?, auto_bid_limit=?
                WHERE id=?
                """;
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt   (1, bid.getAuctionId());
            ps.setInt   (2, bid.getBuyerId());
            ps.setDouble(3, bid.getAmount().doubleValue());
            ps.setString(4, DbUtil.toDbString(bid.getBidTime()));
            ps.setInt   (5, bid.isAutoBid() ? 1 : 0);
            if (bid.getAutoBidLimit() != null) ps.setDouble(6, bid.getAutoBidLimit().doubleValue());
            else ps.setNull(6, Types.DOUBLE);
            ps.setInt   (7, bid.getId());
            ps.executeUpdate();
        }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public void deleteById(int id) throws SQLException {
        String sql = "DELETE FROM bids WHERE id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private Bid mapRow(ResultSet rs) throws SQLException {
        Bid bid = new Bid();
        bid.setId(rs.getInt("id"));
        bid.setAuctionId(rs.getInt("auction_id"));
        bid.setBuyerId(rs.getInt("buyer_id"));
        bid.setBuyerUsername(rs.getString("buyer_username"));
        bid.setAmount(BigDecimal.valueOf(rs.getDouble("amount")));
        bid.setBidTime(DbUtil.fromDbString(rs.getString("bid_time")));
        bid.setAutoBid(rs.getInt("auto_bid") == 1);
        double abl = rs.getDouble("auto_bid_limit");
        bid.setAutoBidLimit(rs.wasNull() ? null : BigDecimal.valueOf(abl));
        return bid;
    }
}