package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.UserRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginController {
    private final UserRepository userDAO = new UserRepository();
    // ── Login fields ──────────────────────────────
    @FXML private VBox          loginCard;
    @FXML private TextField     loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private CheckBox      rememberMe;
    @FXML private Label         loginError;

    // ── Register fields ───────────────────────────
    @FXML private VBox          registerCard;
    @FXML private TextField     regFullName;
    @FXML private TextField     regUsername;
    @FXML private TextField     regEmail;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirm;
    @FXML private Label         regError;

    // ══════════════════════════════════════════════
    //  CHUYỂN SANG FORM ĐĂNG KÝ
    // ══════════════════════════════════════════════
    @FXML
    private void handleRegister() {
        loginCard.setVisible(false);
        loginCard.setManaged(false);
        registerCard.setVisible(true);
        registerCard.setManaged(true);
    }

    // TODO: Enter xuống dòng
    public void initialize() {
        // 🔥 Enter ở username → xuống password
        loginUsername.setOnAction(e -> loginPassword.requestFocus());

        // 🔥 Enter ở password → login
        loginPassword.setOnAction(e -> onLogin());
    }

    // ══════════════════════════════════════════════
    //  QUAY LẠI FORM ĐĂNG NHẬP
    // ══════════════════════════════════════════════
    @FXML
    private void handleBackToLogin() {
        registerCard.setVisible(false);
        registerCard.setManaged(false);
        loginCard.setVisible(true);
        loginCard.setManaged(true);
    }

    // ══════════════════════════════════════════════
    //  XỬ LÝ ĐĂNG NHẬP
    // ══════════════════════════════════════════════
    @FXML
    private void onLogin() {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // ✅ Kiểm tra từ database
        User user = userDAO.login(username, password);
        if (user != null) {
            System.out.println("Đăng nhập thành công: " + user.getFullName());
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/GUI.fxml")
                );
                Parent root = loader.load();
                Stage stage = (Stage) loginUsername.getScene().getWindow();
                stage.getScene().setRoot(root);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            showError("✘ Sai tên đăng nhập hoặc mật khẩu!");
        }
    }

    @FXML
    private void onRegister() {
        String fullName = regFullName.getText().trim();
        String username = regUsername.getText().trim();
        String email    = regEmail.getText().trim();
        String password = regPassword.getText();
        String confirm  = regConfirm.getText();

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirm.isEmpty()) {
            showRegError("Vui lòng điền đầy đủ thông tin.");
            return;
        }
        if (!password.equals(confirm)) {
            showRegError("Mật khẩu xác nhận không khớp.");
            return;
        }

        // ✅ Lưu vào database
        boolean success = userDAO.register(fullName, username, email, password);
        if (success) {
            System.out.println("✅ Đăng ký thành công!");
            handleBackToLogin();
        } else {
            showRegError("❌ Username hoặc email đã tồn tại!");
        }
    }
    @FXML
    private void forgotPassword() {
        // TODO: xử lý quên mật khẩu
        System.out.println("Quên mật khẩu");
    }

    // ── Helpers ───────────────────────────────────


    private void showRegError(String msg) {
        regError.setText(msg);
        regError.setVisible(true);
        regError.setManaged(true);
    }
    private void showError(String message) {
        loginError.setText(message);
        loginError.setVisible(true);
        loginError.setManaged(true);
    }

    private void hideError() {
        loginError.setVisible(false);
        loginError.setManaged(false);
    }
}
