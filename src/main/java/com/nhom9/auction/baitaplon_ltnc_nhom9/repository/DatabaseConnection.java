package com.nhom9.auction.baitaplon_ltnc_nhom9.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL      = "jdbc:mysql://localhost:3306/auction_db";
    private static final String USER     = "root";
    private static final String PASSWORD = "123Mih1234_"; // ← đổi thành mật khẩu MySQL của bạn

    private static Connection connection = null;

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối DB: " + e.getMessage());
        }
        return connection;
    }
}
