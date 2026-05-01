package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;

public abstract class User {
    protected String userId;
    protected String username;
    protected String password;
    protected UserRole role;

    public User(String userId, String username, String password, UserRole role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public boolean login(String pw) {
        return this.password.equals(pw);
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public UserRole getRole() { return role; }
}