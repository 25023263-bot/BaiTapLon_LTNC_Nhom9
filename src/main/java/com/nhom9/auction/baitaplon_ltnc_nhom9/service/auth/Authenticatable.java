package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.AuthenticationException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.DuplicateUserException;

/**
 * Hợp đồng xác thực người dùng.
 */
public interface Authenticatable {

    /**
     * Đăng nhập bằng username + mật khẩu thô.
     * @return User nếu thành công
     * @throws AuthenticationException nếu sai thông tin hoặc tài khoản bị khoá
     */
    User login(String username, String rawPassword) throws AuthenticationException;

    /**
     * Đăng xuất phiên hiện tại.
     */
    void logout();

    /**
     * Đăng ký tài khoản mới.
     * @throws DuplicateUserException nếu username/email đã tồn tại
     */
    User register(String username, String email, String rawPassword,
                  String fullName, String phone, String role)
            throws DuplicateUserException, Exception;

    /**
     * Đổi mật khẩu sau khi xác thực mật khẩu cũ.
     */
    void changePassword(int userId, String oldRaw, String newRaw)
            throws AuthenticationException;
}