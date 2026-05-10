package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.DuplicateUserException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth.AuthService;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Controller cho màn hình đăng ký (RegisterView.fxml).
 *
 * <h3>Trách nhiệm của controller này:</h3>
 * <ul>
 *   <li>Đọc dữ liệu từ các trường nhập liệu (username, email, password, …)</li>
 *   <li>Validate ở phía client (inline error labels, không dùng Alert cho lỗi field)</li>
 *   <li>Gọi {@link AuthService#register} với dữ liệu hợp lệ</li>
 *   <li>Thông báo kết quả cho {@link com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator.RegisterCoordinator}
 *       thông qua callback</li>
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
    @FXML private ToggleGroup  roleGroup;   // inject từ <fx:define> trong FXML

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

    // ── Dependencies & Callbacks ──────────────────────────────────────────────

    /** AuthService được inject từ Coordinator — không tự khởi tạo trong controller. */
    private AuthService authService;

    /**
     * Gọi khi đăng ký thành công. Coordinator dùng callback này để đóng cửa sổ
     * và chuyển về màn Login.
     */
    private Consumer<User> onRegisterSuccess = u -> {};

    /**
     * Gọi khi user bấm "SIGN IN" để quay về màn Login.
     * Coordinator sẽ xử lý việc mở cửa sổ Login.
     */
    private Runnable onBackToLogin = () -> {};

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * JavaFX tự gọi sau khi FXML được nạp xong.
     * Đây là nơi setup ban đầu: listener, giá trị mặc định, v.v.
     *
     * <p><b>Tại sao dùng Platform.runLater?</b>
     * Khi initialize() chạy, cửa sổ chưa hiện ra. requestFocus() cần gọi SAU
     * khi scene đã render, nên ta đẩy nó vào hàng đợi JavaFX Application Thread.</p>
     */
    @FXML
    private void initialize() {
        // Chọn BUYER mặc định khi mở form
        buyerToggle.setSelected(true);

        // Đảm bảo luôn có 1 toggle được chọn (ToggleGroup mặc định cho phép bỏ chọn hết)
        roleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                // Nếu user cố bỏ chọn hết → reselect cái cũ
                oldToggle.setSelected(true);
            }
        });

        // Real-time strength bar: cập nhật khi user gõ password
        passwordField.textProperty().addListener((obs, oldText, newText) -> {
            updateStrengthBar(newText);
            // Nếu đang hiển thị lỗi password, xóa đi khi user gõ lại
            if (!newText.isEmpty()) clearFieldError(passwordField, passwordError);
        });

        // Real-time confirm check: cập nhật khi user gõ vào confirm field
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

        // Focus vào trường đầu tiên khi cửa sổ hiện
        Platform.runLater(() -> {
            if (fullNameField != null) fullNameField.requestFocus();
        });
    }

    // ── Configuration (gọi bởi RegisterCoordinator) ───────────────────────────

    /**
     * Coordinator gọi method này SAU KHI nạp FXML, trước khi hiện cửa sổ.
     *
     * <p><b>Tại sao pattern này?</b>
     * Constructor của controller do JavaFX tự gọi khi load FXML — ta không thể
     * truyền dependency vào đó. Thay vào đó, ta dùng setter method sau khi có
     * controller reference từ {@code FXMLLoader.getController()}.</p>
     *
     * @param authService     service xử lý logic đăng ký
     * @param onSuccess       callback khi đăng ký thành công (nhận User vừa tạo)
     * @param onBackToLogin   callback khi user muốn quay về màn Login
     */
    public void configure(AuthService authService,
                          Consumer<User> onSuccess,
                          Runnable onBackToLogin) {
        this.authService       = Objects.requireNonNull(authService, "authService không được null");
        this.onRegisterSuccess = onSuccess    != null ? onSuccess    : u -> {};
        this.onBackToLogin     = onBackToLogin != null ? onBackToLogin : () -> {};
    }

    // ── Event Handlers (gọi bởi FXML qua onAction="...") ────────────────────

    /**
     * Xử lý khi user bấm "CREATE ACCOUNT".
     *
     * Luồng xử lý:
     * 1. Validate tất cả trường → nếu có lỗi, hiện inline error và dừng lại
     * 2. Gọi authService.register() với dữ liệu đã trim
     * 3. Nếu thành công → gọi onRegisterSuccess callback
     * 4. Nếu lỗi trùng username/email → hiện error vào đúng field
     * 5. Nếu lỗi khác → hiện AlertHelper.showError()
     */
    @FXML
    private void onRegister(ActionEvent event) {
        if (authService == null) {
            AlertHelper.showError("Lỗi hệ thống", "AuthService chưa được khởi tạo.");
            return;
        }

        // Bước 1: Validate tất cả trường
        boolean valid = validateAllFields();
        if (!valid) return; // Dừng lại, inline errors đã hiển thị

        // Bước 2: Thu thập dữ liệu
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String phone    = phoneField.getText().trim();
        String password = passwordField.getText();
        String role     = getSelectedRole();

        // Bước 3: Gọi service
        try {
            User newUser = authService.register(username, email, password, fullName, phone, role);
            // Thành công: thông báo coordinator xử lý tiếp (đóng cửa sổ, mở login, v.v.)
            onRegisterSuccess.accept(newUser);

        } catch (DuplicateUserException ex) {
            // Lỗi trùng lặp → highlight đúng field
            handleDuplicateError(ex);

        } catch (IllegalArgumentException ex) {
            // Lỗi validate từ service (vd: password quá yếu theo server-side check)
            AlertHelper.showError("Dữ liệu không hợp lệ", ex.getMessage());

        } catch (Exception ex) {
            AlertHelper.showError("Lỗi hệ thống", "Đăng ký thất bại: " + ex.getMessage());
        }
    }

    /**
     * Xử lý khi user bấm "SIGN IN" để quay về Login.
     */
    @FXML
    private void onBackToLogin(ActionEvent event) {
        onBackToLogin.run();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Validate toàn bộ form. Hiển thị lỗi inline cho từng trường.
     *
     * <p><b>Tại sao validate ở controller lẫn service?</b>
     * Client-side validation (ở đây) cho UX tốt — phản hồi ngay lập tức mà
     * không cần gọi network/DB. Server-side validation (trong AuthService) là
     * tầng bảo vệ cuối cùng — không thể bỏ qua vì controller có thể bị bypass.</p>
     *
     * @return true nếu tất cả trường hợp lệ
     */
    private boolean validateAllFields() {
        boolean valid = true;

        // ── Full Name (optional nhưng nếu nhập thì không rỗng hoàn toàn) ──
        // Full name là optional, bỏ qua validation nếu muốn

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

        // ── Phone (optional — chỉ validate format nếu không rỗng) ──
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

        return valid;
    }

    /**
     * Xử lý lỗi trùng username hoặc email từ service.
     * Highlight đúng trường bị trùng thay vì hiện Alert chung.
     */
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
     * Hiện error label và đánh dấu input field bị lỗi (viền đỏ).
     *
     * <p><b>Tại sao dùng managed=true/false?</b>
     * Trong JavaFX, {@code visible=false} chỉ ẩn node nhưng vẫn chiếm chỗ
     * trong layout. {@code managed=false} khiến layout bỏ qua node đó hoàn toàn
     * (tương đương {@code display: none} trong CSS). Khi hiện lại, ta phải set
     * cả hai về true.</p>
     *
     * @param field      TextField/PasswordField cần đánh dấu lỗi
     * @param errorLabel Label hiển thị thông báo lỗi
     * @param message    Nội dung lỗi
     */
    private void setFieldError(TextField field, Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        if (!field.getStyleClass().contains("input-error")) {
            field.getStyleClass().add("input-error");
        }
    }

    /**
     * Xóa trạng thái lỗi của field và ẩn error label.
     */
    private void clearFieldError(TextField field, Label errorLabel) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setText("");
        field.getStyleClass().remove("input-error");
    }

    /**
     * Cập nhật thanh ProgressBar độ mạnh mật khẩu theo thời gian thực.
     *
     * Độ mạnh được tính dựa trên:
     * - Độ dài >= 8
     * - Có chữ hoa
     * - Có chữ thường
     * - Có số
     * - Có ký tự đặc biệt (bonus)
     *
     * @param password Mật khẩu hiện tại (chưa được validate hoàn toàn)
     */
    private void updateStrengthBar(String password) {
        if (password == null || password.isEmpty()) {
            strengthBar.setProgress(0);
            strengthBar.getStyleClass().removeAll("weak", "medium", "strong");
            strengthLabel.setVisible(false);
            strengthLabel.setManaged(false);
            return;
        }

        int score = 0;
        if (password.length() >= 8)                         score++;
        if (password.matches(".*[A-Z].*"))                  score++;
        if (password.matches(".*[a-z].*"))                  score++;
        if (password.matches(".*\\d.*"))                    score++;
        if (password.matches(".*[^a-zA-Z0-9].*"))           score++; // special char bonus

        // Phân loại: yếu (1-2), trung bình (3), mạnh (4-5)
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

    /**
     * Kiểm tra password có đủ mạnh không.
     * Logic này phải KHỚP với {@code PasswordHasher.isStrong()} trong AuthService
     * để tránh trường hợp client validate pass nhưng server reject.
     *
     * <p>Rule: >= 8 ký tự, có chữ hoa, chữ thường, và số.</p>
     */
    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        return password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*")
                && password.matches(".*\\d.*");
    }

    /**
     * Lấy role đang được chọn dưới dạng String để truyền vào AuthService.
     * AuthService sẽ parse bằng {@code UserRole.fromString()}.
     *
     * @return "BUYER" hoặc "SELLER"
     */
    private String getSelectedRole() {
        // buyerToggle mặc định được chọn trong initialize(),
        // và roleGroup listener đảm bảo luôn có 1 toggle được chọn.
        return sellerToggle.isSelected() ? "SELLER" : "BUYER";
    }
}