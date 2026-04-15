package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Lớp cơ sở trừu tượng cho mọi người dùng trong hệ thống.
 * Được kế thừa bởi Buyer, Seller, Admin.
 */
public abstract class User {

    protected int id;
    protected String username;
    protected String email;
    protected String passwordHash;
    protected String fullName;
    protected String phone;
    protected UserRole role;
    protected boolean active;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    // ─── Constructor ────────────────────────────────────────────────────────

    protected User() {}

    protected User(int id, String username, String email, String passwordHash,
                   String fullName, String phone, UserRole role) {
        this.id           = id;
        this.username     = username;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.fullName     = fullName;
        this.phone        = phone;
        this.role         = role;
        this.active       = true;
        this.createdAt    = LocalDateTime.now();
        this.updatedAt    = LocalDateTime.now();
    }

    // ─── Abstract ────────────────────────────────────────────────────────────

    /** Trả về mô tả ngắn về quyền/chức năng của user này. */
    public abstract String getRoleDescription();

    // ─── Business Methods ────────────────────────────────────────────────────

    public boolean isActive() { return active; }

    public void deactivate() {
        this.active    = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.active    = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(String fullName, String phone) {
        if (fullName != null && !fullName.isBlank()) this.fullName = fullName.trim();
        if (phone    != null && !phone.isBlank())    this.phone    = phone.trim();
        this.updatedAt = LocalDateTime.now();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.updatedAt    = LocalDateTime.now();
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public int getId()                    { return id; }
    public void setId(int id)             { this.id = id; }

    public String getUsername()                      { return username; }
    public void setUsername(String username)         { this.username = username; }

    public String getEmail()                         { return email; }
    public void setEmail(String email)               { this.email = email; }

    public String getPasswordHash()                  { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName()                      { return fullName; }
    public void setFullName(String fullName)         { this.fullName = fullName; }

    public String getPhone()                         { return phone; }
    public void setPhone(String phone)               { this.phone = phone; }

    public UserRole getRole()                        { return role; }
    public void setRole(UserRole role)               { this.role = role; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime t)        { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()              { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)        { this.updatedAt = t; }

    public void setActive(boolean active)            { this.active = active; }

    // ─── Object ──────────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return id == ((User) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("User{id=%d, username='%s', role=%s, active=%s}",
                id, username, role, active);
    }

}