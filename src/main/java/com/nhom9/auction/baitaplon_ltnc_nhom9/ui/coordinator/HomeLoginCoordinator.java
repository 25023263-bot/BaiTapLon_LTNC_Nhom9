package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator;

import com.nhom9.auction.baitaplon_ltnc_nhom9.HelloApplication;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.UserRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth.AuthService;
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
 * <h3>Vai trò:</h3>
 * <ul>
 *   <li>Tạo và sở hữu {@link AuthService} (dùng chung cho cả Login và Register)</li>
 *   <li>Mở cửa sổ Login, nối các callback vào LoginController</li>
 *   <li>Khi user bấm "SIGN UP" trong Login → chuyển sang màn Register</li>
 *   <li>Khi Register xong → quay về Login để user đăng nhập</li>
 *   <li>Khi đăng nhập xong → thông báo Home refresh UI</li>
 * </ul>
 *
 * <h3>Luồng điều hướng đầy đủ:</h3>
 * <pre>
 *  HomeController
 *       │ gọi openLoginWindow()
 *       ▼
 *  [LoginView] ──── bấm "SIGN UP NOW" ────▶ [RegisterView]
 *       ▲                                         │
 *       │   đăng ký xong / bấm "SIGN IN"          │
 *       └─────────────────────────────────────────┘
 *       │ đăng nhập xong
 *       ▼
 *  HomeController.onAuthStateChanged() → refresh UI
 * </pre>
 *
 * <h3>Cách dùng từ HomeController:</h3>
 * <pre>{@code
 * // Khởi tạo 1 lần (thường trong initialize() của HomeController)
 * HomeLoginCoordinator loginCoord = new HomeLoginCoordinator(primaryStage.getScene().getWindow());
 * loginCoord.setOnAuthStateChanged(() -> refreshHomeUI());
 *
 * // Khi user bấm nút Login trên Home
 * loginCoord.openLoginWindow();
 *
 * // Khi user bấm nút Logout trên Home
 * loginCoord.performLogout();
 * }</pre>
 */
public final class HomeLoginCoordinator {

    private static final Logger LOG = Logger.getLogger(HomeLoginCoordinator.class.getName());

    // ── Fields ────────────────────────────────────────────────────────────────

    /** Cửa sổ cha — tất cả modal window sẽ init owner về đây. */
    private final Window owner;

    /**
     * AuthService dùng chung cho cả Login và Register.
     *
     * <p><b>Tại sao tạo ở đây?</b>
     * AuthService cần UserRepository. Coordinator là nơi "biết" cần dùng
     * service nào — controller không nên tự new service.</p>
     */
    private final AuthService authService;

    /**
     * Callback để thông báo HomeController khi trạng thái đăng nhập thay đổi
     * (đăng nhập thành công hoặc đăng xuất).
     */
    private Runnable onAuthStateChanged = () -> {};

    // ── Constructor ───────────────────────────────────────────────────────────

    public HomeLoginCoordinator(Window owner) {
        this.owner       = Objects.requireNonNull(owner, "owner không được null");
        this.authService = new AuthService(new UserRepository());
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setOnAuthStateChanged(Runnable onAuthStateChanged) {
        this.onAuthStateChanged = onAuthStateChanged != null ? onAuthStateChanged : () -> {};
    }

    public AuthService getAuthService() {
        return authService;
    }

    /**
     * Mở cửa sổ đăng nhập dạng modal.
     *
     * <p>Đây là method quan trọng nhất. Sau khi load FXML và có {@code ctrl},
     * coordinator gán đầy đủ 2 callback:</p>
     * <ol>
     *   <li>{@code configureForModal} — xử lý khi login thành công</li>
     *   <li>{@code setOnSignUpRequest} — xử lý khi user bấm "SIGN UP NOW"</li>
     * </ol>
     */
    public void openLoginWindow() {
        try {
            // ── Bước 1: Load FXML và lấy controller ──────────────────────────
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();

            // getController() trả về instance LoginController mà JavaFX đã tạo
            // khi load FXML. Đây là thời điểm duy nhất ta có thể lấy nó.
            LoginController ctrl = loader.getController();

            // ── Bước 2: Tạo Stage trước khi gán callback ─────────────────────
            // Lý do: callback finishLogin() cần đóng stage này,
            // nên stage phải tồn tại trước khi gán vào lambda.
            Stage loginStage = new Stage();

            // ── Bước 3: Cấu hình LoginController ─────────────────────────────

            // 3a. Gán AuthService và callback khi đăng nhập thành công
            ctrl.configureForModal(
                    authService,
                    user -> finishLogin(loginStage)  // đóng login, notify Home
            );

            // 3b. Gán callback khi user bấm "SIGN UP NOW"
            //
            // Lambda này làm 2 việc:
            //   1. Đóng cửa sổ Login hiện tại
            //   2. Mở cửa sổ Register (thông qua RegisterCoordinator)
            //
            // Tại sao tạo RegisterCoordinator ở đây?
            // Vì RegisterCoordinator cần "this" (HomeLoginCoordinator) để biết
            // phải gọi openLoginWindow() sau khi register/back-to-login.
            // Tạo ở đây đảm bảo RegisterCoordinator luôn có tham chiếu hợp lệ.
            ctrl.setOnSignUpRequest(() -> {
                loginStage.close(); // đóng Login

                // ⚠️ QUAN TRỌNG — Platform.runLater() là bắt buộc ở đây.
                //
                // Vấn đề: Khi user bấm "SIGN UP", button-click event đang chạy
                // bên TRONG call stack của loginStage.showAndWait() (dòng dưới).
                // Nếu ta gọi registerStage.showAndWait() ngay lập tức, JavaFX
                // sẽ tạo ra một "nested event loop" — cửa sổ Register hiện ra
                // nhưng KHÔNG nhận được keyboard input vì focus bị kẹt.
                //
                // Giải pháp: Platform.runLater() đẩy việc mở Register sang
                // vòng lặp sự kiện TIẾP THEO, lúc đó showAndWait() đầu tiên
                // đã thoát hoàn toàn → Register hoạt động bình thường.
                Platform.runLater(HomeLoginCoordinator.this::openRegisterWindow);
            });

            // ── Bước 4: Tạo Scene và hiện Stage ──────────────────────────────
            Scene scene = new Scene(root);
            // Stylesheet đã khai báo trong FXML (stylesheets="@../css/style.css")
            // nên không cần add thủ công ở đây nữa. Nếu cần add thêm:
            // scene.getStylesheets().add(...getResource("/css/style.css").toExternalForm());

            loginStage.setTitle("Đăng nhập — UBid");
            loginStage.initOwner(owner);
            loginStage.initModality(Modality.WINDOW_MODAL);
            loginStage.setMinWidth(640);
            loginStage.setMinHeight(480);
            loginStage.setScene(scene);
            loginStage.showAndWait();
            // showAndWait() block ở đây cho đến khi loginStage đóng lại.
            // Khi đó code tiếp tục bên dưới (nhưng thường không có gì thêm).

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Không tải được LoginView.fxml", e);
            AlertHelper.showError("Lỗi hệ thống", "Không thể mở màn hình đăng nhập.");
        }
    }

    /** Đăng xuất và thông báo Home refresh UI. */
    public void performLogout() {
        authService.logout();
        LOG.info("Đã đăng xuất");
        onAuthStateChanged.run();
    }

    // ── Private Methods ───────────────────────────────────────────────────────

    /**
     * Gọi khi Login thành công.
     * Đóng cửa sổ Login và thông báo HomeController cập nhật UI.
     */
    private void finishLogin(Stage loginStage) {
        loginStage.close();
        UserSession s = UserSession.getInstance();
        LOG.info(s.isLoggedIn()
                ? "Phiên đăng nhập: " + s.getCurrentUsername()
                : "Đăng nhập — session trống (bất thường)");
        onAuthStateChanged.run();
    }

    /**
     * Mở cửa sổ Register dạng modal (gọi qua RegisterCoordinator).
     *
     * <p>RegisterCoordinator nhận {@code this} (HomeLoginCoordinator) để có thể
     * gọi lại {@link #openLoginWindow()} sau khi:</p>
     * <ul>
     *   <li>Đăng ký thành công → thông báo xong → mở Login</li>
     *   <li>User bấm "SIGN IN" trong Register → mở Login</li>
     * </ul>
     */
    private void openRegisterWindow() {
        RegisterCoordinator registerCoord = new RegisterCoordinator(
                owner,
                authService,  // dùng chung AuthService — không tạo mới
                this          // tham chiếu về coordinator này để quay lại Login
        );
        registerCoord.openRegisterWindow();
    }
}