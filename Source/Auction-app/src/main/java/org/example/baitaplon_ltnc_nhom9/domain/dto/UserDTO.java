package org.example.baitaplon_ltnc_nhom9.domain.dto;

import org.example.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object cho User.
 * Dùng để truyền dữ liệu giữa Service → Controller → UI.
 * KHÔNG chứa passwordHash – an toàn khi truyền ra ngoài.
 */
public class UserDTO {

    private int id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private UserRole role;
    private boolean active;
    private LocalDateTime createdAt;

    // Thông tin theo role (null nếu không áp dụng)
    private BigDecimal walletBalance;    // Buyer
    private int totalWins;              // Buyer
    private BigDecimal earningsBalance; // Seller
    private int totalSold;             // Seller
    private double rating;             // Seller
    private int ratingCount;           // Seller
    private int accessLevel;           // Admin

    // ─── Constructor ────────────────────────────────────────────────────────

    public UserDTO() {}

    // ─── Utility ─────────────────────────────────────────────────────────────

    public boolean isBuyer()  { return role == UserRole.BUYER; }
    public boolean isSeller() { return role == UserRole.SELLER; }
    public boolean isAdmin()  { return role == UserRole.ADMIN; }

    public String getDisplayName() {
        return (fullName != null && !fullName.isBlank()) ? fullName : username;
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public int getId()                                  { return id; }
    public void setId(int id)                           { this.id = id; }

    public String getUsername()                         { return username; }
    public void setUsername(String username)            { this.username = username; }

    public String getEmail()                            { return email; }
    public void setEmail(String email)                  { this.email = email; }

    public String getFullName()                         { return fullName; }
    public void setFullName(String fullName)            { this.fullName = fullName; }

    public String getPhone()                            { return phone; }
    public void setPhone(String phone)                  { this.phone = phone; }

    public UserRole getRole()                           { return role; }
    public void setRole(UserRole role)                  { this.role = role; }

    public boolean isActive()                           { return active; }
    public void setActive(boolean active)               { this.active = active; }

    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    public BigDecimal getWalletBalance()                { return walletBalance; }
    public void setWalletBalance(BigDecimal balance)    { this.walletBalance = balance; }

    public int getTotalWins()                           { return totalWins; }
    public void setTotalWins(int totalWins)             { this.totalWins = totalWins; }

    public BigDecimal getEarningsBalance()              { return earningsBalance; }
    public void setEarningsBalance(BigDecimal balance)  { this.earningsBalance = balance; }

    public int getTotalSold()                           { return totalSold; }
    public void setTotalSold(int totalSold)             { this.totalSold = totalSold; }

    public double getRating()                           { return rating; }
    public void setRating(double rating)                { this.rating = rating; }

    public int getRatingCount()                         { return ratingCount; }
    public void setRatingCount(int ratingCount)         { this.ratingCount = ratingCount; }

    public int getAccessLevel()                         { return accessLevel; }
    public void setAccessLevel(int accessLevel)         { this.accessLevel = accessLevel; }

    @Override
    public String toString() {
        return String.format("UserDTO{id=%d, username='%s', role=%s}", id, username, role);
    }
}