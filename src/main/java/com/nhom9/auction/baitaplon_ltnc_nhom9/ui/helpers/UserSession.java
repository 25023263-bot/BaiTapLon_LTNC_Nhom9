package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.UserDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;

/**
 * Singleton giữ trạng thái phiên đăng nhập hiện tại.
 * Truy cập từ bất kỳ Controller nào mà không cần dependency injection.
 *
 * <h3>Bước 8 — Xoá login local:</h3>
 * <ul>
 *   <li>Bỏ field {@code currentUser} (User domain object).</li>
 *   <li>Bỏ method {@code login(User)} — không còn login local.</li>
 *   <li>Bỏ import {@code User} — client không còn phụ thuộc domain model.</li>
 *   <li>Chỉ còn {@link #loginWithDTO(UserDTO)} — luôn đi qua socket.</li>
 * </ul>
 *
 * <h3>Nguyên tắc sử dụng:</h3>
 * <ul>
 *   <li>Chỉ tầng UI (Controller, Coordinator) được đọc/ghi UserSession.</li>
 *   <li>Service layer KHÔNG được import hoặc dùng UserSession.</li>
 * </ul>
 */
public class UserSession {

    private static final UserSession INSTANCE = new UserSession();

    /** UserDTO nhận từ server sau khi login thành công qua socket. */
    private UserDTO currentUserDTO;

    private UserSession() {}

    public static UserSession getInstance() {
        return INSTANCE;
    }

    // ─── Login / Logout ───────────────────────────────────────────────────────

    /**
     * Login qua socket — server trả về UserDTO sau khi xác thực.
     * Đây là cách login duy nhất trong kiến trúc client-server.
     */
    public void loginWithDTO(UserDTO dto) {
        this.currentUserDTO = dto;
    }

    /**
     * Xoá session khi đăng xuất.
     */
    public void logout() {
        this.currentUserDTO = null;
    }

    // ─── State queries ────────────────────────────────────────────────────────

    public boolean isLoggedIn() {
        return currentUserDTO != null;
    }

    /**
     * Trả về UserDTO của user hiện tại.
     * Null nếu chưa đăng nhập.
     */
    public UserDTO getCurrentUserDTO() {
        return currentUserDTO;
    }

    public int getCurrentUserId() {
        requireLogin();
        return currentUserDTO.getId();
    }

    public String getCurrentUsername() {
        requireLogin();
        return currentUserDTO.getUsername();
    }

    public String getCurrentFullName() {
        requireLogin();
        return currentUserDTO.getFullName();
    }

    public UserRole getCurrentRole() {
        requireLogin();
        return currentUserDTO.getRole();
    }

    public boolean isBuyer() {
        return isLoggedIn() && getCurrentRole() == UserRole.BUYER;
    }

    public boolean isSeller() {
        return isLoggedIn() && getCurrentRole() == UserRole.SELLER;
    }

    public boolean isAdmin() {
        return isLoggedIn() && getCurrentRole() == UserRole.ADMIN;
    }

    // ─── Guard ────────────────────────────────────────────────────────────────

    private void requireLogin() {
        if (!isLoggedIn())
            throw new IllegalStateException("Chưa đăng nhập — UserSession trống.");
    }
}
