package org.example.baitaplon_ltnc_nhom9.domain.model.user;

import org.example.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;

import java.time.LocalDateTime;

/**
 * Quản trị viên – có quyền quản lý user và phiên đấu giá.
 */
public class Admin extends User {

    /** Cấp độ quyền: 1 = moderator, 2 = super admin */
    private int accessLevel;

    /** Ghi chú nội bộ về quyền đặc biệt */
    private String notes;

    // ─── Constructor ────────────────────────────────────────────────────────

    public Admin() {
        super();
        this.role        = UserRole.ADMIN;
        this.accessLevel = 1;
    }

    public Admin(int id, String username, String email, String passwordHash,
                 String fullName, String phone, int accessLevel) {
        super(id, username, email, passwordHash, fullName, phone, UserRole.ADMIN);
        this.accessLevel = accessLevel;
    }

    public Admin(int id, String username, String email, String passwordHash,
                 String fullName, String phone, int accessLevel, String notes,
                 boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, username, email, passwordHash, fullName, phone, accessLevel);
        this.notes     = notes;
        this.active    = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ─── Business Logic ──────────────────────────────────────────────────────

    /**
     * Kiểm tra xem admin có phải super admin không.
     */
    public boolean isSuperAdmin() {
        return accessLevel >= 2;
    }

    /**
     * Kiểm tra có quyền thực hiện hành động ở cấp yêu cầu không.
     */
    public boolean hasPermission(int requiredLevel) {
        return this.accessLevel >= requiredLevel;
    }

    @Override
    public String getRoleDescription() {
        return isSuperAdmin()
                ? "Super Admin – toàn quyền quản trị hệ thống."
                : "Moderator – quản lý user và phiên đấu giá.";
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public int getAccessLevel()                 { return accessLevel; }
    public void setAccessLevel(int accessLevel) { this.accessLevel = accessLevel; }

    public String getNotes()                    { return notes; }
    public void setNotes(String notes)          { this.notes = notes; }

    @Override
    public String toString() {
        return String.format("Admin{id=%d, username='%s', level=%d}",
                id, username, accessLevel);
    }
}
