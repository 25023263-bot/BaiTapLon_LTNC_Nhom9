package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter.ProfilePresenter;

import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

/**
 * HomeAvatarManager — quản lý avatar button và login chrome trên thanh nav.
 *
 * <p>Được tách ra từ HomeController để giữ đúng nguyên tắc Single Responsibility:
 * HomeController chỉ wiring các thành phần, còn HomeAvatarManager chịu trách
 * nhiệm hoàn toàn về logic hiển thị avatar và trạng thái đăng nhập trên nav bar.</p>
 *
 * <p>Cách dùng trong HomeController:</p>
 * <pre>
 *   avatarManager = new HomeAvatarManager(btnLogInProminent, btnUserAvatar, profilePresenter);
 *   avatarManager.setOnLogoutRequest(() -> loginCoordinator.performLogout());
 *   avatarManager.refresh();
 * </pre>
 */
public class HomeAvatarManager {

    private static final int    AVATAR_SIZE        = 40;
    private static final String EXTERNAL_AVATAR_URL = ""; // để trống = dùng monogram

    private final Button           btnLogInProminent;
    private final Button           btnUserAvatar;
    private final ProfilePresenter profilePresenter;

    private ContextMenu avatarMenu;
    private Runnable    onLogoutRequest; // callback về HomeController

    // ─── Constructor ─────────────────────────────────────────────────────────

    /**
     * @param btnLogInProminent nút "Đăng nhập" hiện khi chưa login
     * @param btnUserAvatar     nút avatar hiện khi đã login
     * @param profilePresenter  dùng để cập nhật monogram trong profile panel
     */
    public HomeAvatarManager(Button btnLogInProminent,
                             Button btnUserAvatar,
                             ProfilePresenter profilePresenter) {
        this.btnLogInProminent = btnLogInProminent;
        this.btnUserAvatar     = btnUserAvatar;
        this.profilePresenter  = profilePresenter;
        buildAvatarMenu();
        wireAvatarButton();
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Đặt callback được gọi khi user chọn "Đăng xuất" từ menu avatar.
     * HomeController truyền vào: {@code () -> loginCoordinator.performLogout()}.
     */
    public void setOnLogoutRequest(Runnable callback) {
        this.onLogoutRequest = callback;
    }

    /**
     * Cập nhật trạng thái hiển thị dựa trên UserSession hiện tại.
     * Gọi mỗi khi trạng thái đăng nhập thay đổi.
     */
    public void refresh() {
        boolean logged = UserSession.getInstance().isLoggedIn();

        // Ẩn/hiện nút login và avatar
        btnLogInProminent.setVisible(!logged);
        btnLogInProminent.setManaged(!logged);
        btnUserAvatar.setVisible(logged);
        btnUserAvatar.setManaged(logged);

        if (logged) {
            updateAvatarGraphic();
            // Đồng bộ monogram trong Profile panel
            String username = UserSession.getInstance().getCurrentUsername();
            String fullName = UserSession.getInstance().getCurrentFullName();
            profilePresenter.updateHeroMonogram(username, fullName);
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void buildAvatarMenu() {
        avatarMenu = new ContextMenu();
        avatarMenu.getStyleClass().add("dark-context-menu");

        MenuItem miLogout = new MenuItem("Đăng xuất");
        miLogout.getStyleClass().add("menu-danger");
        miLogout.setOnAction(e -> {
            if (onLogoutRequest != null) onLogoutRequest.run();
        });

        avatarMenu.getItems().add(miLogout);
    }

    private void wireAvatarButton() {
        // Khi bấm avatar → hiện dropdown menu
        btnUserAvatar.setOnAction(e -> {
            if (avatarMenu != null) avatarMenu.show(btnUserAvatar, Side.BOTTOM, 0, 0);
        });
    }

    /**
     * Cập nhật graphic của avatar button.
     *
     * <p>Ưu tiên ảnh từ URL (nếu có). Nếu không → hiện monogram (chữ đầu tên).
     * Monogram được clip hình tròn để trông đẹp.</p>
     */
    private void updateAvatarGraphic() {
        String username = UserSession.getInstance().getCurrentUsername();
        String fullName = UserSession.getInstance().getCurrentFullName();

        // Tính chữ monogram: ưu tiên họ tên đầy đủ, fallback về username
        String monogram = (fullName != null && !fullName.isBlank())
                ? firstLetter(fullName)
                : firstLetter(username);

        // Inner circle: màu fill được định nghĩa trong CSS class avatar-inner-fill
        StackPane inner = new StackPane();
        inner.getStyleClass().add("avatar-inner-fill");
        inner.setMinSize(AVATAR_SIZE - 6, AVATAR_SIZE - 6);
        inner.setMaxSize(AVATAR_SIZE - 6, AVATAR_SIZE - 6);

        boolean usedImage = tryLoadExternalAvatar(inner);
        if (!usedImage) {
            Label g = new Label(monogram);
            g.getStyleClass().add("avatar-monogram");
            inner.getChildren().add(g);
        }

        // Outer wrapper: clip thành hình tròn
        StackPane wrap = new StackPane(inner);
        wrap.setMinSize(AVATAR_SIZE, AVATAR_SIZE);
        wrap.setMaxSize(AVATAR_SIZE, AVATAR_SIZE);

        Circle outerClip = new Circle(AVATAR_SIZE / 2.0 - 2);
        outerClip.centerXProperty().bind(wrap.widthProperty().divide(2));
        outerClip.centerYProperty().bind(wrap.heightProperty().divide(2));
        wrap.setClip(outerClip);

        btnUserAvatar.setGraphic(wrap);
        btnUserAvatar.setText(null);
    }

    /**
     * Cố tải ảnh từ URL bên ngoài. Trả về true nếu thành công.
     * Thất bại (URL trống, ảnh lỗi) → trả false → dùng monogram.
     */
    private boolean tryLoadExternalAvatar(StackPane container) {
        if (EXTERNAL_AVATAR_URL == null || EXTERNAL_AVATAR_URL.isBlank()) return false;
        try {
            Image img = new Image(EXTERNAL_AVATAR_URL,
                    AVATAR_SIZE - 8, AVATAR_SIZE - 8, true, true, true);
            if (img.isError()) return false;
            ImageView iv = new ImageView(img);
            iv.setSmooth(true);
            iv.setPreserveRatio(true);
            container.getChildren().add(iv);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String firstLetter(String s) {
        if (s == null) return "?";
        String t = s.trim();
        return t.isEmpty() ? "?" : t.substring(0, 1).toUpperCase();
    }
}
