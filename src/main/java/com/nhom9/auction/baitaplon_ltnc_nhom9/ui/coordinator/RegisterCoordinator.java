package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator;

import com.nhom9.auction.baitaplon_ltnc_nhom9.HelloApplication;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth.AuthService;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller.RegisterController;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinator cho màn hình đăng ký.
 *
 * <h3>Coordinator là gì?</h3>
 * Trong kiến trúc MVC + Coordinator pattern, Coordinator đóng vai trò
 * <em>điều phối luồng điều hướng (navigation flow)</em> giữa các màn hình.
 * Controller KHÔNG tự mở/đóng cửa sổ khác — nó chỉ gọi callback.
 * Coordinator nhận callback đó và quyết định phải làm gì tiếp theo.
 *
 * <h3>RegisterCoordinator xử lý:</h3>
 * <ul>
 *   <li>Mở cửa sổ đăng ký ({@link #openRegisterWindow()})</li>
 *   <li>Khi đăng ký thành công → hiện thông báo, đóng Register, mở Login</li>
 *   <li>Khi user bấm "SIGN IN" → đóng Register, mở Login</li>
 * </ul>
 *
 * <h3>Cách sử dụng (từ HomeController hoặc LoginController):</h3>
 * <pre>{@code
 * RegisterCoordinator registerCoord = new RegisterCoordinator(
 *     primaryStage,       // owner window
 *     authService,        // AuthService dùng chung với LoginCoordinator
 *     loginCoordinator    // để mở login sau khi đăng ký xong
 * );
 * registerCoord.openRegisterWindow();
 * }</pre>
 */
public final class RegisterCoordinator {

    private static final Logger LOG = Logger.getLogger(RegisterCoordinator.class.getName());

    // ── Dependencies ──────────────────────────────────────────────────────────

    /** Cửa sổ cha — Register window sẽ là modal của cửa sổ này. */
    private final Window owner;

    /** AuthService dùng chung, được inject từ ngoài vào. */
    private final AuthService authService;

    /**
     * Coordinator của màn Login. RegisterCoordinator sẽ gọi nó sau khi
     * đăng ký thành công hoặc khi user muốn quay về Login.
     *
     * Có thể là null nếu app dùng luồng khác (vd: standalone mode).
     */
    private final HomeLoginCoordinator loginCoordinator;

    // ── Callbacks tùy chỉnh (optional) ───────────────────────────────────────

    /**
     * Callback tùy chỉnh sau khi đăng ký thành công.
     * Nếu không set, mặc định sẽ mở cửa sổ Login.
     */
    private Consumer<User> onRegisterSuccess;

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * Constructor đầy đủ — dùng khi tích hợp vào app có sẵn LoginCoordinator.
     *
     * @param owner            cửa sổ cha (Stage/Window)
     * @param authService      service xác thực dùng chung
     * @param loginCoordinator để mở Login sau khi register xong
     */
    public RegisterCoordinator(Window owner,
                               AuthService authService,
                               HomeLoginCoordinator loginCoordinator) {
        this.owner            = Objects.requireNonNull(owner, "owner không được null");
        this.authService      = Objects.requireNonNull(authService, "authService không được null");
        this.loginCoordinator = loginCoordinator; // có thể null
    }

    /**
     * Constructor tối giản — khi không có LoginCoordinator sẵn.
     * Trong trường hợp này, sau khi đăng ký thành công, app chỉ đóng cửa sổ
     * (không tự mở Login). Bạn cần set callback thủ công qua {@link #setOnRegisterSuccess}.
     *
     * @param owner       cửa sổ cha
     * @param authService service xác thực
     */
    public RegisterCoordinator(Window owner, AuthService authService) {
        this(owner, authService, null);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Đặt callback tùy chỉnh khi đăng ký thành công.
     * Nếu không set, mặc định coordinator sẽ mở Login window.
     *
     * @param callback nhận User vừa đăng ký
     */
    public void setOnRegisterSuccess(Consumer<User> callback) {
        this.onRegisterSuccess = callback;
    }

    /**
     * Mở cửa sổ đăng ký dạng modal.
     *
     * <p>Luồng xử lý:
     * <ol>
     *   <li>Load RegisterView.fxml</li>
     *   <li>Lấy RegisterController và gọi {@code configure()} để inject dependencies</li>
     *   <li>Tạo Stage và hiện lên với {@code showAndWait()}</li>
     * </ol>
     * </p>
     */
    public void openRegisterWindow() {
        try {
            // ── Bước 1: Nạp FXML ──
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/RegisterView.fxml"));
            Parent root = loader.load();

            // ── Bước 2: Lấy controller và inject dependencies ──
            RegisterController controller = loader.getController();

            Stage registerStage = new Stage();

            controller.configure(
                    authService,
                    // Callback khi đăng ký thành công
                    user -> handleRegisterSuccess(user, registerStage),
                    // Callback khi user bấm "SIGN IN"
                    () -> handleBackToLogin(registerStage)
            );

            // ── Bước 3: Tạo Scene và Stage ──
            Scene scene = new Scene(root);
            // Nạp stylesheet bổ sung (style.css đã được khai báo trong FXML,
            // nhưng cũng load ở đây để đảm bảo nếu cần override theo stage)
            // scene.getStylesheets() đã được xử lý bởi FXML stylesheets attribute

            registerStage.setTitle("Đăng ký — UBid");
            registerStage.initOwner(owner);
            registerStage.initModality(Modality.WINDOW_MODAL);

            // Kích thước phù hợp với form có nhiều field hơn Login
            registerStage.setMinWidth(720);
            registerStage.setMinHeight(600);
            registerStage.setWidth(900);
            registerStage.setHeight(700);

            registerStage.setScene(scene);
            registerStage.showAndWait();

        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Không thể tải RegisterView.fxml", ex);
            AlertHelper.showError("Lỗi hệ thống",
                    "Không thể mở màn hình đăng ký. Vui lòng thử lại.");
        }
    }

    // ── Private Handlers ──────────────────────────────────────────────────────

    /**
     * Xử lý khi đăng ký thành công.
     *
     * Luồng mặc định:
     * 1. Đóng cửa sổ Register
     * 2. Hiện thông báo thành công
     * 3. Mở Login để user đăng nhập với tài khoản vừa tạo
     *    (hoặc gọi custom callback nếu đã set)
     *
     * @param newUser       User vừa được tạo
     * @param registerStage Stage của Register window (để đóng)
     */
    private void handleRegisterSuccess(User newUser, Stage registerStage) {
        registerStage.close();
        LOG.info("Đăng ký thành công: " + newUser.getUsername()
                + " [" + newUser.getRole() + "]");

        if (onRegisterSuccess != null) {
            // Dùng custom callback nếu đã được set
            onRegisterSuccess.accept(newUser);
        } else {
            // Luồng mặc định: thông báo thành công → mở Login
            AlertHelper.showSuccess(
                    "Tài khoản '" + newUser.getUsername() + "' đã được tạo thành công!\n"
                            + "Vui lòng đăng nhập để tiếp tục.");

            openLoginAfterRegister();
        }
    }

    /**
     * Xử lý khi user bấm "SIGN IN" trên màn Register (muốn quay về Login).
     *
     * @param registerStage Stage của Register window (để đóng)
     */
    private void handleBackToLogin(Stage registerStage) {
        registerStage.close();
        LOG.info("User quay về màn Login từ Register.");
        openLoginAfterRegister();
    }

    /**
     * Mở cửa sổ Login sau khi đóng Register.
     *
     * Nếu có LoginCoordinator → dùng nó (tái sử dụng AuthService và logic cũ).
     * Nếu không → log cảnh báo (coordinator chưa được cấu hình đầy đủ).
     */
    private void openLoginAfterRegister() {
        if (loginCoordinator != null) {
            loginCoordinator.openLoginWindow();
        } else {
            LOG.warning("LoginCoordinator chưa được set — không thể mở cửa sổ Login tự động. "
                    + "Hãy set loginCoordinator hoặc dùng setOnRegisterSuccess() để xử lý thủ công.");
        }
    }
}