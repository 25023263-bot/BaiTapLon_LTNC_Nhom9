package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;

/**
 * Singleton giữ trạng thái phiên đăng nhập hiện tại.
 * Truy cập từ bất kỳ Controller nào mà không cần dependency injection.
 *
 * <h3>Tại sao dùng Eager Initialization?</h3>
 * <pre>
 *   // Lazy (cũ — có vấn đề tiềm ẩn):
 *   if (instance == null) instance = new UserSession();
 *
 *   // Eager (mới — JVM đảm bảo thread-safe):
 *   private static final UserSession INSTANCE = new UserSession();
 * </pre>
 *
 * <p>UserSession rất nhẹ (chỉ chứa 1 field User), nên eager init không lãng phí
 * tài nguyên và loại bỏ hoàn toàn nguy cơ race condition.
 *
 * <h3>Nguyên tắc sử dụng:</h3>
 * <ul>
 *   <li>Chỉ tầng UI (Controller, Coordinator) được đọc/ghi UserSession</li>
 *   <li>Service layer KHÔNG được import hoặc dùng UserSession</li>
 * </ul>
 */
public class UserSession {

    // ── Eager initialization — an toàn, không cần synchronized ───────────────
    private static final UserSession INSTANCE = new UserSession();

    private User currentUser;

    private UserSession() {}

    public static UserSession getInstance() {
        return INSTANCE;
    }

    // ─── Login / Logout ───────────────────────────────────────────────────────

    /**
     * Ghi user vào session sau khi đăng nhập thành công.
     * Gọi bởi: LoginController (sau khi AuthService xác thực OK).
     */
    public void login(User user) {
        this.currentUser = user;
    }

    /**
     * Xoá session khi đăng xuất.
     * Gọi bởi: HomeLoginCoordinator.performLogout().
     */
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

    public boolean isBuyer()  { return isLoggedIn() && currentUser.getRole() == UserRole.BUYER;  }
    public boolean isSeller() { return isLoggedIn() && currentUser.getRole() == UserRole.SELLER; }
    public boolean isAdmin()  { return isLoggedIn() && currentUser.getRole() == UserRole.ADMIN;  }

    // ─── Guard ────────────────────────────────────────────────────────────────

    private void requireLogin() {
        if (!isLoggedIn())
            throw new IllegalStateException("Chưa đăng nhập — UserSession trống.");
    }
}