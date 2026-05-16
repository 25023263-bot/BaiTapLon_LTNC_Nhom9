package com.nhom9.auction.baitaplon_ltnc_nhom9.repository;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Transaction;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.PaymentStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DatabaseConnection;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DbUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Truy cập dữ liệu cho Transaction.
 *
 * Cấu trúc bảng transactions tối giản:
 *   id, auction_id, buyer_id, seller_id, amount,
 *   payment_method, payment_status, created_at, completed_at
 */
public class TransactionRepository {

    private Connection db() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    public Transaction save(Transaction t) throws SQLException {
        String sql = """
                INSERT INTO transactions
                  (auction_id, buyer_id, seller_id, amount,
                   payment_method, payment_status, created_at, completed_at)
                VALUES (?,?,?,?,?,?,?,?)
                """;
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, t.getAuctionId());
            ps.setInt   (2, t.getBuyerId());
            ps.setInt   (3, t.getSellerId());
            ps.setDouble(4, t.getAmount().doubleValue());
            ps.setString(5, t.getPaymentMethod());
            ps.setString(6, t.getPaymentStatus().name());
            ps.setString(7, DbUtil.toDbString(t.getCreatedAt()));
            ps.setString(8, DbUtil.toDbString(t.getCompletedAt()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) t.setId(rs.getInt(1));
            }
        }
        return t;
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

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

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private Transaction mapRow(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getInt("id"),
                rs.getInt("auction_id"),
                rs.getInt("buyer_id"),
                rs.getInt("seller_id"),
                BigDecimal.valueOf(rs.getDouble("amount")),
                rs.getString("payment_method"),
                PaymentStatus.valueOf(rs.getString("payment_status")),
                DbUtil.fromDbString(rs.getString("created_at")),
                DbUtil.fromDbString(rs.getString("completed_at"))
        );
    }
}