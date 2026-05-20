package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.*;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.AuthenticationException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.DuplicateUserException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.UserRepository;

import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Xử lý đăng nhập, đăng ký, đổi mật khẩu.
 *
 * <p><b>Nguyên tắc quan trọng:</b> AuthService là tầng Service — nó KHÔNG biết
 * UI tồn tại. Việc ghi vào UserSession (UI state) là trách nhiệm của Controller
 * hoặc Coordinator, không phải của Service.
 *
 * <p>Lý do: nếu sau này bạn xây REST API, AuthService vẫn dùng được nguyên vẹn
 * mà không cần sửa, vì nó không phụ thuộc vào bất kỳ thứ gì của JavaFX/UI.
 */
public class AuthService implements Authenticatable {

    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());

    private final UserRepository userRepo;

    public AuthService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    /**
     * Xác thực thông tin đăng nhập và trả về User nếu hợp lệ.
     *
     * <p><b>Lưu ý cho người gọi:</b> method này CHỈ xác thực và trả về User.
     * Người gọi (Controller/Coordinator) tự quyết định phải làm gì tiếp theo,
     * ví dụ: {@code UserSession.getInstance().login(user)}.
     *
     * @return User đã xác thực thành công
     * @throws AuthenticationException nếu thông tin sai hoặc tài khoản bị khoá
     */
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

            // KHÔNG ghi UserSession ở đây — đó là việc của UI layer.
            // Service chỉ xác nhận "user này hợp lệ" rồi trả về.
            LOG.info("Xác thực thành công: " + user.getUsername());
            return user;

        } catch (SQLException e) {
            LOG.severe("Lỗi DB khi đăng nhập: " + e.getMessage());
            throw new AuthenticationException("Lỗi hệ thống, vui lòng thử lại.");
        }
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    /**
     * Thực hiện các tác vụ cleanup phía server khi đăng xuất (nếu có).
     *
     * <p>Việc xoá UserSession là trách nhiệm của Coordinator, không phải ở đây.
     * Khi tích hợp Spring Boot sau này, đây là nơi bạn sẽ invalidate JWT token.
     *
     * @param username username đang đăng xuất — chỉ dùng để ghi log
     */
    @Override
    public void logout(String username) {
        // TODO: khi có Spring Security — invalidate token tại đây
        LOG.info("Đã đăng xuất: " + username);
    }

    // ─── Register ─────────────────────────────────────────────────────────────

    @Override
    public User register(String username, String email, String rawPassword,
                         String fullName, String phone, String role)
            throws DuplicateUserException, Exception {

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
                case ADMIN  -> new Admin(0, username.trim(), email.trim(), hash, fullName, phone);
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