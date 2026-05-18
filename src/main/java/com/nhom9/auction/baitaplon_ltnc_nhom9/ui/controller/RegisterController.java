package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.UserDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.network.ServerConnection;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Controller cho màn hình đăng ký (RegisterView.fxml).
 *
 * <h3>Bước 8 — Xoá AuthService:</h3>
 * <ul>
 *   <li>Bỏ field {@code authService} và import {@code AuthService}.</li>
 *   <li>{@code onRegister()} gọi {@link ServerConnection#register(UserDTO)}
 *       trên background thread thay vì gọi DB trực tiếp.</li>
 *   <li>{@code configure()} chỉ nhận callback, không nhận AuthService nữa.</li>
 * </ul>
 */
public class RegisterController {

    // ── FXML Fields ───────────────────────────────────────────────────────────

    @FXML private ToggleButton buyerToggle;
    @FXML private ToggleButton sellerToggle;
    @FXML private ToggleGroup  roleGroup;

    @FXML private TextField    fullNameField;
    @FXML private Label        fullNameError;

    @FXML private TextField    usernameField;
    @FXML private Label        usernameError;

    @FXML private TextField    emailField;
    @FXML private Label        emailError;

    @FXML private TextField    phoneField;
    @FXML private Label        phoneError;

    @FXML private PasswordField passwordField;
    @FXML private ProgressBar   strengthBar;
    @FXML private Label         strengthLabel;
    @FXML private Label         passwordError;

    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         confirmError;

    @FXML private VBox     sellerTermsSection;
    @FXML private CheckBox termsMerchandiseCheck;
    @FXML private Label    termsMerchandiseError;
    @FXML private CheckBox termsContentCheck;
    @FXML private Label    termsContentError;
    @FXML private CheckBox termsPrivacyCheck;
    @FXML private Label    termsPrivacyError;

    @FXML private Button registerButton;

    // ── Callbacks ─────────────────────────────────────────────────────────────

    private Runnable onRegisterSuccess = () -> {};
    private Runnable onBackToLogin     = () -> {};

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        buyerToggle.setSelected(true);

        roleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) { oldToggle.setSelected(true); return; }
            boolean isSeller = (newToggle == sellerToggle);
            showSellerTerms(isSeller);
            if (!isSeller) clearAllTermsErrors();
        });

        passwordField.textProperty().addListener((obs, o, n) -> {
            updateStrengthBar(n);
            if (!n.isEmpty()) clearFieldError(passwordField, passwordError);
        });

        confirmPasswordField.textProperty().addListener((obs, o, n) -> {
            if (!n.isEmpty()) {
                if (!n.equals(passwordField.getText()))
                    setFieldError(confirmPasswordField, confirmError, "Mật khẩu không khớp.");
                else
                    clearFieldError(confirmPasswordField, confirmError);
            } else {
                clearFieldError(confirmPasswordField, confirmError);
            }
        });

        termsMerchandiseCheck.selectedProperty().addListener((obs, o, c) -> { if (c) hideLabel(termsMerchandiseError); });
        termsContentCheck.selectedProperty().addListener((obs, o, c)     -> { if (c) hideLabel(termsContentError); });
        termsPrivacyCheck.selectedProperty().addListener((obs, o, c)     -> { if (c) hideLabel(termsPrivacyError); });

        Platform.runLater(() -> { if (fullNameField != null) fullNameField.requestFocus(); });
    }

    // ── Configuration ─────────────────────────────────────────────────────────

    /**
     * Coordinator gọi method này SAU KHI nạp FXML, trước khi hiện cửa sổ.
     *
     * @param onSuccess    callback khi đăng ký thành công
     * @param onBackToLogin callback khi user bấm "SIGN IN"
     */
    public void configure(Runnable onSuccess, Runnable onBackToLogin) {
        this.onRegisterSuccess = onSuccess     != null ? onSuccess     : () -> {};
        this.onBackToLogin     = onBackToLogin != null ? onBackToLogin : () -> {};
    }

    // ── Event Handlers ────────────────────────────────────────────────────────

    @FXML
    private void onRegister(ActionEvent event) {
        if (!validateAllFields()) return;

        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String phone    = phoneField.getText().trim();
        String password = passwordField.getText();
        UserRole role   = sellerToggle.isSelected() ? UserRole.SELLER : UserRole.BUYER;

        UserDTO dto = new UserDTO();
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setFullName(fullName);
        dto.setPhone(phone);
        dto.setRole(role);
        // Convention: password gửi trong field phone của UserDTO khi type = REGISTER
        // Nhưng RegisterController dùng UserDTO đặc biệt: password trong field "phone"
        // chỉ khi phone thực sự rỗng. Nếu user điền phone, cần 1 field riêng.
        // Giải pháp tạm: đóng gói phone + password vào 1 DTO mở rộng.
        // => Dùng UserDTO.setPhone(password) khi phone rỗng, hoặc dùng convention:
        //    server đọc password từ field phone nếu có prefix "pwd:"
        //
        // Đơn giản nhất: thêm field password vào UserDTO nếu cần.
        // Tạm thời dùng convention: phone field = phone#password
        String phoneAndPwd = phone.isEmpty()
                ? password
                : phone + "#PWD#" + password;
        dto.setPhone(phoneAndPwd);

        if (registerButton != null) registerButton.setDisable(true);

        Thread t = new Thread(() -> {
            try {
                ServerConnection.register(dto);
                Platform.runLater(() -> {
                    if (registerButton != null) registerButton.setDisable(false);
                    AlertHelper.showInfo("Đăng ký thành công!",
                            "Tài khoản \"" + username + "\" đã được tạo.\n" +
                                    "Vui lòng đăng nhập để tiếp tục.");
                    onRegisterSuccess.run();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (registerButton != null) registerButton.setDisable(false);
                    String msg = ex.getMessage() != null ? ex.getMessage() : "Đăng ký thất bại.";
                    // Phân loại lỗi trùng username/email
                    if (msg.toLowerCase().contains("username") || msg.toLowerCase().contains("tên đăng nhập")) {
                        setFieldError(usernameField, usernameError, msg);
                    } else if (msg.toLowerCase().contains("email")) {
                        setFieldError(emailField, emailError, msg);
                    } else {
                        AlertHelper.showError("Đăng ký thất bại", msg);
                    }
                });
            }
        }, "register-request-thread");
        t.setDaemon(true);
        t.start();
    }

    @FXML private void onBackToLogin(ActionEvent event) { onBackToLogin.run(); }

    @FXML
    private void onViewMerchandiseTerms(ActionEvent event) {
        showTermsDialog("Chính sách về Hàng hóa",
                """
                1. HÀNG HÓA ĐƯỢC PHÉP
                Chỉ đăng bán hàng hợp pháp, có nguồn gốc rõ ràng.

                2. HÀNG HÓA BỊ CẤM
                Hàng giả, vũ khí, chất độc, động vật hoang dã, tài liệu giả.

                3. TRÁCH NHIỆM
                Người bán chịu trách nhiệm về tính xác thực và nguồn gốc hàng hóa.

                4. XỬ LÝ VI PHẠM
                Tài khoản vi phạm bị khóa vĩnh viễn và có thể bị truy cứu pháp lý.
                """);
    }

    @FXML
    private void onViewContentTerms(ActionEvent event) {
        showTermsDialog("Chính sách về Nội dung",
                """
                1. MÔ TẢ SẢN PHẨM
                Phải trung thực, chính xác. Không cố ý gây hiểu nhầm.

                2. HÌNH ẢNH
                Hình ảnh thực tế, không vi phạm bản quyền, không chỉnh sửa che giấu lỗi.

                3. NỘI DUNG BỊ CẤM
                Nội dung khiêu dâm, bạo lực, xúc phạm, quảng cáo bất hợp pháp.

                4. NGÔN NGỮ
                Lịch sự, không thô tục.
                """);
    }

    @FXML
    private void onViewPrivacyTerms(ActionEvent event) {
        showTermsDialog("Chính sách Bảo mật",
                """
                1. THÔNG TIN THU THẬP
                Họ tên, email, số điện thoại, lịch sử giao dịch.

                2. MỤC ĐÍCH
                Xác thực danh tính, xử lý giao dịch, hỗ trợ người dùng.

                3. BẢO VỆ
                Mật khẩu được mã hóa BCrypt. Không bán thông tin cho bên thứ ba.

                4. QUYỀN CỦA BẠN
                Có thể yêu cầu xem, sửa, xóa dữ liệu cá nhân.
                """);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validateAllFields() {
        boolean valid = true;

        String username = usernameField.getText().trim();
        if (username.length() < 3) {
            setFieldError(usernameField, usernameError, "Tối thiểu 3 ký tự."); valid = false;
        } else if (!username.matches("[a-zA-Z0-9_]+")) {
            setFieldError(usernameField, usernameError, "Chỉ dùng chữ, số và dấu _"); valid = false;
        } else clearFieldError(usernameField, usernameError);

        String email = emailField.getText().trim();
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[a-z]{2,}$")) {
            setFieldError(emailField, emailError, "Email không hợp lệ."); valid = false;
        } else clearFieldError(emailField, emailError);

        String phone = phoneField.getText().trim();
        if (!phone.isEmpty() && !phone.matches("^[0-9+\\-\\s()]{7,15}$")) {
            setFieldError(phoneField, phoneError, "Số điện thoại không hợp lệ."); valid = false;
        } else clearFieldError(phoneField, phoneError);

        String password = passwordField.getText();
        if (!isStrongPassword(password)) {
            setFieldError(passwordField, passwordError,
                    "Tối thiểu 8 ký tự, gồm chữ hoa, chữ thường và số."); valid = false;
        } else clearFieldError(passwordField, passwordError);

        String confirm = confirmPasswordField.getText();
        if (!confirm.equals(password)) {
            setFieldError(confirmPasswordField, confirmError, "Mật khẩu không khớp."); valid = false;
        } else clearFieldError(confirmPasswordField, confirmError);

        if (sellerToggle.isSelected()) {
            valid = validateTerms() && valid;
        }
        return valid;
    }

    private boolean validateTerms() {
        boolean ok = true;
        if (!termsMerchandiseCheck.isSelected()) { showLabel(termsMerchandiseError); ok = false; }
        if (!termsContentCheck.isSelected())     { showLabel(termsContentError);     ok = false; }
        if (!termsPrivacyCheck.isSelected())     { showLabel(termsPrivacyError);     ok = false; }
        return ok;
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    private void showSellerTerms(boolean show) {
        sellerTermsSection.setVisible(show);
        sellerTermsSection.setManaged(show);
    }

    private void clearAllTermsErrors() {
        hideLabel(termsMerchandiseError);
        hideLabel(termsContentError);
        hideLabel(termsPrivacyError);
    }

    private void setFieldError(TextField field, Label lbl, String msg) {
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
        if (!field.getStyleClass().contains("input-error")) field.getStyleClass().add("input-error");
    }

    private void clearFieldError(TextField field, Label lbl) {
        lbl.setVisible(false); lbl.setManaged(false); lbl.setText("");
        field.getStyleClass().remove("input-error");
    }

    private void showLabel(Label l) { l.setVisible(true);  l.setManaged(true);  }
    private void hideLabel(Label l) { l.setVisible(false); l.setManaged(false); }

    private void showTermsDialog(String title, String content) {
        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(500, 360);
        textArea.setStyle(
                "-fx-control-inner-background: #0d1a30;" +
                        "-fx-text-fill: #c5cedc;" +
                        "-fx-font-size: 12.5px;" +
                        "-fx-border-color: rgba(241,196,93,0.20);" +
                        "-fx-border-radius: 8; -fx-background-radius: 8;");
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().add(
                new ButtonType("Đã hiểu", ButtonBar.ButtonData.OK_DONE));
        dialog.getDialogPane().setStyle(
                "-fx-background-color: #0d1a30; -fx-border-color: rgba(241,196,93,0.25);");
        dialog.showAndWait();
    }

    private void updateStrengthBar(String password) {
        if (strengthBar == null) return;
        int score = 0;
        if (password.length() >= 8)               score++;
        if (password.matches(".*[A-Z].*"))         score++;
        if (password.matches(".*[a-z].*"))         score++;
        if (password.matches(".*[0-9].*"))         score++;
        if (password.matches(".*[^A-Za-z0-9].*")) score++;
        strengthBar.setProgress((double) score / 5);
        if (strengthLabel != null) {
            strengthLabel.setText(switch (score) {
                case 0, 1 -> "Rất yếu";
                case 2    -> "Yếu";
                case 3    -> "Trung bình";
                case 4    -> "Mạnh";
                default   -> "Rất mạnh";
            });
        }
    }

    private boolean isStrongPassword(String p) {
        return p.length() >= 8
                && p.matches(".*[A-Z].*")
                && p.matches(".*[a-z].*")
                && p.matches(".*[0-9].*");
    }
}
