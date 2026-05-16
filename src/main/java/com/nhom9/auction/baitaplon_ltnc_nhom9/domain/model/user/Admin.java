package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;

import java.time.LocalDateTime;

/**
 * Quản trị viên – có quyền quản lý user và phiên đấu giá.
 */
public class Admin extends User {

    /** Cấp độ quyền hạn (1 = mặc định, có thể mở rộng sau) */
    private int accessLevel;

    // ─── Constructor ────────────────────────────────────────────────────────

    public Admin() {
        super();
        this.role        = UserRole.ADMIN;
        this.accessLevel = 1;
    }

    /** Dùng khi tạo admin mới, không cần chỉ định accessLevel (mặc định = 1) */
    public Admin(int id, String username, String email, String passwordHash,
                 String fullName, String phone) {
        super(id, username, email, passwordHash, fullName, phone, UserRole.ADMIN);
        this.accessLevel = 1;
    }

    /** Dùng khi tạo admin mới với accessLevel chỉ định — ví dụ: AuthService.register() */
    public Admin(int id, String username, String email, String passwordHash,
                 String fullName, String phone, int accessLevel) {
        this(id, username, email, passwordHash, fullName, phone);
        this.accessLevel = accessLevel;
    }

    /** Dùng khi load admin từ database — đầy đủ tất cả thông tin */
    public Admin(int id, String username, String email, String passwordHash,
                 String fullName, String phone,
                 int accessLevel, boolean active,
                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, username, email, passwordHash, fullName, phone, accessLevel);
        this.active    = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public int getAccessLevel()              { return accessLevel; }
    public void setAccessLevel(int level)    { this.accessLevel = level; }

    // ─── Abstract Implementation ─────────────────────────────────────────────

    @Override
    public String getRoleDescription() {
        return "Admin – toàn quyền quản trị hệ thống.";
    }

    @Override
    public String toString() {
        return String.format("Admin{id=%d, username='%s', accessLevel=%d}",
                id, username, accessLevel);
    }
}