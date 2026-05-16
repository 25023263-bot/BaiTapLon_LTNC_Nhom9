package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;

import java.time.LocalDateTime;

/**
 * Quản trị viên – có quyền quản lý user và phiên đấu giá.
 */
public class Admin extends User {

    // ─── Constructor ────────────────────────────────────────────────────────

    public Admin() {
        super();
        this.role = UserRole.ADMIN;
    }

    public Admin(int id, String username, String email, String passwordHash,
                 String fullName, String phone) {
        super(id, username, email, passwordHash, fullName, phone, UserRole.ADMIN);
    }

    public Admin(int id, String username, String email, String passwordHash,
                 String fullName, String phone,
                 boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, username, email, passwordHash, fullName, phone);
        this.active    = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public String getRoleDescription() {
        return "Admin – toàn quyền quản trị hệ thống.";
    }

    @Override
    public String toString() {
        return String.format("Admin{id=%d, username='%s'}", id, username);
    }
}
