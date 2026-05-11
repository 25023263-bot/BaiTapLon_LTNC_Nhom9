package com.nhom9.auction.baitaplon_ltnc_nhom9.repository;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Transaction;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.PaymentStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DatabaseConnection;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DbUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Truy cập dữ liệu cho Transaction.
 *
 * Thay đổi so với phiên bản cũ:
 *  - Mỗi method mở Connection riêng và đóng trong try-with-resources.
 *  - Dùng DbUtil.toDbString/fromDbString.
 */
public class TransactionRepository {

    private Connection db() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    public Transaction save(Transaction t) throws SQLException {
        String sql = """
                INSERT INTO transactions
                  (auction_id, buyer_id, seller_id, amount, shipping_fee, platform_fee,
                   total_paid, seller_receives, payment_status, payment_method,
                   external_ref, created_at, completed_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1,  t.getAuctionId());
            ps.setInt   (2,  t.getBuyerId());
            ps.setInt   (3,  t.getSellerId());
            ps.setDouble(4,  t.getAmount().doubleValue());
            ps.setDouble(5,  t.getShippingFee().doubleValue());
            ps.setDouble(6,  t.getPlatformFee().doubleValue());
            ps.setDouble(7,  t.getTotalPaid().doubleValue());
            ps.setDouble(8,  t.getSellerReceives().doubleValue());
            ps.setString(9,  t.getPaymentStatus().name());
            ps.setString(10, t.getPaymentMethod());
            ps.setString(11, t.getExternalRef());
            ps.setString(12, DbUtil.toDbString(t.getCreatedAt()));
            ps.setString(13, DbUtil.toDbString(t.getCompletedAt()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) t.setId(rs.getInt(1));
            }
        }
        return t;
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    public Optional<Transaction> findById(int id) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Transaction> findByAuctionId(int auctionId) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE auction_id=? LIMIT 1";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Transaction> findByBuyerId(int buyerId) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE buyer_id=? ORDER BY created_at DESC";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Transaction> findBySellerId(int sellerId) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE seller_id=? ORDER BY created_at DESC";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Transaction> findAll() throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";
        try (Connection conn = db();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public void updateStatus(int id, PaymentStatus status, LocalDateTime completedAt) throws SQLException {
        String sql = "UPDATE transactions SET payment_status=?, completed_at=? WHERE id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, DbUtil.toDbString(completedAt));
            ps.setInt   (3, id);
            ps.executeUpdate();
        }
    }

    // ─── Stats ────────────────────────────────────────────────────────────────

    /** Tổng doanh thu nền tảng từ platform_fee. */
    public BigDecimal totalPlatformRevenue() throws SQLException {
        String sql = "SELECT SUM(platform_fee) FROM transactions WHERE payment_status='COMPLETED'";
        try (Connection conn = db();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return BigDecimal.valueOf(rs.getDouble(1));
        }
        return BigDecimal.ZERO;
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private Transaction mapRow(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getInt("id"),
                rs.getInt("auction_id"),
                rs.getInt("buyer_id"),
                rs.getInt("seller_id"),
                BigDecimal.valueOf(rs.getDouble("amount")),
                BigDecimal.valueOf(rs.getDouble("shipping_fee")),
                BigDecimal.valueOf(rs.getDouble("platform_fee")),
                BigDecimal.valueOf(rs.getDouble("total_paid")),
                BigDecimal.valueOf(rs.getDouble("seller_receives")),
                PaymentStatus.valueOf(rs.getString("payment_status")),
                rs.getString("payment_method"),
                rs.getString("external_ref"),
                DbUtil.fromDbString(rs.getString("created_at")),
                DbUtil.fromDbString(rs.getString("completed_at"))
        );
    }
}