package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator;

import com.nhom9.auction.baitaplon_ltnc_nhom9.HelloApplication;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller.LoginController;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinator điều phối luồng Login ↔ Register ↔ Home.
 *
 * <h3>Bước 8 — Xoá AuthService:</h3>
 * <ul>
 *   <li>Bỏ field {@code authService}, import {@code AuthService},
 *       {@code UserRepository}.</li>
 *   <li>{@code configureForModal()} chỉ nhận 1 callback thay vì 2 tham số.</li>
 *   <li>{@code performLogout()} chỉ xóa {@code UserSession} — không gọi
 *       {@code authService.logout()} nữa (server stateless).</li>
 *   <li>{@code openRegisterWindow()} không truyền {@code authService}.</li>
 * </ul>
 */
public final class HomeLoginCoordinator {

    private static final Logger LOG = Logger.getLogger(HomeLoginCoordinator.class.getName());

    private final Window  owner;
    private Runnable onAuthStateChanged = () -> {};

    public HomeLoginCoordinator(Window owner) {
        this.owner = Objects.requireNonNull(owner, "owner không được null");
    }

    public void setOnAuthStateChanged(Runnable onAuthStateChanged) {
        this.onAuthStateChanged = onAuthStateChanged != null ? onAuthStateChanged : () -> {};
    }

    // ── Open Login ────────────────────────────────────────────────────────────

    public void openLoginWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();
            LoginController ctrl = loader.getController();

            Stage loginStage = new Stage();

            // Chỉ truyền callback — không cần AuthService nữa
            ctrl.configureForModal(user -> finishLogin(loginStage));

            ctrl.setOnSignUpRequest(() -> {
                loginStage.close();
                Platform.runLater(HomeLoginCoordinator.this::openRegisterWindow);
            });

            Scene scene = new Scene(root);
            loginStage.setTitle("Đăng nhập — UBid");
            loginStage.initOwner(owner);
            loginStage.initModality(Modality.WINDOW_MODAL);
            loginStage.setMinWidth(640);
            loginStage.setMinHeight(480);
            loginStage.setScene(scene);
            loginStage.showAndWait();

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Không tải được LoginView.fxml", e);
            AlertHelper.showError("Lỗi hệ thống", "Không thể mở màn hình đăng nhập.");
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * Đăng xuất: xóa session phía client.
     * Server stateless — không cần gọi thêm gì.
     * Nếu muốn ghi log phía server, thêm ServerConnection.logout() vào đây.
     */
    public void performLogout() {
        String username = UserSession.getInstance().isLoggedIn()
                ? UserSession.getInstance().getCurrentUsername() : "unknown";
        LOG.info("Đăng xuất: " + username);
        UserSession.getInstance().logout();
        onAuthStateChanged.run();
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void finishLogin(Stage loginStage) {
        loginStage.close();
        UserSession s = UserSession.getInstance();
        LOG.info(s.isLoggedIn()
                ? "Phiên đăng nhập: " + s.getCurrentUsername()
                : "Đăng nhập — session trống (bất thường)");
        onAuthStateChanged.run();
    }

    private void openRegisterWindow() {
        // Không truyền authService nữa
        RegisterCoordinator registerCoord = new RegisterCoordinator(owner, this);
        registerCoord.openRegisterWindow();
    }
}
