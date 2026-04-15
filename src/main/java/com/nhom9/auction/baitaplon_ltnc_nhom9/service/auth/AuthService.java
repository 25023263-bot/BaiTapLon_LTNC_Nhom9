package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.*;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.AuthenticationException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.DuplicateUserException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.UserRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;

import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Xử lý đăng nhập, đăng ký, đổi mật khẩu.
 */
public class AuthService implements Authenticatable {

    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());

    private final UserRepository userRepo;

    public AuthService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @Override
    public User login(String username, String rawPassword) throws AuthenticationException {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank())
            throw new AuthenticationException(AuthenticationException.Reason.INVALID_CREDENTIALS);

        try {
            User user = userRepo.findByUsername(username.trim())
                    .orElseThrow(() -> new AuthenticationException(
                            AuthenticationException.Reason.INVALID_CREDENTIALS));

            if (!user.isActive())
                throw new AuthenticationException(AuthenticationException.Reason.ACCOUNT_DISABLED);

            if (!PasswordHasher.verify(rawPassword, user.getPasswordHash()))
                throw new AuthenticationException(AuthenticationException.Reason.INVALID_CREDENTIALS);

            // Lưu vào session
            UserSession.getInstance().login(user);
            LOG.info("Đăng nhập thành công: " + user.getUsername());
            return user;

        } catch (SQLException e) {
            LOG.severe("Lỗi DB khi đăng nhập: " + e.getMessage());
            throw new AuthenticationException("Lỗi hệ thống, vui lòng thử lại.");
        }
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @Override
    public void logout() {
        String username = UserSession.getInstance().isLoggedIn()
                ? UserSession.getInstance().getCurrentUser().getUsername() : "unknown";
        UserSession.getInstance().logout();
        LOG.info("Đã đăng xuất: " + username);
    }

    // ─── Register ─────────────────────────────────────────────────────────────

    @Override
    public User register(String username, String email, String rawPassword,
                         String fullName, String phone, String role)
            throws DuplicateUserException, Exception {

        // Validate input
        validateRegistration(username, email, rawPassword);

        try {
            if (userRepo.existsByUsername(username.trim()))
                throw new DuplicateUserException(DuplicateUserException.Field.USERNAME, username);
            if (userRepo.existsByEmail(email.trim()))
                throw new DuplicateUserException(DuplicateUserException.Field.EMAIL, email);

            String hash = PasswordHasher.hash(rawPassword);
            UserRole userRole = UserRole.fromString(role);

            User newUser = switch (userRole) {
                case BUYER  -> new Buyer (0, username.trim(), email.trim(), hash, fullName, phone);
                case SELLER -> new Seller(0, username.trim(), email.trim(), hash, fullName, phone);
                case ADMIN  -> new Admin (0, username.trim(), email.trim(), hash, fullName, phone, 1);
            };

            userRepo.save(newUser);
            LOG.info("Đăng ký thành công: " + newUser.getUsername() + " [" + userRole + "]");
            return newUser;

        } catch (DuplicateUserException e) {
            throw e;
        } catch (SQLException e) {
            LOG.severe("Lỗi DB khi đăng ký: " + e.getMessage());
            throw new Exception("Lỗi hệ thống khi đăng ký: " + e.getMessage());
        }
    }

    // ─── Change Password ──────────────────────────────────────────────────────

    @Override
    public void changePassword(int userId, String oldRaw, String newRaw)
            throws AuthenticationException {
        try {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new AuthenticationException("Không tìm thấy tài khoản."));

            if (!PasswordHasher.verify(oldRaw, user.getPasswordHash()))
                throw new AuthenticationException(AuthenticationException.Reason.INVALID_CREDENTIALS);

            if (!PasswordHasher.isStrong(newRaw))
                throw new AuthenticationException(
                        "Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, thường và số.");

            String newHash = PasswordHasher.hash(newRaw);
            userRepo.updatePassword(userId, newHash);
            LOG.info("Đổi mật khẩu thành công cho user #" + userId);

        } catch (AuthenticationException e) {
            throw e;
        } catch (SQLException e) {
            throw new AuthenticationException("Lỗi hệ thống khi đổi mật khẩu.");
        }
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    private void validateRegistration(String username, String email, String rawPassword)
            throws IllegalArgumentException {
        if (username == null || username.trim().length() < 3)
            throw new IllegalArgumentException("Tên đăng nhập phải có ít nhất 3 ký tự.");
        if (!username.matches("[a-zA-Z0-9_]+"))
            throw new IllegalArgumentException("Tên đăng nhập chỉ được chứa chữ, số và dấu gạch dưới.");
        if (email == null || !email.matches("^[\\w.+-]+@[\\w-]+\\.[a-z]{2,}$"))
            throw new IllegalArgumentException("Email không hợp lệ.");
        if (!PasswordHasher.isStrong(rawPassword))
            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, thường và số.");
    }
}