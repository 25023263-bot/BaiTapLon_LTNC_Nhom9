package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.DuplicateUserException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth.AuthService;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Controller cho màn hình đăng ký (RegisterView.fxml).
 *
 * <h3>Trách nhiệm của controller này:</h3>
 * <ul>
 *   <li>Đọc dữ liệu từ các trường nhập liệu</li>
 *   <li>Validate ở phía client (inline error labels)</li>
 *   <li>Hiện/ẩn phần điều khoản khi user chuyển vai trò BUYER ↔ SELLER</li>
 *   <li>Mở Dialog xem nội dung từng điều khoản khi user bấm "Xem"</li>
 *   <li>Gọi {@link AuthService#register} với dữ liệu hợp lệ</li>
 *   <li>Thông báo kết quả cho Coordinator thông qua callback</li>
 * </ul>
 *
 * <h3>Điều controller KHÔNG làm:</h3>
 * <ul>
 *   <li>Mở/đóng cửa sổ — đó là việc của Coordinator</li>
 *   <li>Chứa business logic — AuthService lo việc đó</li>
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

    // ── FXML Fields — Điều khoản Người Bán (MỚI) ─────────────────────────────

    /**
     * VBox bao toàn bộ phần điều khoản.
     * visible=false, managed=false khi user chọn BUYER (không chiếm chỗ trong layout).
     * visible=true, managed=true khi user chọn SELLER.
     *
     * Tại sao cần managed=false chứ không chỉ visible=false?
     * → Trong JavaFX, visible=false ẩn node nhưng vẫn giữ chỗ trong layout
     *   (giống opacity: 0 trong CSS). managed=false khiến layout bỏ qua
     *   node hoàn toàn — tương đương display: none trong CSS.
     */
    @FXML private VBox     sellerTermsSection;

    @FXML private CheckBox termsMerchandiseCheck;
    @FXML private Label    termsMerchandiseError;

    @FXML private CheckBox termsContentCheck;
    @FXML private Label    termsContentError;

    @FXML private CheckBox termsPrivacyCheck;
    @FXML private Label    termsPrivacyError;

    // ── Dependencies & Callbacks ──────────────────────────────────────────────

    private AuthService authService;
    private Consumer<User> onRegisterSuccess = u -> {};
    private Runnable onBackToLogin = () -> {};

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        // Chọn BUYER mặc định
        buyerToggle.setSelected(true);

        // Đảm bảo luôn có 1 toggle được chọn
        roleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);
                return;
            }
            // Khi chuyển vai trò → hiện/ẩn phần điều khoản
            boolean isSeller = (newToggle == sellerToggle);
            showSellerTerms(isSeller);

            // Nếu chuyển từ SELLER về BUYER → xóa các lỗi điều khoản
            if (!isSeller) clearAllTermsErrors();
        });

        // Real-time strength bar
        passwordField.textProperty().addListener((obs, oldText, newText) -> {
            updateStrengthBar(newText);
            if (!newText.isEmpty()) clearFieldError(passwordField, passwordError);
        });

        // Real-time confirm check
        confirmPasswordField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.isEmpty()) {
                if (!newText.equals(passwordField.getText())) {
                    setFieldError(confirmPasswordField, confirmError, "Mật khẩu không khớp.");
                } else {
                    clearFieldError(confirmPasswordField, confirmError);
                }
            } else {
                clearFieldError(confirmPasswordField, confirmError);
            }
        });

        // Xóa lỗi điều khoản khi user tích checkbox
        // Tại sao làm vậy? UX tốt hơn — lỗi biến mất ngay khi người dùng thực hiện hành động đúng
        termsMerchandiseCheck.selectedProperty().addListener((obs, o, checked) -> {
            if (checked) hideLabel(termsMerchandiseError);
        });
        termsContentCheck.selectedProperty().addListener((obs, o, checked) -> {
            if (checked) hideLabel(termsContentError);
        });
        termsPrivacyCheck.selectedProperty().addListener((obs, o, checked) -> {
            if (checked) hideLabel(termsPrivacyError);
        });

        Platform.runLater(() -> {
            if (fullNameField != null) fullNameField.requestFocus();
        });
    }

    // ── Configuration ─────────────────────────────────────────────────────────

    /**
     * Coordinator gọi method này SAU KHI nạp FXML, trước khi hiện cửa sổ.
     */
    public void configure(AuthService authService,
                          Consumer<User> onSuccess,
                          Runnable onBackToLogin) {
        this.authService       = Objects.requireNonNull(authService, "authService không được null");
        this.onRegisterSuccess = onSuccess     != null ? onSuccess     : u -> {};
        this.onBackToLogin     = onBackToLogin != null ? onBackToLogin : () -> {};
    }

    // ── Event Handlers ────────────────────────────────────────────────────────

    /**
     * Xử lý khi user bấm "TẠO TÀI KHOẢN".
     *
     * Luồng:
     * 1. Validate tất cả fields (gồm cả điều khoản nếu là SELLER)
     * 2. Gọi authService.register()
     * 3. Callback thành công hoặc xử lý lỗi
     */
    @FXML
    private void onRegister(ActionEvent event) {
        if (authService == null) {
            AlertHelper.showError("Lỗi hệ thống", "AuthService chưa được khởi tạo.");
            return;
        }

        boolean valid = validateAllFields();
        if (!valid) return;

        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String phone    = phoneField.getText().trim();
        String password = passwordField.getText();
        String role     = getSelectedRole();

        try {
            User newUser = authService.register(username, email, password, fullName, phone, role);
            onRegisterSuccess.accept(newUser);

        } catch (DuplicateUserException ex) {
            handleDuplicateError(ex);

        } catch (IllegalArgumentException ex) {
            AlertHelper.showError("Dữ liệu không hợp lệ", ex.getMessage());

        } catch (Exception ex) {
            AlertHelper.showError("Lỗi hệ thống", "Đăng ký thất bại: " + ex.getMessage());
        }
    }

    /** Xử lý khi bấm nút "Xem" bên cạnh Chính sách Hàng hóa */
    @FXML
    private void onViewMerchandiseTerms(ActionEvent event) {
        showTermsDialog(
                "Chính sách về Hàng hóa",
                """
                1. HÀNG HÓA ĐƯỢC PHÉP ĐĂNG BÁN
                Người bán được phép đăng bán các mặt hàng hợp pháp, có nguồn gốc xuất xứ rõ ràng và không vi phạm pháp luật Việt Nam.
    
                2. HÀNG HÓA BỊ CẤM
                Các mặt hàng sau đây bị nghiêm cấm đăng bán:
                • Hàng giả, hàng nhái, hàng vi phạm sở hữu trí tuệ
                • Vũ khí, đạn dược, chất nổ, chất độc hại
                • Động vật hoang dã, mẫu vật được bảo vệ
                • Tài liệu giả mạo, thông tin sai lệch
                • Hàng hóa bị cấm theo quy định của pháp luật
    
                3. TIÊU CHUẨN CHẤT LƯỢNG
                Người bán có trách nhiệm đảm bảo hàng hóa đúng với mô tả, ảnh chụp và tình trạng được công bố trong phiên đấu giá.
    
                4. TRÁCH NHIỆM VỀ NGUỒN GỐC
                Người bán phải chứng minh được quyền sở hữu hoặc quyền bán đối với hàng hóa đăng bán khi được yêu cầu.
    
                5. XỬ LÝ VI PHẠM
                Tài khoản vi phạm chính sách hàng hóa sẽ bị khóa vĩnh viễn và có thể bị truy cứu trách nhiệm pháp lý.
                """
        );
    }

    /** Xử lý khi bấm nút "Xem" bên cạnh Chính sách Nội dung */
    @FXML
    private void onViewContentTerms(ActionEvent event) {
        showTermsDialog(
                "Chính sách về Nội dung",
                """
                1. MÔ TẢ SẢN PHẨM
                Mô tả sản phẩm phải trung thực, chính xác và đầy đủ. Không được cố ý cung cấp thông tin sai lệch để đánh lừa người mua.
    
                2. HÌNH ẢNH SẢN PHẨM
                • Hình ảnh phải là ảnh thực tế của sản phẩm đang bán
                • Không sử dụng hình ảnh vi phạm bản quyền
                • Không chỉnh sửa ảnh để che giấu khuyết điểm của sản phẩm
                • Ảnh phải rõ ràng, đủ ánh sáng và đúng góc nhìn
    
                3. NỘI DUNG BỊ CẤM
                Nghiêm cấm đăng tải:
                • Nội dung khiêu dâm, bạo lực
                • Thông tin xúc phạm, phân biệt đối xử
                • Quảng cáo dịch vụ bất hợp pháp
                • Liên kết đến trang web độc hại
    
                4. NGÔN NGỮ SỬ DỤNG
                Nội dung đăng tải phải sử dụng ngôn ngữ lịch sự, không dùng từ ngữ thô tục hay xúc phạm.
    
                5. CẬP NHẬT THÔNG TIN
                Người bán có trách nhiệm cập nhật thông tin sản phẩm kịp thời khi có thay đổi về tình trạng, giá cả hoặc tính sẵn có.
                """
        );
    }

    /** Xử lý khi bấm nút "Xem" bên cạnh Chính sách Bảo mật */
    @FXML
    private void onViewPrivacyTerms(ActionEvent event) {
        showTermsDialog(
                "Chính sách Bảo mật Thông tin",
                """
                1. THÔNG TIN CHÚNG TÔI THU THẬP
                Khi bạn đăng ký tài khoản Người Bán, chúng tôi thu thập:
                • Thông tin định danh: họ tên
                • Thông tin liên hệ: email, số điện thoại
                • Thông tin tài khoản: tên đăng nhập (mật khẩu được mã hóa bằng BCrypt)
                • Lịch sử giao dịch và hoạt động trên nền tảng
    
                2. MỤC ĐÍCH SỬ DỤNG
                Thông tin được sử dụng để:
                • Xác thực danh tính và quản lý tài khoản
                • Xử lý các giao dịch đấu giá
                • Liên hệ hỗ trợ khi cần thiết
                • Cải thiện trải nghiệm người dùng
                • Tuân thủ các yêu cầu pháp lý
    
                3. BẢO VỆ THÔNG TIN
                • Mật khẩu được mã hóa, không ai có thể xem được kể cả quản trị viên
                • Dữ liệu được lưu trữ trên máy chủ bảo mật
                • Chúng tôi KHÔNG bán thông tin cá nhân cho bên thứ ba
                • Truy cập dữ liệu được giới hạn cho nhân viên có thẩm quyền
    
                4. QUYỀN CỦA BẠN
                Bạn có quyền:
                • Yêu cầu xem, sửa đổi thông tin cá nhân
                • Yêu cầu xóa tài khoản và dữ liệu liên quan
                • Khiếu nại nếu thông tin bị sử dụng sai mục đích
    
                5. LIÊN HỆ
                Mọi thắc mắc về bảo mật: privacy@ubid.vn
                """
        );
    }

    @FXML
    private void onBackToLogin(ActionEvent event) {
        onBackToLogin.run();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Validate toàn bộ form.
     *
     * Tại sao không return sớm khi gặp lỗi đầu tiên?
     * → Để hiển thị TẤT CẢ lỗi cùng lúc, giúp người dùng không phải
     *   submit nhiều lần mới thấy hết các lỗi.
     */
    private boolean validateAllFields() {
        boolean valid = true;

        // ── Username ──
        String username = usernameField.getText().trim();
        if (username.length() < 3) {
            setFieldError(usernameField, usernameError, "Tối thiểu 3 ký tự.");
            valid = false;
        } else if (!username.matches("[a-zA-Z0-9_]+")) {
            setFieldError(usernameField, usernameError, "Chỉ dùng chữ, số và dấu _");
            valid = false;
        } else {
            clearFieldError(usernameField, usernameError);
        }

        // ── Email ──
        String email = emailField.getText().trim();
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[a-z]{2,}$")) {
            setFieldError(emailField, emailError, "Email không hợp lệ.");
            valid = false;
        } else {
            clearFieldError(emailField, emailError);
        }

        // ── Phone (optional) ──
        String phone = phoneField.getText().trim();
        if (!phone.isEmpty() && !phone.matches("^[0-9+\\-\\s()]{7,15}$")) {
            setFieldError(phoneField, phoneError, "Số điện thoại không hợp lệ.");
            valid = false;
        } else {
            clearFieldError(phoneField, phoneError);
        }

        // ── Password ──
        String password = passwordField.getText();
        if (!isStrongPassword(password)) {
            setFieldError(passwordField, passwordError,
                    "Tối thiểu 8 ký tự, gồm chữ hoa, chữ thường và số.");
            valid = false;
        } else {
            clearFieldError(passwordField, passwordError);
        }

        // ── Confirm Password ──
        String confirm = confirmPasswordField.getText();
        if (!confirm.equals(password)) {
            setFieldError(confirmPasswordField, confirmError, "Mật khẩu không khớp.");
            valid = false;
        } else {
            clearFieldError(confirmPasswordField, confirmError);
        }

        // ── Điều khoản — chỉ validate khi user chọn SELLER ──
        //
        // Tại sao chỉ validate cho SELLER?
        // → Người mua không cần chấp nhận điều khoản bán hàng.
        //   Điều khoản này là cam kết đặc biệt của người có quyền đăng sản phẩm.
        if (sellerToggle.isSelected()) {
            valid = validateTerms() && valid;
            // Chú ý: gọi validateTerms() TRƯỚC khi && với valid
            // để đảm bảo validateTerms() luôn chạy (hiện lỗi) dù valid đã false
        }

        return valid;
    }

    /**
     * Validate 3 checkbox điều khoản.
     * Hiện error label riêng cho từng checkbox chưa tích.
     *
     * @return true nếu cả 3 đã được chấp nhận
     */
    private boolean validateTerms() {
        boolean allAccepted = true;

        if (!termsMerchandiseCheck.isSelected()) {
            showLabel(termsMerchandiseError);
            allAccepted = false;
        }
        if (!termsContentCheck.isSelected()) {
            showLabel(termsContentError);
            allAccepted = false;
        }
        if (!termsPrivacyCheck.isSelected()) {
            showLabel(termsPrivacyError);
            allAccepted = false;
        }

        return allAccepted;
    }

    private void handleDuplicateError(DuplicateUserException ex) {
        if (ex.getDuplicateField() == DuplicateUserException.Field.USERNAME) {
            setFieldError(usernameField, usernameError,
                    "Username '" + ex.getValue() + "' đã được sử dụng.");
        } else {
            setFieldError(emailField, emailError,
                    "Email '" + ex.getValue() + "' đã được đăng ký.");
        }
    }

    // ── UI Helpers ────────────────────────────────────────────────────────────

    /**
     * Hiện/ẩn phần điều khoản tùy theo vai trò được chọn.
     * managed=false → layout bỏ qua hoàn toàn, không để lại khoảng trắng.
     */
    private void showSellerTerms(boolean show) {
        sellerTermsSection.setVisible(show);
        sellerTermsSection.setManaged(show);
    }

    private void clearAllTermsErrors() {
        hideLabel(termsMerchandiseError);
        hideLabel(termsContentError);
        hideLabel(termsPrivacyError);
    }

    private void setFieldError(TextField field, Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        if (!field.getStyleClass().contains("input-error")) {
            field.getStyleClass().add("input-error");
        }
    }

    private void clearFieldError(TextField field, Label errorLabel) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setText("");
        field.getStyleClass().remove("input-error");
    }

    private void showLabel(Label label) {
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideLabel(Label label) {
        label.setVisible(false);
        label.setManaged(false);
    }

    // ── Hiển thị Dialog nội dung điều khoản ──────────────────────────────────

    /**
     * Hiện popup chứa nội dung điều khoản.
     *
     * Tại sao dùng Dialog thay vì màn hình riêng?
     * → Điều khoản là thông tin phụ trợ, không phải bước chính trong luồng.
     *   Dialog giữ user trong context đăng ký, tránh làm gián đoạn luồng UX.
     *   User đọc xong bấm "Đã hiểu" và tiếp tục tick checkbox.
     *
     * @param title   Tiêu đề dialog
     * @param content Nội dung điều khoản (text thuần)
     */
    private void showTermsDialog(String title, String content) {
        // TextArea cho phép scroll khi nội dung dài, không edit được
        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(500, 360);
        // Áp dụng màu sắc theo theme dark của app
        textArea.setStyle(
                "-fx-control-inner-background: #0d1a30;" +
                        "-fx-background-color: #0d1a30;" +
                        "-fx-text-fill: #c5cedc;" +
                        "-fx-font-size: 12.5px;" +
                        "-fx-border-color: rgba(241, 196, 93, 0.20);" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-family: 'Be Vietnam Pro', 'Noto Sans', Arial, sans-serif;"
        );

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(textArea);

        // Thêm nút "Đã hiểu" — ButtonData.OK_DONE để Enter trigger được
        ButtonType confirmBtn = new ButtonType("Đã hiểu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(confirmBtn);

        // Style nút theo theme gold của app
        dialog.getDialogPane().lookupButton(confirmBtn).setStyle(
                "-fx-background-color: #f1c45d;" +
                        "-fx-text-fill: #0a1424;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 9 22 9 22;" +
                        "-fx-background-radius: 20;" +
                        "-fx-cursor: hand;"
        );

        // Style dialog pane theo theme tối
        dialog.getDialogPane().setStyle(
                "-fx-background-color: #0a1424;" +
                        "-fx-border-color: rgba(241, 196, 93, 0.25);" +
                        "-fx-border-width: 1;"
        );

        dialog.showAndWait();
    }

    // ── Strength Bar ──────────────────────────────────────────────────────────

    private void updateStrengthBar(String password) {
        if (password == null || password.isEmpty()) {
            strengthBar.setProgress(0);
            strengthBar.getStyleClass().removeAll("weak", "medium", "strong");
            strengthLabel.setVisible(false);
            strengthLabel.setManaged(false);
            return;
        }

        int score = 0;
        if (password.length() >= 8)               score++;
        if (password.matches(".*[A-Z].*"))        score++;
        if (password.matches(".*[a-z].*"))        score++;
        if (password.matches(".*\\d.*"))          score++;
        if (password.matches(".*[^a-zA-Z0-9].*")) score++; // ký tự đặc biệt (bonus)

        strengthBar.getStyleClass().removeAll("weak", "medium", "strong");
        strengthLabel.setVisible(true);
        strengthLabel.setManaged(true);
        strengthLabel.getStyleClass().removeAll("weak", "medium", "strong");

        if (score <= 2) {
            strengthBar.setProgress(0.33);
            strengthBar.getStyleClass().add("weak");
            strengthLabel.setText("Weak");
            strengthLabel.getStyleClass().add("weak");
        } else if (score == 3) {
            strengthBar.setProgress(0.66);
            strengthBar.getStyleClass().add("medium");
            strengthLabel.setText("Medium");
            strengthLabel.getStyleClass().add("medium");
        } else {
            strengthBar.setProgress(1.0);
            strengthBar.getStyleClass().add("strong");
            strengthLabel.setText("Strong");
            strengthLabel.getStyleClass().add("strong");
        }
    }

    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        return password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*")
                && password.matches(".*\\d.*");
    }

    private String getSelectedRole() {
        return sellerToggle.isSelected() ? "SELLER" : "BUYER";
    }
}
