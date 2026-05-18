package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator;

import com.nhom9.auction.baitaplon_ltnc_nhom9.HelloApplication;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller.RegisterController;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;

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
 * Coordinator điều phối màn hình đăng ký.
 *
 * <h3>Bước 8 — Xoá AuthService:</h3>
 * <ul>
 *   <li>Bỏ field {@code authService} và tham số khỏi constructor.</li>
 *   <li>{@code configure()} của RegisterController chỉ nhận callback.</li>
 *   <li>Sau khi đăng ký thành công → tự động mở Login.</li>
 * </ul>
 */
public final class RegisterCoordinator {

    private static final Logger LOG = Logger.getLogger(RegisterCoordinator.class.getName());

    private final Window owner;
    private final HomeLoginCoordinator loginCoordinator; // có thể null

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Constructor chính — dùng khi có LoginCoordinator để mở Login sau register.
     */
    public RegisterCoordinator(Window owner, HomeLoginCoordinator loginCoordinator) {
        this.owner            = Objects.requireNonNull(owner, "owner không được null");
        this.loginCoordinator = loginCoordinator;
    }

    /**
     * Constructor tối giản — không tự mở Login sau register.
     */
    public RegisterCoordinator(Window owner) {
        this(owner, null);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void openRegisterWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/RegisterView.fxml"));
            Parent root = loader.load();

            RegisterController controller = loader.getController();
            Stage registerStage = new Stage();

            // configure chỉ nhận 2 callback, không nhận AuthService
            controller.configure(
                    () -> handleRegisterSuccess(registerStage),  // onSuccess
                    () -> handleBackToLogin(registerStage)        // onBackToLogin
            );

            Scene scene = new Scene(root);
            registerStage.setTitle("Đăng ký — UBid");
            registerStage.initOwner(owner);
            registerStage.initModality(Modality.WINDOW_MODAL);
            registerStage.setMinWidth(720);
            registerStage.setMinHeight(600);
            registerStage.setWidth(900);
            registerStage.setHeight(700);
            registerStage.setScene(scene);
            registerStage.showAndWait();

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Không tải được RegisterView.fxml", e);
            AlertHelper.showError("Lỗi hệ thống", "Không thể mở màn hình đăng ký.");
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    /**
     * Đăng ký thành công → đóng Register → mở Login.
     */
    private void handleRegisterSuccess(Stage registerStage) {
        registerStage.close();
        if (loginCoordinator != null) {
            // Dùng runLater để tránh nested event loop
            Platform.runLater(loginCoordinator::openLoginWindow);
        }
    }

    /**
     * User bấm "SIGN IN" → đóng Register → mở Login.
     */
    private void handleBackToLogin(Stage registerStage) {
        registerStage.close();
        if (loginCoordinator != null) {
            Platform.runLater(loginCoordinator::openLoginWindow);
        }
    }
}
