package com.nhom9.auction.baitaplon_ltnc_nhom9.repository;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.*;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DatabaseConnection;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DbUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Truy cập dữ liệu cho User, Buyer, Seller, Admin.
 * Dùng table-per-subclass: users + (buyers | sellers | admins).
 *
 * ─── THAY ĐỔI SO VỚI PHIÊN BẢN CŨ ──────────────────────────────────────
 * 1. Mỗi method tự mượn Connection từ pool và đóng trong try-with-resources.
 *    → Trước: conn() trả về singleton → không bao giờ được đóng.
 *    → Sau: try (Connection conn = db().getConnection()) { ... }
 *           conn.close() tự gọi khi ra khỏi block → trả về pool.
 *
 * 2. INSERT OR REPLACE → check-then-insert/update (hoạt động trên cả hai DB).
 *    → INSERT OR REPLACE là cú pháp riêng của SQLite.
 *    → MySQL dùng ON DUPLICATE KEY UPDATE hoặc REPLACE INTO (khác ngữ nghĩa).
 *    → Giải pháp portable nhất: kiểm tra tồn tại rồi INSERT hoặc UPDATE.
 *
 * 3. toStr/fromStr → DbUtil.toDbString/fromDbString.
 *    → Dùng format "yyyy-MM-dd HH:mm:ss" nhất quán trên cả hai DB.
 * ──────────────────────────────────────────────────────────────────────────
 */
public class UserRepository {

    private static final Logger LOG = Logger.getLogger(UserRepository.class.getName());

    /** Lấy connection từ pool – phải dùng trong try-with-resources! */
    private Connection db() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    /**
     * Lưu user mới vào DB.
     * INSERT vào bảng users + bảng phụ (buyers / sellers / admins).
     *
     * Cả hai INSERT chạy trong cùng 1 transaction để đảm bảo nhất quán:
     * nếu INSERT bảng phụ lỗi, user cũng bị rollback.
     *
     * @return user với id được DB gán
     */
    public User save(User user) throws SQLException {
        try (Connection conn = db()) {
            conn.setAutoCommit(false);
            try {
                insertUser(conn, user);
                insertRoleExtension(conn, user);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return user;
    }

    /** INSERT vào bảng users và lấy ID được DB tạo ra. */
    private void insertUser(Connection conn, User user) throws SQLException {
        String sql = """
                INSERT INTO users (username, email, password_hash, full_name, phone, role, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getRole().name());
            ps.setInt   (7, user.isActive() ? 1 : 0);
            ps.setString(8, DbUtil.toDbString(user.getCreatedAt()));
            ps.setString(9, DbUtil.toDbString(user.getUpdatedAt()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) user.setId(rs.getInt(1));
            }
        }
    }

    /**
     * INSERT vào bảng phụ tương ứng với role của user.
     *
     * Tại sao không dùng INSERT OR REPLACE?
     * → "INSERT OR REPLACE" là cú pháp riêng SQLite (không có trong MySQL).
     * → Thay bằng: kiểm tra row đã tồn tại chưa, rồi INSERT hoặc UPDATE.
     * → Hoạt động giống nhau trên cả SQLite lẫn MySQL.
     */
    private void insertRoleExtension(Connection conn, User user) throws SQLException {
        if (user instanceof Buyer b) {
            if (DbUtil.rowExists(conn, "buyers", "user_id", b.getId())) {
                String sql = "UPDATE buyers SET wallet_balance=? WHERE user_id=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setDouble(1, b.getWalletBalance().doubleValue());
                    ps.setInt   (2, b.getId());
                    ps.executeUpdate();
                }
            } else {
                String sql = "INSERT INTO buyers (user_id, wallet_balance) VALUES (?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt   (1, b.getId());
                    ps.setDouble(2, b.getWalletBalance().doubleValue());
                    ps.executeUpdate();
                }
            }

        } else if (user instanceof Seller s) {
            if (DbUtil.rowExists(conn, "sellers", "user_id", s.getId())) {
                String sql = "UPDATE sellers SET earnings_balance=? WHERE user_id=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setDouble(1, s.getEarningsBalance().doubleValue());
                    ps.setInt   (2, s.getId());
                    ps.executeUpdate();
                }
            } else {
                String sql = "INSERT INTO sellers (user_id, earnings_balance) VALUES (?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt   (1, s.getId());
                    ps.setDouble(2, s.getEarningsBalance().doubleValue());
                    ps.executeUpdate();
                }
            }

        } else if (user instanceof Admin a) {
            if (!DbUtil.rowExists(conn, "admins", "user_id", a.getId())) {
                String sql = "INSERT INTO admins (user_id) VALUES (?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, a.getId());
                    ps.executeUpdate();
                }
            }
            // Admin không có trường đặc thù cần update
        }
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    public Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapWithExtension(conn, rs));
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapWithExtension(conn, rs));
            }
        }
        return Optional.empty();
    }

    public List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (Connection conn = db();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapWithExtension(conn, rs));
        }
        return list;
    }

    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    /**
     * Cập nhật user (thông tin cơ bản + bảng phụ).
     * Tương tự save(), dùng transaction để đảm bảo nhất quán.
     */
    public void update(User user) throws SQLException {
        try (Connection conn = db()) {
            conn.setAutoCommit(false);
            try {
                String sql = """
                        UPDATE users SET full_name=?, phone=?, email=?, active=?, updated_at=?
                        WHERE id=?
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, user.getFullName());
                    ps.setString(2, user.getPhone());
                    ps.setString(3, user.getEmail());
                    ps.setInt   (4, user.isActive() ? 1 : 0);
                    ps.setString(5, DbUtil.toDbString(LocalDateTime.now()));
                    ps.setInt   (6, user.getId());
                    ps.executeUpdate();
                }
                insertRoleExtension(conn, user);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void updatePassword(int userId, String newHash) throws SQLException {
        String sql = "UPDATE users SET password_hash=?, updated_at=? WHERE id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, DbUtil.toDbString(LocalDateTime.now()));
            ps.setInt   (3, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Nâng cấp một Buyer thành Seller trong DB.
     *
     * Thực hiện 2 bước trong cùng một transaction:
     *   1. UPDATE bảng users: đổi cột role từ 'BUYER' → 'SELLER'
     *   2. INSERT vào bảng sellers: tạo row với earnings_balance = 0
     *
     * @param userId ID của Buyer cần nâng cấp
     */
    public void upgradeToSeller(int userId) throws SQLException {
        try (Connection conn = db()) {
            conn.setAutoCommit(false);
            try {
                String updateRole = "UPDATE users SET role=?, updated_at=? WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(updateRole)) {
                    ps.setString(1, UserRole.SELLER.name());
                    ps.setString(2, DbUtil.toDbString(LocalDateTime.now()));
                    ps.setInt   (3, userId);
                    ps.executeUpdate();
                }

                if (!DbUtil.rowExists(conn, "sellers", "user_id", userId)) {
                    String insertSeller = "INSERT INTO sellers (user_id, earnings_balance) VALUES (?, 0)";
                    try (PreparedStatement ps = conn.prepareStatement(insertSeller)) {
                        ps.setInt(1, userId);
                        ps.executeUpdate();
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void updateWalletBalance(int buyerId, BigDecimal balance) throws SQLException {
        String sql = "UPDATE buyers SET wallet_balance=? WHERE user_id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, balance.doubleValue());
            ps.setInt   (2, buyerId);
            ps.executeUpdate();
        }
    }

    public void updateEarningsBalance(int sellerId, BigDecimal balance) throws SQLException {
        String sql = "UPDATE sellers SET earnings_balance=? WHERE user_id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, balance.doubleValue());
            ps.setInt   (2, sellerId);
            ps.executeUpdate();
        }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public void deleteById(int id) throws SQLException {
        // ON DELETE CASCADE trong schema sẽ xoá buyers/sellers/admins tương ứng
        String sql = "DELETE FROM users WHERE id=?";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    /**
     * Map một row từ ResultSet sang đúng subclass User.
     *
     * Nhận conn làm tham số để load bảng phụ trong CÙNG connection.
     * Điều này quan trọng khi dùng transaction (cần cùng connection để thấy uncommitted data).
     */
    private User mapWithExtension(Connection conn, ResultSet rs) throws SQLException {
        UserRole role = UserRole.fromString(rs.getString("role"));
        int id = rs.getInt("id");
        return switch (role) {
            case BUYER  -> loadBuyer (conn, id, rs);
            case SELLER -> loadSeller(conn, id, rs);
            case ADMIN  -> loadAdmin (conn, id, rs);
            default     -> throw new SQLException("Unknown role: " + role);
        };
    }

    private Buyer loadBuyer(Connection conn, int id, ResultSet rs) throws SQLException {
        Buyer b = new Buyer();
        applyBaseFields(b, rs);
        String sql = "SELECT * FROM buyers WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet r2 = ps.executeQuery()) {
                if (r2.next()) {
                    b.setWalletBalance(BigDecimal.valueOf(r2.getDouble("wallet_balance")));
                }
            }
        }
        return b;
    }

    private Seller loadSeller(Connection conn, int id, ResultSet rs) throws SQLException {
        Seller s = new Seller();
        applyBaseFields(s, rs);
        String sql = "SELECT * FROM sellers WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet r2 = ps.executeQuery()) {
                if (r2.next()) {
                    s.setEarningsBalance(BigDecimal.valueOf(r2.getDouble("earnings_balance")));
                }
            }
        }
        return s;
    }

    private Admin loadAdmin(Connection conn, int id, ResultSet rs) throws SQLException {
        Admin a = new Admin();
        applyBaseFields(a, rs);
        // Admin không có bảng phụ chứa dữ liệu cần load ở giai đoạn này
        return a;
    }

    private void applyBaseFields(User u, ResultSet rs) throws SQLException {
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setPhone(rs.getString("phone"));
        u.setRole(UserRole.fromString(rs.getString("role")));
        u.setActive(rs.getInt("active") == 1);
        u.setCreatedAt(DbUtil.fromDbString(rs.getString("created_at")));
        u.setUpdatedAt(DbUtil.fromDbString(rs.getString("updated_at")));
    }
}
