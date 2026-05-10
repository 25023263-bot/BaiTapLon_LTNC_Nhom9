package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.AuthenticationException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth.AuthService;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Controller cho UBID LoginView — có thể dùng fullscreen hoặc modal (qua {@link #configureForModal}).
 */
public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox      rememberCheckBox;

    /** Dịch vụ xác thực; có thể null khi chỉ demo — modal flow luôn gán qua coordinator. */
    private AuthService authService;

    /**
     * Khi đăng nhập OK: nhận user (đã nằm trong UserSession).
     */
    private Consumer<User> onLoginSuccess = u -> {};

    /**
     * Khi user bấm "SIGN UP NOW": coordinator sẽ đóng Login và mở Register.
     *
     * Mặc định là no-op (không làm gì) — coordinator gán qua {@link #setOnSignUpRequest}.
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
     * @param onSuccess   callback khi đăng nhập thành công
     */
    public void configureForModal(AuthService authService, Consumer<User> onSuccess) {
        this.authService    = Objects.requireNonNull(authService);
        this.onLoginSuccess = onSuccess != null ? onSuccess : u -> {};
    }

    /**
     * Gán hành động khi user bấm "SIGN UP NOW".
     *
     * <p>Coordinator gọi setter này SAU {@link #configureForModal} để nối
     * nút Sign Up với luồng mở RegisterView. Controller không cần biết
     * Register hoạt động thế nào — nó chỉ gọi {@code onSignUpRequest.run()}.</p>
     *
     * @param onSignUpRequest lambda do coordinator cung cấp
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
        String user = usernameField != null ? usernameField.getText() : "";
        String pass = passwordField != null ? passwordField.getText() : "";

        if (authService == null) {
            showError("AuthService chưa được khởi tạo — không đăng nhập được.");
            return;
        }
        try {
            User loggedIn = authService.login(user, pass);
            onLoginSuccess.accept(loggedIn);
        } catch (AuthenticationException ex) {
            showError(mapAuthMessage(ex));
        } catch (Exception ex) {
            showError("Lỗi hệ thống khi đăng nhập: " + ex.getMessage());
        }
    }

    @FXML
    private void onSignUp(ActionEvent event) {
        // Delegate hoàn toàn cho coordinator — controller không mở cửa sổ trực tiếp
        onSignUpRequest.run();
    }

    @FXML
    private void onForgotPassword(ActionEvent event) {
        showInfo("Quên mật khẩu — ghép recovery/email theo đồ án.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String mapAuthMessage(AuthenticationException ex) {
        AuthenticationException.Reason reason = ex.getReason();
        if (reason != null) return reason.getMessage();
        return ex.getMessage() != null ? ex.getMessage() : "Đăng nhập thất bại.";
    }

    private static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("UBID");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("UBID — Đăng nhập");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}