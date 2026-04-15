package com.nhom9.auction.baitaplon_ltnc_nhom9.repository;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.common.FilterCriteria;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.common.Page;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.*;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Truy cập dữ liệu cho AuctionItem (PhysicalItem & DigitalItem).
 */
public class ItemRepository {

    private static final Logger LOG = Logger.getLogger(ItemRepository.class.getName());

    private Connection conn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    public AuctionItem save(AuctionItem item) throws SQLException {
        String sql = """
                INSERT INTO auction_items
                  (seller_id, title, description, category, image_url, item_type,
                   starting_price, min_bid_increment, buy_now_price, current_price,
                   leading_bidder_id, status, start_time, end_time, created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
            ps.setString(13, toStr(item.getStartTime()));
            ps.setString(14, toStr(item.getEndTime()));
            ps.setString(15, toStr(item.getCreatedAt() != null ? item.getCreatedAt() : LocalDateTime.now()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) item.setId(rs.getInt(1));
            }
        }
        saveTypeExtension(item);
        return item;
    }

    private void saveTypeExtension(AuctionItem item) throws SQLException {
        if (item instanceof PhysicalItem p) {
            String sql = """
                    INSERT OR REPLACE INTO physical_items
                      (item_id, condition_text, weight_grams, dimensions, location, shipping_cost, allow_pickup)
                    VALUES (?,?,?,?,?,?,?)
                    """;
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setInt   (1, p.getId());
                ps.setString(2, p.getCondition());
                ps.setDouble(3, p.getWeightGrams());
                ps.setString(4, p.getDimensions());
                ps.setString(5, p.getLocation());
                ps.setDouble(6, p.getShippingCost() != null ? p.getShippingCost().doubleValue() : 0);
                ps.setInt   (7, p.isAllowPickup() ? 1 : 0);
                ps.executeUpdate();
            }
        } else if (item instanceof DigitalItem d) {
            String sql = """
                    INSERT OR REPLACE INTO digital_items
                      (item_id, digital_type, platform, file_size_mb, expiry_date,
                       delivery_content, replacement_guarantee)
                    VALUES (?,?,?,?,?,?,?)
                    """;
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setInt   (1, d.getId());
                ps.setString(2, d.getDigitalType());
                ps.setString(3, d.getPlatform());
                setNullableDouble(ps, 4, d.getFileSizeMB() != null ? BigDecimal.valueOf(d.getFileSizeMB()) : null);
                ps.setString(5, toStr(d.getExpiryDate()));
                ps.setString(6, d.getDeliveryContent());
                ps.setInt   (7, d.isReplacementGuarantee() ? 1 : 0);
                ps.executeUpdate();
            }
        }
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    public Optional<AuctionItem> findById(int id) throws SQLException {
        String sql = "SELECT * FROM auction_items WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapWithExtension(rs));
            }
        }
        return Optional.empty();
    }

    public List<AuctionItem> findBySellerId(int sellerId) throws SQLException {
        List<AuctionItem> list = new ArrayList<>();
        String sql = "SELECT * FROM auction_items WHERE seller_id=? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapWithExtension(rs));
            }
        }
        return list;
    }

    public List<AuctionItem> findByStatus(AuctionStatus status) throws SQLException {
        List<AuctionItem> list = new ArrayList<>();
        String sql = "SELECT * FROM auction_items WHERE status=? ORDER BY end_time ASC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapWithExtension(rs));
            }
        }
        return list;
    }

    /**
     * Lấy các phiên ACTIVE đã hết giờ (end_time <= now) – dùng bởi AuctionScheduler.
     */
    public List<AuctionItem> findExpiredActive() throws SQLException {
        List<AuctionItem> list = new ArrayList<>();
        String sql = """
                SELECT * FROM auction_items
                WHERE status='ACTIVE' AND end_time <= datetime('now','localtime')
                """;
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapWithExtension(rs));
        }
        return list;
    }

    /**
     * Lấy phiên PENDING đã đến giờ bắt đầu.
     */
    public List<AuctionItem> findDueToStart() throws SQLException {
        List<AuctionItem> list = new ArrayList<>();
        String sql = """
                SELECT * FROM auction_items
                WHERE status='PENDING' AND start_time <= datetime('now','localtime')
                """;
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapWithExtension(rs));
        }
        return list;
    }

    // ─── Search / Filter ──────────────────────────────────────────────────────

    /**
     * Tìm kiếm có phân trang theo FilterCriteria.
     */
    public Page<AuctionItem> search(FilterCriteria f, int pageNumber, int pageSize) throws SQLException {
        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (f.hasKeyword()) {
            where.append("AND (LOWER(ai.title) LIKE ? OR LOWER(ai.description) LIKE ?) ");
            String kw = "%" + f.getKeyword().toLowerCase() + "%";
            params.add(kw); params.add(kw);
        }
        if (f.hasCategory()) {
            where.append("AND ai.category = ? ");
            params.add(f.getCategory());
        }
        if (f.hasItemType()) {
            where.append("AND ai.item_type = ? ");
            params.add(f.getItemType().toUpperCase());
        }
        if (f.hasStatus()) {
            where.append("AND ai.status = ? ");
            params.add(f.getStatus().name());
        } else if (f.isActiveOnly()) {
            where.append("AND ai.status = 'ACTIVE' ");
        }
        if (f.hasPriceRange()) {
            if (f.getMinPrice() != null) { where.append("AND ai.current_price >= ? "); params.add(f.getMinPrice().doubleValue()); }
            if (f.getMaxPrice() != null) { where.append("AND ai.current_price <= ? "); params.add(f.getMaxPrice().doubleValue()); }
        }
        if (f.hasSellerId()) {
            where.append("AND ai.seller_id = ? ");
            params.add(f.getSellerId());
        }

        // Count
        String countSql = "SELECT COUNT(*) FROM auction_items ai " + where;
        int total = 0;
        try (PreparedStatement ps = conn().prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) setParam(ps, i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) total = rs.getInt(1); }
        }

        // Sort
        String orderBy = " ORDER BY ";
        if (f.hasSort()) {
            String col = switch (f.getSortBy()) {
                case "price"     -> "ai.current_price";
                case "endTime"   -> "ai.end_time";
                case "title"     -> "ai.title";
                default          -> "ai.created_at";
            };
            orderBy += col + (f.isSortAscending() ? " ASC" : " DESC");
        } else {
            orderBy += "ai.end_time ASC";
        }

        // Data
        String dataSql = "SELECT ai.* FROM auction_items ai " + where + orderBy + " LIMIT ? OFFSET ?";
        List<AuctionItem> content = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(dataSql)) {
            int idx = 1;
            for (Object p : params) setParam(ps, idx++, p);
            ps.setInt(idx++, pageSize);
            ps.setInt(idx,   pageNumber * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) content.add(mapWithExtension(rs));
            }
        }
        return new Page<>(content, pageNumber, pageSize, total);
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public void update(AuctionItem item) throws SQLException {
        String sql = """
                UPDATE auction_items SET
                  title=?, description=?, category=?, image_url=?,
                  min_bid_increment=?, buy_now_price=?, current_price=?,
                  leading_bidder_id=?, status=?, start_time=?, end_time=?
                WHERE id=?
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1,  item.getTitle());
            ps.setString(2,  item.getDescription());
            ps.setString(3,  item.getCategory());
            ps.setString(4,  item.getImageUrl());
            ps.setDouble(5,  item.getMinBidIncrement().doubleValue());
            setNullableDouble(ps, 6, item.getBuyNowPrice());
            ps.setDouble(7,  item.getCurrentPrice().doubleValue());
            setNullableInt   (ps, 8, item.getLeadingBidderId() == 0 ? null : item.getLeadingBidderId());
            ps.setString(9,  item.getStatus().name());
            ps.setString(10, toStr(item.getStartTime()));
            ps.setString(11, toStr(item.getEndTime()));
            ps.setInt   (12, item.getId());
            ps.executeUpdate();
        }
        saveTypeExtension(item);
    }

    public void updateStatus(int itemId, AuctionStatus status) throws SQLException {
        String sql = "UPDATE auction_items SET status=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt   (2, itemId);
            ps.executeUpdate();
        }
    }

    public void updateCurrentBid(int itemId, BigDecimal price, int leadingBidderId) throws SQLException {
        String sql = "UPDATE auction_items SET current_price=?, leading_bidder_id=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setDouble(1, price.doubleValue());
            ps.setInt   (2, leadingBidderId);
            ps.setInt   (3, itemId);
            ps.executeUpdate();
        }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public void deleteById(int id) throws SQLException {
        String sql = "DELETE FROM auction_items WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private AuctionItem mapWithExtension(ResultSet rs) throws SQLException {
        String type = rs.getString("item_type");
        int id = rs.getInt("id");
        AuctionItem item;

        if ("PHYSICAL".equals(type)) {
            PhysicalItem p = new PhysicalItem();
            applyBaseFields(p, rs);
            loadPhysicalExtension(p, id);
            item = p;
        } else {
            DigitalItem d = new DigitalItem();
            applyBaseFields(d, rs);
            loadDigitalExtension(d, id);
            item = d;
        }
        return item;
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
        item.setStartTime(fromStr(rs.getString("start_time")));
        item.setEndTime(fromStr(rs.getString("end_time")));
        item.setCreatedAt(fromStr(rs.getString("created_at")));
    }

    private void loadPhysicalExtension(PhysicalItem p, int id) throws SQLException {
        String sql = "SELECT * FROM physical_items WHERE item_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
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

    private void loadDigitalExtension(DigitalItem d, int id) throws SQLException {
        String sql = "SELECT * FROM digital_items WHERE item_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    d.setDigitalType(rs.getString("digital_type"));
                    d.setPlatform(rs.getString("platform"));
                    double fsz = rs.getDouble("file_size_mb");
                    d.setFileSizeMB(rs.wasNull() ? null : fsz);
                    d.setExpiryDate(fromStr(rs.getString("expiry_date")));
                    d.setDeliveryContent(rs.getString("delivery_content"));
                    d.setReplacementGuarantee(rs.getInt("replacement_guarantee") == 1);
                }
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void setNullableDouble(PreparedStatement ps, int idx, BigDecimal val) throws SQLException {
        if (val == null) ps.setNull(idx, Types.REAL);
        else             ps.setDouble(idx, val.doubleValue());
    }

    private void setNullableInt(PreparedStatement ps, int idx, Integer val) throws SQLException {
        if (val == null) ps.setNull(idx, Types.INTEGER);
        else             ps.setInt(idx, val);
    }

    private void setParam(PreparedStatement ps, int idx, Object val) throws SQLException {
        if      (val instanceof String  s) ps.setString(idx, s);
        else if (val instanceof Integer i) ps.setInt(idx, i);
        else if (val instanceof Double  d) ps.setDouble(idx, d);
        else                               ps.setObject(idx, val);
    }

    private String toStr(LocalDateTime t) { return t != null ? t.toString() : null; }

    private LocalDateTime fromStr(String s) {
        try { return s != null ? LocalDateTime.parse(s) : null; }
        catch (Exception e) { return null; }
    }
}