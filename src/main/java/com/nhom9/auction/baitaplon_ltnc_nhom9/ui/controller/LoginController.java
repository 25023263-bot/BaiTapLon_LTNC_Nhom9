package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.WARNING, "Thiếu thông tin",
                    "Vui lòng nhập đầy đủ tài khoản và mật khẩu.");
            return;
        }

        if (!isValidUsername(username)) {
            showAlert(AlertType.ERROR, "Tài khoản không hợp lệ",
                    "Tài khoản chỉ được chứa chữ cái (a-z, A-Z) và số (0-9).\n"
                            + "Không được dùng ký tự đặc biệt hoặc dấu tiếng Việt.");
            usernameField.requestFocus();
            return;
        }

        if (!isValidPassword(password)) {
            showAlert(AlertType.ERROR, "Mật khẩu không hợp lệ",
                    "Mật khẩu phải có tối thiểu 8 ký tự,\n"
                            + "bao gồm ít nhất 1 chữ cái và 1 chữ số.");
            passwordField.requestFocus();
            return;
        }

        // TODO: thay bằng logic xác thực thực tế (database/service)
        boolean loginSuccess = checkCredentials(username, password);

        if (loginSuccess) {
            switchToMainView();
        } else {
            showAlert(AlertType.ERROR, "Đăng nhập thất bại",
                    "Tài khoản hoặc mật khẩu không đúng.");
        }
    }

    private boolean isValidUsername(String username) {
        for (char c : username.toCharArray()) {
            if (!Character.isLetterOrDigit(c) || c > 127) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 8) return false;

        boolean hasLetter = false;
        boolean hasDigit  = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c))  hasDigit  = true;
        }

        return hasLetter && hasDigit;
    }

    // Placeholder — thay bằng logic thật (gọi service, truy vấn DB, ...)
    private boolean checkCredentials(String username, String password) {
        return username.equals("admin") && password.equals("admin123");
    }

    private void switchToMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/fxml/ItemDetailView.fxml"
                    )
            );
            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Lỗi hệ thống",
                    "Không thể chuyển màn hình: " + e.getMessage());
        }
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}