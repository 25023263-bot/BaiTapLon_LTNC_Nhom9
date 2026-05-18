package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.UserDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.server.protocol.Request;
import com.nhom9.auction.baitaplon_ltnc_nhom9.server.protocol.Response;
import com.nhom9.auction.baitaplon_ltnc_nhom9.client.SocketClient;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;

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
 * <h3>Bước 8 — Xoá fallback offline:</h3>
 * <ul>
 *   <li>Bỏ field {@code authService} và import {@code AuthService}.</li>
 *   <li>Bỏ method {@code loginLocally()} — không còn gọi DB trực tiếp từ client.</li>
 *   <li>Bỏ method {@code setStandaloneAuth()} và tham số {@code authService}
 *       trong {@code configureForModal()}.</li>
 *   <li>{@code onLogin()} luôn gọi {@code loginViaSocket()} —
 *       nếu server offline thì hiện lỗi rõ ràng.</li>
 * </ul>
 *
 * <h3>Luồng đăng nhập:</h3>
 * <pre>
 *   User nhập username/password
 *       │
 *       ▼  Background thread
 *   SocketClient.sendRequest(LOGIN, dto)
 *       │
 *       ▼  Server xác thực, trả Response
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

        if (!SocketClient.getInstance().isConnected()) {
            AlertHelper.showError("UBID — Không có kết nối",
                    "Không thể kết nối đến server.\n" +
                            "Hãy đảm bảo AuctionServer đang chạy và thử lại.");
            return;
        }

        loginViaSocket(username, password);
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
     * Login qua socket trên background thread — không block JavaFX UI thread.
     *
     * Convention: password gửi trong field {@code phone} của UserDTO
     * (vì DTO không có field password riêng).
     */
    private void loginViaSocket(String username, String password) {
        setLoginButtonEnabled(false);

        UserDTO dto = new UserDTO();
        dto.setUsername(username);
        dto.setPhone(password); // phone = password (convention)

        Thread t = new Thread(() -> {
            try {
                Request  req = new Request(Request.Type.LOGIN, dto);
                Response res = SocketClient.getInstance().sendRequest(req);

                Platform.runLater(() -> {
                    setLoginButtonEnabled(true);
                    if (res.isOk()) {
                        UserDTO loggedIn = (UserDTO) res.getData();
                        UserSession.getInstance().loginWithDTO(loggedIn);
                        onLoginSuccess.accept(null);
                    } else {
                        AlertHelper.showError("UBID — Đăng nhập", res.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoginButtonEnabled(true);
                    AlertHelper.showError("UBID — Lỗi kết nối",
                            "Mất kết nối đến server.\n" +
                                    "Kiểm tra AuctionServer đang chạy và thử lại.");
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
