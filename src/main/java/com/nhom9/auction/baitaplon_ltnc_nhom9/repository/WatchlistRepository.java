package com.nhom9.auction.baitaplon_ltnc_nhom9.repository;

import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy cập dữ liệu cho Watchlist (danh sách theo dõi của Buyer).
 */
public class WatchlistRepository {

    private Connection conn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Thêm vật phẩm vào watchlist. Bỏ qua nếu đã có (UNIQUE constraint).
     */
    public void add(int buyerId, int itemId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO watchlist (buyer_id, item_id) VALUES (?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            ps.setInt(2, itemId);
            ps.executeUpdate();
        }
    }

    /**
     * Xoá vật phẩm khỏi watchlist.
     */
    public void remove(int buyerId, int itemId) throws SQLException {
        String sql = "DELETE FROM watchlist WHERE buyer_id=? AND item_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            ps.setInt(2, itemId);
            ps.executeUpdate();
        }
    }

    /**
     * Kiểm tra vật phẩm có trong watchlist không.
     */
    public boolean isWatching(int buyerId, int itemId) throws SQLException {
        String sql = "SELECT 1 FROM watchlist WHERE buyer_id=? AND item_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            ps.setInt(2, itemId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /**
     * Lấy danh sách item_id trong watchlist của buyer.
     */
    public List<Integer> findItemIdsByBuyer(int buyerId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT item_id FROM watchlist WHERE buyer_id=? ORDER BY added_at DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("item_id"));
            }
        }
        return ids;
    }

    /**
     * Số lượng người theo dõi một vật phẩm.
     */
    public int countWatchers(int itemId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM watchlist WHERE item_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Xoá toàn bộ watchlist của buyer (dùng khi xoá tài khoản).
     */
    public void clearByBuyer(int buyerId) throws SQLException {
        String sql = "DELETE FROM watchlist WHERE buyer_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            ps.executeUpdate();
        }
    }
}