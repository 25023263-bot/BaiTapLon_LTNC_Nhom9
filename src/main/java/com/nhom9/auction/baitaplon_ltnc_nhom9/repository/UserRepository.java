package com.nhom9.auction.baitaplon_ltnc_nhom9.repository;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import java.sql.*;

public class UserRepository {
    // Đăng nhập - kiểm tra username + password
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getDouble("balance")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // null = sai tài khoản
    }

    // Đăng ký - thêm user mới
    public boolean register(String fullName, String username, String email, String password) {
        String sql = "INSERT INTO users (full_name, username, email, password) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = DatabaseConnection.getConnection().prepareStatement(sql)) {
            stmt.setString(1, fullName);
            stmt.setString(2, username);
            stmt.setString(3, email);
            stmt.setString(4, password);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Lỗi DUPLICATE (username/email đã tồn tại)
            if (e.getErrorCode() == 1062) {
                System.err.println("Username hoặc email đã tồn tại!");
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }
}
