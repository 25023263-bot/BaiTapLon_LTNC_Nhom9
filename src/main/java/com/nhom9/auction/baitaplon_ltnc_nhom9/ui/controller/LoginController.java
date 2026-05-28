package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.UserDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.network.ServerConnection;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

/**
 * Controller cho LoginView.
 *
 * <h3>Luồng đăng nhập:</h3>
 * <pre>
 *   User nhập username/password
 *       │
 *       ▼  Background thread
 *   ServerConnection.login(username, password)
 *       │
 *       ▼  Server xác thực, trả UserDTO
 *   OK  → lưu UserDTO vào UserSession → gọi onLoginSuccess
 *   ERR → hiện thông báo lỗi
 * </pre>
 */
public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox      rememberCheckBox;
    @FXML private Button        loginButton;

    /** Callback khi đăng nhập OK — coordinator gán handler để đóng cửa sổ và refresh UI. */
    private Consumer<User> onLoginSuccess = u -> {};

    /** Callback khi user bấm "SIGN UP NOW". */
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
     * Cấu hình controller khi mở ở dạng modal từ HomeLoginCoordinator.
     *
     * @param onSuccess callback khi login thành công (nhận User, có thể null nếu login qua socket)
     */
    public void configureForModal(Consumer<User> onSuccess) {
        this.onLoginSuccess = onSuccess != null ? onSuccess : u -> {};
    }

    public void setOnSignUpRequest(Runnable onSignUpRequest) {
        this.onSignUpRequest = onSignUpRequest != null ? onSignUpRequest : () -> {};
    }

    // ── Event Handlers ────────────────────────────────────────────────────────

    @FXML
    private void onLogin(ActionEvent event) {
        String username = usernameField != null ? usernameField.getText().trim() : "";
        String password = passwordField != null ? passwordField.getText() : "";

        if (username.isEmpty() || password.isEmpty()) {
            AlertHelper.showError("UBID — Đăng nhập", "Vui lòng nhập tên đăng nhập và mật khẩu.");
            return;
        }

        if (!ServerConnection.isConnected()) {
            AlertHelper.showError("UBID — Không có kết nối",
                    "Không thể kết nối đến server.\n" +
                            "Hãy đảm bảo AuctionServer đang chạy và thử lại.");
            return;
        }

        loginAsync(username, password);
    }

    @FXML
    private void onSignUp(ActionEvent event) {
        onSignUpRequest.run();
    }

    @FXML
    private void onForgotPassword(ActionEvent event) {
        AlertHelper.showInfo("UBID", "Tính năng quên mật khẩu đang được phát triển.");
    }

    // ── Login via socket ──────────────────────────────────────────────────────

    /**
     * Gọi ServerConnection.login() trên background thread — không block JavaFX UI thread.
     */
    private void loginAsync(String username, String password) {
        setLoginButtonEnabled(false);

        Thread t = new Thread(() -> {
            try {
                UserDTO loggedIn = ServerConnection.login(username, password);

                Platform.runLater(() -> {
                    setLoginButtonEnabled(true);
                    UserSession.getInstance().loginWithDTO(loggedIn);
                    onLoginSuccess.accept(null);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoginButtonEnabled(true);
                    AlertHelper.showError("UBID — Đăng nhập",
                            e.getMessage() != null ? e.getMessage() :
                                    "Mất kết nối đến server.\nKiểm tra AuctionServer đang chạy và thử lại.");
                });
            }
        }, "login-request-thread");
        t.setDaemon(true);
        t.start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setLoginButtonEnabled(boolean enabled) {
        if (loginButton != null) loginButton.setDisable(!enabled);
    }
}
