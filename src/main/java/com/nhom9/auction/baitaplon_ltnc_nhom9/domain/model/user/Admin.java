package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;

import java.time.LocalDateTime;

/**
 * Quản trị viên – có thể xoá phiên đấu giá và vô hiệu hoá tài khoản người dùng.
 *
 * Admin không có dữ liệu đặc thù ngoài thông tin User cơ bản.
 * Quyền hạn được xác định bằng role = ADMIN trong bảng users.
 */
public class Admin extends User {

    // ─── Constructor ────────────────────────────────────────────────────────

    public Admin() {
        super();
        this.role = UserRole.ADMIN;
    }

    /** Dùng khi tạo Admin mới (ví dụ: AuthService.register) */
    public Admin(int id, String username, String email, String passwordHash,
                 String fullName, String phone) {
        super(id, username, email, passwordHash, fullName, phone, UserRole.ADMIN);
    }

    /** Dùng khi load Admin từ database */
    public Admin(int id, String username, String email, String passwordHash,
                 String fullName, String phone,
                 boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, username, email, passwordHash, fullName, phone);
        this.active    = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("Admin{id=%d, username='%s'}", id, username);
    }
}
