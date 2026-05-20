package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.AuthenticationException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.DuplicateUserException;

/**
 * Hợp đồng xác thực người dùng.
 */
public interface Authenticatable {

    /**
     * Xác thực và trả về User nếu thông tin hợp lệ.
     * Người gọi tự quyết định ghi vào UserSession sau khi nhận kết quả.
     *
     * @return User nếu thành công
     * @throws AuthenticationException nếu sai thông tin hoặc tài khoản bị khoá
     */
    User login(String username, String rawPassword) throws AuthenticationException;

    /**
     * Cleanup server-side khi đăng xuất (ghi log, invalidate token...).
     * Việc xoá UserSession là trách nhiệm của tầng UI (Coordinator/Controller).
     *
     * @param username username đang đăng xuất, dùng để ghi log
     */
    void logout(String username);

    /**
     * Đăng ký tài khoản mới.
     *
     * @throws DuplicateUserException nếu username/email đã tồn tại
     */
    User register(String username, String email, String rawPassword,
                  String fullName, String phone, String role)
            throws DuplicateUserException, Exception;

}