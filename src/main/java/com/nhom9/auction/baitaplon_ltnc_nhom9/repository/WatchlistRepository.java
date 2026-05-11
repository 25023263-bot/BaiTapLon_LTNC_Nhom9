package com.nhom9.auction.baitaplon_ltnc_nhom9.repository;

import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Watchlist: buyers theo dõi các phiên đấu giá.
 *
 * Thay đổi so với phiên bản cũ:
 *  - Mỗi method mở Connection riêng và đóng trong try-with-resources.
 *  - INSERT OR IGNORE → dùng try/catch hoặc kiểm tra trước (hoạt động trên MySQL).
 *
 * Về INSERT OR IGNORE trong watchlist:
 *  Bảng watchlist có UNIQUE(buyer_id, auction_id), nên nếu insert trùng sẽ lỗi.
 *  Thay vì dùng INSERT OR IGNORE (SQLite only), ta dùng try/catch:
 *  - Nếu thành công → OK
 *  - Nếu lỗi constraint (trùng) → bỏ qua (vì đây là hành vi mong muốn)
 */
public class WatchlistRepository {

    private Connection db() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Thêm vào watchlist.
     * Nếu đã theo dõi rồi → không làm gì (bỏ qua lỗi unique constraint).
     *
     * Tại sao không dùng INSERT OR IGNORE?
     * → INSERT OR IGNORE là cú pháp SQLite. MySQL dùng INSERT IGNORE.
     * → Cách portable nhất: kiểm tra trước rồi mới INSERT.
     */
    public void add(int buyerId, int auctionId) throws SQLException {
        if (isWatching(buyerId, auctionId)) return; // Đã theo dõi → bỏ qua
        String sql = "INSERT INTO watchlist (buyer_id, auction_id) VALUES (?,?)";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            ps.setInt(2, auctionId);
            ps.executeUpdate();
        }
    }

    public void remove(int buyerId, int auctionId) throws SQLException {
        String sql = "DELETE FROM watchlist WHERE buyer_id=? AND auction_id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            ps.setInt(2, auctionId);
            ps.executeUpdate();
        }
    }

    public boolean isWatching(int buyerId, int auctionId) throws SQLException {
        String sql = "SELECT 1 FROM watchlist WHERE buyer_id=? AND auction_id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            ps.setInt(2, auctionId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /** Auction IDs mà một buyer đang theo dõi. */
    public List<Integer> findAuctionIdsByBuyer(int buyerId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT auction_id FROM watchlist WHERE buyer_id=? ORDER BY added_at DESC";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("auction_id"));
            }
        }
        return ids;
    }

    /** Buyer IDs đang theo dõi một auction (dùng để gửi notification). */
    public List<Integer> findBuyerIdsByAuctionId(int auctionId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT buyer_id FROM watchlist WHERE auction_id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("buyer_id"));
            }
        }
        return ids;
    }

    public int countWatchers(int auctionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM watchlist WHERE auction_id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void clearByBuyer(int buyerId) throws SQLException {
        String sql = "DELETE FROM watchlist WHERE buyer_id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, buyerId);
            ps.executeUpdate();
        }
    }
}