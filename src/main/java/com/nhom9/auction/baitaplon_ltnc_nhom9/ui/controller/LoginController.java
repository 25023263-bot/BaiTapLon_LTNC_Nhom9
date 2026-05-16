package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.AuthenticationException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth.AuthService;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Controller cho UBID LoginView — có thể dùng fullscreen hoặc modal (qua {@link #configureForModal}).
 *
 * <p>Controller này có 2 trách nhiệm rõ ràng:
 * <ol>
 *   <li>Đọc input từ UI, gọi AuthService xác thực</li>
 *   <li>Sau khi xác thực thành công → ghi vào UserSession, rồi gọi callback</li>
 * </ol>
 *
 * <p>Lý do Controller ghi UserSession (không phải AuthService):
 * UserSession là trạng thái của UI — chỉ tầng UI mới được đọc/ghi nó.
 * AuthService chỉ biết "user này có hợp lệ không", không biết UI làm gì với kết quả.
 */
public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox      rememberCheckBox;

    /** Dịch vụ xác thực; coordinator gán qua {@link #configureForModal}. */
    private AuthService authService;

    /** Callback khi đăng nhập OK — coordinator gán handler để đóng cửa sổ và refresh UI. */
    private Consumer<User> onLoginSuccess = u -> {};

    /**
     * Callback khi user bấm "SIGN UP NOW" — coordinator gán để mở RegisterView.
     * Mặc định no-op để tránh NullPointerException nếu quên gán.
     */
    private Runnable onSignUpRequest = () -> {};

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            if (usernameField != null) usernameField.requestFocus();
        });
    }

    // ── Configuration (gọi bởi HomeLoginCoordinator) ──────────────────────────

    /**
     * Cấu hình khi mở từ {@link com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator.HomeLoginCoordinator}.
     *
     * @param authService service xác thực
     * @param onSuccess   callback sau khi login thành công và session đã được ghi
     */
    public void configureForModal(AuthService authService, Consumer<User> onSuccess) {
        this.authService    = Objects.requireNonNull(authService);
        this.onLoginSuccess = onSuccess != null ? onSuccess : u -> {};
    }

    /**
     * Gán hành động khi user bấm "SIGN UP NOW".
     * Coordinator gọi setter này SAU {@link #configureForModal}.
     */
    public void setOnSignUpRequest(Runnable onSignUpRequest) {
        this.onSignUpRequest = onSignUpRequest != null ? onSignUpRequest : () -> {};
    }

    public void setStandaloneAuth(AuthService authService) {
        this.authService = authService;
    }

    // ── Event Handlers ────────────────────────────────────────────────────────

    @FXML
    private void onLogin(ActionEvent event) {
        String username = usernameField != null ? usernameField.getText() : "";
        String password = passwordField != null ? passwordField.getText() : "";

        if (authService == null) {
            AlertHelper.showError("UBID — Đăng nhập", "AuthService chưa được khởi tạo.");
            return;
        }
        try {
            // 1. AuthService xác thực — chỉ trả về User, không ghi session
            User loggedIn = authService.login(username, password);

            // 2. UI layer tự ghi session — đây là đúng chỗ
            UserSession.getInstance().login(loggedIn);

            // 3. Thông báo coordinator để đóng cửa sổ và refresh Home
            onLoginSuccess.accept(loggedIn);

        } catch (AuthenticationException ex) {
            AlertHelper.showError("UBID — Đăng nhập", mapAuthMessage(ex));
        } catch (Exception ex) {
            AlertHelper.showError("UBID — Đăng nhập", "Lỗi hệ thống: " + ex.getMessage());
        }
    }

    @FXML
    private void onSignUp(ActionEvent event) {
        // Delegate hoàn toàn cho coordinator — controller không mở cửa sổ trực tiếp
        onSignUpRequest.run();
    }

    @FXML
    private void onForgotPassword(ActionEvent event) {
        AlertHelper.showInfo("UBID", "Quên mật khẩu — ghép recovery/email theo đồ án.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String mapAuthMessage(AuthenticationException ex) {
        AuthenticationException.Reason reason = ex.getReason();
        if (reason != null) return reason.getMessage();
        return ex.getMessage() != null ? ex.getMessage() : "Đăng nhập thất bại.";
    }
}