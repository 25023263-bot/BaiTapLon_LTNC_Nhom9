package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;

/**
 * Singleton giữ trạng thái phiên đăng nhập hiện tại.
 * Truy cập từ bất kỳ Controller nào mà không cần dependency injection.
 */
public class UserSession {

    private static UserSession instance;
    private User currentUser;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) instance = new UserSession();
        return instance;
    }

    // ─── Login / Logout ───────────────────────────────────────────────────────

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    // ─── State ────────────────────────────────────────────────────────────────

    public boolean isLoggedIn() { return currentUser != null; }

    public User getCurrentUser() { return currentUser; }

    public int getCurrentUserId() {
        requireLogin();
        return currentUser.getId();
    }

    public String getCurrentUsername() {
        requireLogin();
        return currentUser.getUsername();
    }

    public UserRole getCurrentRole() {
        requireLogin();
        return currentUser.getRole();
    }

    public boolean isBuyer()  { return isLoggedIn() && currentUser.getRole() == UserRole.BUYER;  }
    public boolean isSeller() { return isLoggedIn() && currentUser.getRole() == UserRole.SELLER; }
    public boolean isAdmin()  { return isLoggedIn() && currentUser.getRole() == UserRole.ADMIN;  }

    // ─── Guard ────────────────────────────────────────────────────────────────

    private void requireLogin() {
        if (!isLoggedIn())
            throw new IllegalStateException("Chưa đăng nhập – UserSession trống.");
    }
}