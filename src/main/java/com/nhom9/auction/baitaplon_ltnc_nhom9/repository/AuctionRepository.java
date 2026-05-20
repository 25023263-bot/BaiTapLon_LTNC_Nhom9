package com.nhom9.auction.baitaplon_ltnc_nhom9.repository;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.DigitalItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.PhysicalItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DatabaseConnection;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DbUtil;
import javafx.beans.value.ObservableBooleanValue;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository cho auction listings.
 * Bảng: auctions + physical_items / digital_items (table-per-subclass).
 *
 * ─── THAY ĐỔI SO VỚI PHIÊN BẢN CŨ ──────────────────────────────────────
 * 1. Connection được đóng đúng cách (try-with-resources).
 * 2. INSERT OR REPLACE → check-then-insert/update (hoạt động trên MySQL).
 * 3. datetime('now','localtime') → DbUtil.nowSql() (hoạt động trên cả hai).
 * 4. toStr/fromStr → DbUtil.toDbString/fromDbString.
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
                insertTypeExtension(conn, item);
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
                   starting_price, min_bid_increment, buy_now_price, current_price,
                   leading_bidder_id, status, start_time, end_time, created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
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
            setNullableDouble(ps, 9,  item.getBuyNowPrice());
            ps.setDouble(10, item.getCurrentPrice().doubleValue());
            setNullableInt   (ps, 11, item.getLeadingBidderId() == 0 ? null : item.getLeadingBidderId());
            ps.setString(12, item.getStatus().name());
            ps.setString(13, DbUtil.toDbString(item.getStartTime()));
            ps.setString(14, DbUtil.toDbString(item.getEndTime()));
            ps.setString(15, DbUtil.toDbString(item.getCreatedAt() != null ? item.getCreatedAt() : LocalDateTime.now()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) item.setId(rs.getInt(1));
            }
        }
    }

    /**
     * INSERT bảng phụ (physical_items / digital_items).
     * Thay INSERT OR REPLACE bằng check-then-insert/update.
     */
    private void insertTypeExtension(Connection conn, AuctionItem item) throws SQLException {
        if (item instanceof PhysicalItem p) {
            if (DbUtil.rowExists(conn, "physical_items", "auction_id", p.getId())) {
                String sql = """
                        UPDATE physical_items SET condition_text=?, weight_grams=?, dimensions=?,
                          location=?, shipping_cost=?, allow_pickup=?
                        WHERE auction_id=?
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, p.getCondition());
                    ps.setDouble(2, p.getWeightGrams());
                    ps.setString(3, p.getDimensions());
                    ps.setString(4, p.getLocation());
                    ps.setDouble(5, p.getShippingCost() != null ? p.getShippingCost().doubleValue() : 0);
                    ps.setInt   (6, p.isAllowPickup() ? 1 : 0);
                    ps.setInt   (7, p.getId());
                    ps.executeUpdate();
                }
            } else {
                String sql = """
                        INSERT INTO physical_items
                          (auction_id, condition_text, weight_grams, dimensions, location, shipping_cost, allow_pickup)
                        VALUES (?,?,?,?,?,?,?)
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt   (1, p.getId());
                    ps.setString(2, p.getCondition());
                    ps.setDouble(3, p.getWeightGrams());
                    ps.setString(4, p.getDimensions());
                    ps.setString(5, p.getLocation());
                    ps.setDouble(6, p.getShippingCost() != null ? p.getShippingCost().doubleValue() : 0);
                    ps.setInt   (7, p.isAllowPickup() ? 1 : 0);
                    ps.executeUpdate();
                }
            }

        } else if (item instanceof DigitalItem d) {
            if (DbUtil.rowExists(conn, "digital_items", "auction_id", d.getId())) {
                String sql = """
                        UPDATE digital_items SET digital_type=?, platform=?, file_size_mb=?,
                          expiry_date=?, delivery_content=?, replacement_guarantee=?
                        WHERE auction_id=?
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, d.getDigitalType());
                    ps.setString(2, d.getPlatform());
                    setNullableDouble(ps, 3, d.getFileSizeMB() != null ? BigDecimal.valueOf(d.getFileSizeMB()) : null);
                    ps.setString(4, DbUtil.toDbString(d.getExpiryDate()));
                    ps.setString(5, d.getDeliveryContent());
                    ps.setInt   (6, d.isReplacementGuarantee() ? 1 : 0);
                    ps.setInt   (7, d.getId());
                    ps.executeUpdate();
                }
            } else {
                String sql = """
                        INSERT INTO digital_items
                          (auction_id, digital_type, platform, file_size_mb, expiry_date, delivery_content, replacement_guarantee)
                        VALUES (?,?,?,?,?,?,?)
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt   (1, d.getId());
                    ps.setString(2, d.getDigitalType());
                    ps.setString(3, d.getPlatform());
                    setNullableDouble(ps, 4, d.getFileSizeMB() != null ? BigDecimal.valueOf(d.getFileSizeMB()) : null);
                    ps.setString(5, DbUtil.toDbString(d.getExpiryDate()));
                    ps.setString(6, d.getDeliveryContent());
                    ps.setInt   (7, d.isReplacementGuarantee() ? 1 : 0);
                    ps.executeUpdate();
                }
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
                if (rs.next()) return Optional.of(mapWithExtension(conn, rs));
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
            while (rs.next()) list.add(mapWithExtension(conn, rs));
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
                while (rs.next()) list.add(mapWithExtension(conn, rs));
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
                while (rs.next()) list.add(mapWithExtension(conn, rs));
            }
        }
        return list;
    }

    /**
     * ACTIVE auctions đã quá end_time – dùng trong scheduler.
     *
     * DbUtil.nowSql() trả về:
     *   → SQLite: datetime('now','localtime')
     *   → MySQL:  NOW()
     * → Câu SQL hoạt động đúng trên cả hai.
     */
    public List<AuctionItem> findExpiredActive() throws SQLException {
        List<AuctionItem> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status='ACTIVE' AND end_time <= " + DbUtil.nowSql();
        try (Connection conn = db();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapWithExtension(conn, rs));
        }
        return list;
    }

    /**
     * PENDING auctions đến giờ bắt đầu – dùng trong scheduler.
     */
    public List<AuctionItem> findDueToStart() throws SQLException {
        List<AuctionItem> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status='PENDING' AND start_time <= " + DbUtil.nowSql();
        try (Connection conn = db();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapWithExtension(conn, rs));
        }
        return list;
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public void update(AuctionItem item) throws SQLException {
        try (Connection conn = db()) {
            conn.setAutoCommit(false);
            try {
                String sql = """
                        UPDATE auctions SET
                          title=?, description=?, category=?, image_url=?,
                          min_bid_increment=?, buy_now_price=?, current_price=?,
                          leading_bidder_id=?, status=?, start_time=?, end_time=?
                        WHERE id=?
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1,  item.getTitle());
                    ps.setString(2,  item.getDescription());
                    ps.setString(3,  item.getCategory());
                    ps.setString(4,  item.getImageUrl());
                    ps.setDouble(5,  item.getMinBidIncrement().doubleValue());
                    setNullableDouble(ps, 6, item.getBuyNowPrice());
                    ps.setDouble(7,  item.getCurrentPrice().doubleValue());
                    setNullableInt   (ps, 8, item.getLeadingBidderId() == 0 ? null : item.getLeadingBidderId());
                    ps.setString(9,  item.getStatus().name());
                    ps.setString(10, DbUtil.toDbString(item.getStartTime()));
                    ps.setString(11, DbUtil.toDbString(item.getEndTime()));
                    ps.setInt   (12, item.getId());
                    ps.executeUpdate();
                }
                insertTypeExtension(conn, item);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Tự động đóng các phiên ACTIVE đã quá end_time → CLOSED.
     * Gọi khi app khởi động và mỗi khi timer phát hiện có phiên hết hạn.
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
     * Nhẹ hơn update(item) vì không cần load/ghi toàn bộ record.
     *
     * @param auctionId  ID phiên đấu giá cần gia hạn
     * @param newEndTime thời điểm kết thúc mới
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

    /** Map từ ResultSet + load bảng phụ, dùng chung connection. */
    private AuctionItem mapWithExtension(Connection conn, ResultSet rs) throws SQLException {
        String type = rs.getString("item_type");
        int id = rs.getInt("id");

        if ("PHYSICAL".equals(type)) {
            PhysicalItem p = new PhysicalItem();
            applyBaseFields(p, rs);
            loadPhysicalExtension(conn, p, id);
            return p;
        } else {
            DigitalItem d = new DigitalItem();
            applyBaseFields(d, rs);
            loadDigitalExtension(conn, d, id);
            return d;
        }
    }

    private void applyBaseFields(AuctionItem item, ResultSet rs) throws SQLException {
        item.setId(rs.getInt("id"));
        item.setSellerId(rs.getInt("seller_id"));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setCategory(rs.getString("category"));
        item.setImageUrl(rs.getString("image_url"));
        item.setStartingPrice(BigDecimal.valueOf(rs.getDouble("starting_price")));
        item.setMinBidIncrement(BigDecimal.valueOf(rs.getDouble("min_bid_increment")));
        double bnp = rs.getDouble("buy_now_price");
        item.setBuyNowPrice(rs.wasNull() ? null : BigDecimal.valueOf(bnp));
        item.setCurrentPrice(BigDecimal.valueOf(rs.getDouble("current_price")));
        int lbId = rs.getInt("leading_bidder_id");
        item.setLeadingBidderId(rs.wasNull() ? 0 : lbId);
        item.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        item.setStartTime(DbUtil.fromDbString(rs.getString("start_time")));
        item.setEndTime(DbUtil.fromDbString(rs.getString("end_time")));
        item.setCreatedAt(DbUtil.fromDbString(rs.getString("created_at")));
    }

    private void loadPhysicalExtension(Connection conn, PhysicalItem p, int auctionId) throws SQLException {
        String sql = "SELECT * FROM physical_items WHERE auction_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    p.setCondition(rs.getString("condition_text"));
                    p.setWeightGrams(rs.getDouble("weight_grams"));
                    p.setDimensions(rs.getString("dimensions"));
                    p.setLocation(rs.getString("location"));
                    p.setShippingCost(BigDecimal.valueOf(rs.getDouble("shipping_cost")));
                    p.setAllowPickup(rs.getInt("allow_pickup") == 1);
                }
            }
        }
    }

    private void loadDigitalExtension(Connection conn, DigitalItem d, int auctionId) throws SQLException {
        String sql = "SELECT * FROM digital_items WHERE auction_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    d.setDigitalType(rs.getString("digital_type"));
                    d.setPlatform(rs.getString("platform"));
                    double fsz = rs.getDouble("file_size_mb");
                    d.setFileSizeMB(rs.wasNull() ? null : fsz);
                    d.setExpiryDate(DbUtil.fromDbString(rs.getString("expiry_date")));
                    d.setDeliveryContent(rs.getString("delivery_content"));
                    d.setReplacementGuarantee(rs.getInt("replacement_guarantee") == 1);
                }
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void setNullableDouble(PreparedStatement ps, int idx, BigDecimal val) throws SQLException {
        if (val == null) ps.setNull(idx, Types.DOUBLE);
        else ps.setDouble(idx, val.doubleValue());
    }

    private void setNullableInt(PreparedStatement ps, int idx, Integer val) throws SQLException {
        if (val == null) ps.setNull(idx, Types.INTEGER);
        else ps.setInt(idx, val);
    }

    private void setParam(PreparedStatement ps, int idx, Object val) throws SQLException {
        if (val instanceof String s) ps.setString(idx, s);
        else if (val instanceof Integer i) ps.setInt(idx, i);
        else if (val instanceof Double d) ps.setDouble(idx, d);
        else ps.setObject(idx, val);
    }
}