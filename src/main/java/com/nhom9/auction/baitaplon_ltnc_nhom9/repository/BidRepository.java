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
 *
 * [AUTO-BID] Thêm 2 method mới để hỗ trợ Proxy Bidding:
 *  - findTopAutoBidByBuyer()      : tìm auto-bid có limit cao nhất của 1 người trong 1 phiên
 *  - findActiveAutoBidsExcluding(): tìm tất cả auto-bid của người khác (trừ người đang dẫn đầu)
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

    // ─── [MỚI] Auto-bid Proxy Bidding ─────────────────────────────────────────

    /**
     * Tìm auto-bid có limit cao nhất của một buyer trong một phiên.
     *
     * Mục đích: Biết "giới hạn tối đa" thực sự mà người này sẵn sàng trả,
     * dù họ đã đặt nhiều lần auto-bid (mỗi lần đặt tạo ra 1 row mới trong DB).
     *
     * Ví dụ: A đặt auto-bid 3 lần → 3 rows trong bảng bids.
     * Method này chỉ lấy row có auto_bid_limit cao nhất → đó là "limit hiện tại" của A.
     *
     * @param auctionId phiên đấu giá cần tra
     * @param buyerId   người cần tra
     * @return Optional chứa Bid nếu người đó có auto-bid, rỗng nếu không
     */
    public Optional<Bid> findTopAutoBidByBuyer(int auctionId, int buyerId) throws SQLException {
        String sql = """
                SELECT b.*, u.username AS buyer_username
                FROM bids b JOIN users u ON b.buyer_id = u.id
                WHERE b.auction_id = ? AND b.buyer_id = ? AND b.auto_bid = 1
                ORDER BY b.auto_bid_limit DESC
                LIMIT 1
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

    /**
     * Tìm tất cả auto-bid đang "hiệu lực" của những người KHÁC trong một phiên,
     * sắp xếp theo limit giảm dần (người có limit cao nhất đứng đầu).
     *
     * "Hiệu lực" = auto_bid = 1 và là auto_bid_limit cao nhất của người đó.
     * Tại sao cần GROUP BY buyer_id?
     * → Một người có thể đặt auto-bid nhiều lần (mỗi lần counter tạo 1 row).
     *   GROUP BY đảm bảo mỗi người chỉ xuất hiện 1 lần, đại diện bởi limit cao nhất.
     *
     * Tại sao dùng subquery thay vì MAX() đơn giản?
     * → Vì cần lấy toàn bộ thông tin của Bid (username, bidTime...), không chỉ limit.
     *   Subquery tìm đúng row có limit cao nhất, rồi lấy dữ liệu từ row đó.
     *
     * @param auctionId      phiên đấu giá cần tra
     * @param excludeBuyerId buyer đang dẫn đầu — không cần counter chính mình
     * @return danh sách Bid, sắp xếp theo auto_bid_limit giảm dần
     */
    public List<Bid> findActiveAutoBidsExcluding(int auctionId, int excludeBuyerId) throws SQLException {
        List<Bid> list = new ArrayList<>();
        // Giải thích SQL:
        //   - Outer query: lấy row của mỗi buyer có auto_bid_limit bằng MAX của chính họ
        //   - Subquery:    tìm auto_bid_limit cao nhất của từng buyer trong phiên này
        //   - GROUP BY b.buyer_id: đảm bảo mỗi buyer chỉ xuất hiện 1 lần
        //   - ORDER BY b.auto_bid_limit DESC: người có giới hạn cao nhất đứng đầu
        String sql = """
                SELECT b.*, u.username AS buyer_username
                FROM bids b JOIN users u ON b.buyer_id = u.id
                WHERE b.auction_id = ?
                  AND b.buyer_id != ?
                  AND b.auto_bid = 1
                  AND b.auto_bid_limit = (
                      SELECT MAX(b2.auto_bid_limit)
                      FROM bids b2
                      WHERE b2.auction_id = b.auction_id
                        AND b2.buyer_id   = b.buyer_id
                        AND b2.auto_bid   = 1
                  )
                GROUP BY b.buyer_id
                ORDER BY b.auto_bid_limit DESC
                """;
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, excludeBuyerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
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