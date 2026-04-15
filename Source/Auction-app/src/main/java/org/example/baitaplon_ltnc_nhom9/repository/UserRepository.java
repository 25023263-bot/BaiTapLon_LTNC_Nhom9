package org.example.baitaplon_ltnc_nhom9.repository;

import org.example.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import org.example.baitaplon_ltnc_nhom9.domain.model.user.*;
import org.example.baitaplon_ltnc_nhom9.DatabaseConnection;

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
 */
public class UserRepository {

    private static final Logger LOG = Logger.getLogger(UserRepository.class.getName());

    private Connection conn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    /**
     * Lưu user mới vào DB (INSERT vào users + bảng phụ tương ứng).
     * @return user với id được DB gán
     */
    public User save(User user) throws SQLException {
        String sql = """
                INSERT INTO users (username, email, password_hash, full_name, phone, role, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getRole().name());
            ps.setInt   (7, user.isActive() ? 1 : 0);
            ps.setString(8, toStr(user.getCreatedAt()));
            ps.setString(9, toStr(user.getUpdatedAt()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) user.setId(rs.getInt(1));
            }
        }
        saveRoleExtension(user);
        return user;
    }

    private void saveRoleExtension(User user) throws SQLException {
        if (user instanceof Buyer b) {
            String sql = "INSERT OR REPLACE INTO buyers (user_id, wallet_balance, total_wins) VALUES (?,?,?)";
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setInt   (1, b.getId());
                ps.setDouble(2, b.getWalletBalance().doubleValue());
                ps.setInt   (3, b.getTotalWins());
                ps.executeUpdate();
            }
        } else if (user instanceof Seller s) {
            String sql = "INSERT OR REPLACE INTO sellers (user_id, earnings_balance, total_sold, rating, rating_count) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setInt   (1, s.getId());
                ps.setDouble(2, s.getEarningsBalance().doubleValue());
                ps.setInt   (3, s.getTotalSold());
                ps.setDouble(4, s.getRating());
                ps.setInt   (5, s.getRatingCount());
                ps.executeUpdate();
            }
        } else if (user instanceof Admin a) {
            String sql = "INSERT OR REPLACE INTO admins (user_id, access_level, notes) VALUES (?,?,?)";
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setInt   (1, a.getId());
                ps.setInt   (2, a.getAccessLevel());
                ps.setString(3, a.getNotes());
                ps.executeUpdate();
            }
        }
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    public Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapWithExtension(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? COLLATE NOCASE";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapWithExtension(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ? COLLATE NOCASE";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapWithExtension(rs));
            }
        }
        return Optional.empty();
    }

    public List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapWithExtension(rs));
        }
        return list;
    }

    public List<User> findByRole(UserRole role) throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY username";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapWithExtension(rs));
            }
        }
        return list;
    }

    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ? COLLATE NOCASE";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ? COLLATE NOCASE";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public void update(User user) throws SQLException {
        String sql = """
                UPDATE users SET full_name=?, phone=?, email=?, active=?, updated_at=?
                WHERE id=?
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getEmail());
            ps.setInt   (4, user.isActive() ? 1 : 0);
            ps.setString(5, toStr(LocalDateTime.now()));
            ps.setInt   (6, user.getId());
            ps.executeUpdate();
        }
        saveRoleExtension(user);
    }

    public void updatePassword(int userId, String newHash) throws SQLException {
        String sql = "UPDATE users SET password_hash=?, updated_at=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, toStr(LocalDateTime.now()));
            ps.setInt   (3, userId);
            ps.executeUpdate();
        }
    }

    public void updateWalletBalance(int buyerId, BigDecimal balance) throws SQLException {
        String sql = "UPDATE buyers SET wallet_balance=? WHERE user_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setDouble(1, balance.doubleValue());
            ps.setInt   (2, buyerId);
            ps.executeUpdate();
        }
    }

    public void updateEarningsBalance(int sellerId, BigDecimal balance) throws SQLException {
        String sql = "UPDATE sellers SET earnings_balance=? WHERE user_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setDouble(1, balance.doubleValue());
            ps.setInt   (2, sellerId);
            ps.executeUpdate();
        }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public void deleteById(int id) throws SQLException {
        // Cascade sẽ xoá buyers/sellers/admins tương ứng
        String sql = "DELETE FROM users WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    /**
     * Map một row từ ResultSet sang đúng subclass User + load extension.
     */
    private User mapWithExtension(ResultSet rs) throws SQLException {
        UserRole role = UserRole.fromString(rs.getString("role"));
        User user;
        int id = rs.getInt("id");

        switch (role) {
            case BUYER  -> user = loadBuyer(id, rs);
            case SELLER -> user = loadSeller(id, rs);
            case ADMIN  -> user = loadAdmin(id, rs);
            default     -> throw new SQLException("Unknown role: " + role);
        }
        return user;
    }

    private Buyer loadBuyer(int id, ResultSet rs) throws SQLException {
        Buyer b = new Buyer();
        applyBaseFields(b, rs);

        String sql = "SELECT * FROM buyers WHERE user_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet r2 = ps.executeQuery()) {
                if (r2.next()) {
                    b.setWalletBalance(BigDecimal.valueOf(r2.getDouble("wallet_balance")));
                    b.setTotalWins(r2.getInt("total_wins"));
                }
            }
        }
        return b;
    }

    private Seller loadSeller(int id, ResultSet rs) throws SQLException {
        Seller s = new Seller();
        applyBaseFields(s, rs);

        String sql = "SELECT * FROM sellers WHERE user_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet r2 = ps.executeQuery()) {
                if (r2.next()) {
                    s.setEarningsBalance(BigDecimal.valueOf(r2.getDouble("earnings_balance")));
                    s.setTotalSold(r2.getInt("total_sold"));
                    s.setRating(r2.getDouble("rating"));
                    s.setRatingCount(r2.getInt("rating_count"));
                }
            }
        }
        return s;
    }

    private Admin loadAdmin(int id, ResultSet rs) throws SQLException {
        Admin a = new Admin();
        applyBaseFields(a, rs);

        String sql = "SELECT * FROM admins WHERE user_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet r2 = ps.executeQuery()) {
                if (r2.next()) {
                    a.setAccessLevel(r2.getInt("access_level"));
                    a.setNotes(r2.getString("notes"));
                }
            }
        }
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
        u.setCreatedAt(fromStr(rs.getString("created_at")));
        u.setUpdatedAt(fromStr(rs.getString("updated_at")));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String toStr(LocalDateTime t) {
        return t != null ? t.toString() : null;
    }

    private LocalDateTime fromStr(String s) {
        try { return s != null ? LocalDateTime.parse(s) : null; }
        catch (Exception e) { return null; }
    }
}
