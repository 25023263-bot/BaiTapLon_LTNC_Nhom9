package com.nhom9.auction.baitaplon_ltnc_nhom9.repository;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) {
            System.err.println("Database connection is unavailable.");
            return null;
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("username"),
                        rs.getDouble("balance")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean register(String fullName, String username, String password) {
        String sql = "INSERT INTO users (full_name, username, password) VALUES (?, ?, ?)";
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) {
            System.err.println("Database connection is unavailable.");
            return false;
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, fullName);
            stmt.setString(2, username);
            stmt.setString(3, password);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.err.println("Username already exists.");
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }
}
