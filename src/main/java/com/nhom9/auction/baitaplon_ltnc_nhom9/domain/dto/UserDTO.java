package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private UserRole role;
    private boolean active;
    private LocalDateTime createdAt;

    private BigDecimal walletBalance;
    private BigDecimal earningsBalance;

    public UserDTO() {}

    public boolean isBuyer()  { return role == UserRole.BUYER; }
    public boolean isAdmin()  { return role == UserRole.ADMIN; }

    public String getDisplayName() {
        return (fullName != null && !fullName.isBlank()) ? fullName : username;
    }

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

    public BigDecimal getEarningsBalance()              { return earningsBalance; }
    public void setEarningsBalance(BigDecimal balance)  { this.earningsBalance = balance; }

    @Override
    public String toString() {
        return String.format("UserDTO{id=%d, username='%s', role=%s}", id, username, role);
    }
}