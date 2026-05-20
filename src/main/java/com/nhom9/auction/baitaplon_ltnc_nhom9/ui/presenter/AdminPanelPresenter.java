package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.ItemDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.CurrencyFormatHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.network.ServerConnection;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.mapper.AuctionCardMapper;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Bảng quản trị dành cho ADMIN.
 *
 * <h3>Thay đổi so với phiên bản cũ:</h3>
 * <ul>
 *   <li>Bỏ {@code UserRepository}, {@code AuctionRepository}, {@code ServiceLocator}.</li>
 *   <li>{@link #loadAuctions()} dùng {@link ServerConnection#getAuctions()} qua socket.</li>
 *   <li>{@link #forceCloseAuction} dùng {@link ServerConnection#cancelAuction} qua socket.</li>
 *   <li>{@link #loadUsers()} dùng {@link ServerConnection#getUsers()} qua socket.</li>
 *   <li>{@link #toggleUserLock} dùng {@link ServerConnection#toggleUserLock(int)} qua socket.</li>
 *   <li>Tất cả thao tác mạng chạy trên background thread.</li>
 * </ul>
 */
public final class AdminPanelPresenter {

    private static final Logger LOG = Logger.getLogger(AdminPanelPresenter.class.getName());

    private static final String TAB_ACTIVE_STYLE =
            "-fx-background-color: rgba(201,168,76,0.07);"
                    + "-fx-background-insets: 0;-fx-background-radius: 0;"
                    + "-fx-border-color: null;-fx-border-width: 0;"
                    + "-fx-text-fill: #c9a84c;";
    private static final String TAB_INACTIVE_STYLE =
            "-fx-background-color: null;-fx-background-insets: 0;"
                    + "-fx-border-color: null;-fx-border-width: 0;-fx-text-fill: #4a4a4a;";

    private AdminPanelView view;
    private List<User> adminAllUsers = new ArrayList<>();
    private List<ItemDTO> adminAllItems = new ArrayList<>();

    // ── Bind ─────────────────────────────────────────────────────────────────

    public void bind(AdminPanelView view) {
        this.view = view;
    }

    // ── Init / Refresh ────────────────────────────────────────────────────────

    public void refresh() {
        // Admin session info
        var session = UserSession.getInstance();
        if (session.isLoggedIn() && view.adminSubtitleLabel() != null) {
            String name = session.getCurrentUsername();
            view.adminSubtitleLabel().setText(
                    "Xin chào " + name + " · Quản lý người dùng và phiên đấu giá");
        }

        // Khởi tạo filter combo nếu chưa có
        if (view.adminUserRoleFilter().getItems().isEmpty()) {
            view.adminUserRoleFilter().getItems().addAll("Tất cả", "BUYER", "SELLER", "ADMIN");
            view.adminUserRoleFilter().getSelectionModel().selectFirst();
            view.adminUserRoleFilter().setOnAction(e -> searchUsers());
        }
        if (view.adminAuctionStatusFilter().getItems().isEmpty()) {
            view.adminAuctionStatusFilter().getItems().addAll(
                    "Tất cả", "PENDING", "ACTIVE", "CLOSED", "EXPIRED", "CANCELLED");
            view.adminAuctionStatusFilter().getSelectionModel().selectFirst();
            view.adminAuctionStatusFilter().setOnAction(e -> searchAuctions());
        }
        showUsersTab();
    }

    // ── Tab switching ─────────────────────────────────────────────────────────

    public void showUsersTab() {
        setVisible(view.adminUsersPanel(), true);
        setVisible(view.adminAuctionsPanel(), false);
        view.adminTabUsers().getStyleClass().remove("admin-tab-active");
        view.adminTabUsers().getStyleClass().add("admin-tab-active");
        view.adminTabUsers().setStyle(TAB_ACTIVE_STYLE);
        view.adminTabAuctions().getStyleClass().remove("admin-tab-active");
        view.adminTabAuctions().setStyle(TAB_INACTIVE_STYLE);
        view.adminTabUsersIndicator().setStyle("-fx-background-color: #c9a84c;");
        view.adminTabAuctionsIndicator().setStyle("-fx-background-color: transparent;");
        view.adminOverlay().requestFocus();
        loadUsers();
    }

    public void showAuctionsTab() {
        setVisible(view.adminUsersPanel(), false);
        setVisible(view.adminAuctionsPanel(), true);
        view.adminTabAuctions().getStyleClass().remove("admin-tab-active");
        view.adminTabAuctions().getStyleClass().add("admin-tab-active");
        view.adminTabAuctions().setStyle(TAB_ACTIVE_STYLE);
        view.adminTabUsers().getStyleClass().remove("admin-tab-active");
        view.adminTabUsers().setStyle(TAB_INACTIVE_STYLE);
        view.adminTabAuctionsIndicator().setStyle("-fx-background-color: #c9a84c;");
        view.adminTabUsersIndicator().setStyle("-fx-background-color: transparent;");
        view.adminOverlay().requestFocus();
        loadAuctions();
    }

    // ── Load users ────────────────────────────────────────────────────────────

    /**
     * Load danh sách users từ server qua socket (background thread).
     * Dùng {@link ServerConnection#getUsers()} thay vì ServiceLocator trực tiếp.
     */
    public void loadUsers() {
        Thread t = new Thread(() -> {
            try {
                List<User> users = ServerConnection.getUsers();
                Platform.runLater(() -> {
                    adminAllUsers = new ArrayList<>(users);
                    searchUsers();
                });
            } catch (Exception e) {
                LOG.warning("Admin: không load được danh sách user qua socket: " + e.getMessage());
                Platform.runLater(() -> AlertHelper.showToast("Lỗi tải danh sách người dùng"));
            }
        }, "admin-load-users");
        t.setDaemon(true);
        t.start();
    }

    // ── Load auctions (qua socket) ────────────────────────────────────────────

    /**
     * Load danh sách phiên đấu giá từ server qua {@link ServerConnection#getAuctions()}.
     */
    public void loadAuctions() {
        Thread t = new Thread(() -> {
            try {
                List<ItemDTO> items = ServerConnection.getAuctions();
                Platform.runLater(() -> {
                    adminAllItems = new ArrayList<>(items);
                    searchAuctions();
                });
            } catch (Exception e) {
                LOG.warning("Admin: lỗi load phiên đấu giá qua socket: " + e.getMessage());
                Platform.runLater(() -> AlertHelper.showToast("Lỗi tải danh sách phiên đấu giá"));
            }
        }, "admin-load-auctions");
        t.setDaemon(true);
        t.start();
    }

    // ── Search / Filter ───────────────────────────────────────────────────────

    public void searchUsers() {
        String keyword = view.adminUserSearchField().getText().trim().toLowerCase();
        String roleFilter = view.adminUserRoleFilter().getValue() == null
                || view.adminUserRoleFilter().getValue().equals("Tất cả")
                ? "" : view.adminUserRoleFilter().getValue().toLowerCase();

        List<User> filtered = adminAllUsers.stream()
                .filter(u -> {
                    boolean matchText = keyword.isEmpty()
                            || u.getUsername().toLowerCase().contains(keyword)
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(keyword));
                    boolean matchRole = roleFilter.isEmpty()
                            || u.getRole().name().equalsIgnoreCase(roleFilter);
                    return matchText && matchRole;
                })
                .toList();
        renderUserRows(filtered);
    }

    public void searchAuctions() {
        String keyword = view.adminAuctionSearchField().getText().trim().toLowerCase();
        String statusFilter = view.adminAuctionStatusFilter().getValue() == null
                || view.adminAuctionStatusFilter().getValue().equals("Tất cả")
                ? "" : view.adminAuctionStatusFilter().getValue().toLowerCase();

        var filtered = adminAllItems.stream()
                .filter(item -> {
                    boolean matchText = keyword.isEmpty()
                            || item.getTitle().toLowerCase().contains(keyword);
                    boolean matchStatus = statusFilter.isEmpty()
                            || item.getStatus().name().equalsIgnoreCase(statusFilter);
                    return matchText && matchStatus;
                })
                .toList();
        renderAuctionRows(filtered);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    private void renderUserRows(List<User> users) {
        view.adminUsersList().getChildren().clear();
        boolean empty = users.isEmpty();
        setVisible(view.adminUsersEmpty(), empty);
        if (empty) return;

        int currentAdminId = UserSession.getInstance().getCurrentUserId();
        for (User u : users) {
            HBox row = new HBox(0);
            row.getStyleClass().add("admin-row");
            row.setAlignment(Pos.CENTER_LEFT);

            Label idLabel = new Label(String.valueOf(u.getId()));
            idLabel.getStyleClass().add("admin-td-muted");
            idLabel.setMinWidth(50); idLabel.setMaxWidth(50);

            Label nameLabel = new Label(u.getUsername());
            nameLabel.getStyleClass().add("admin-td");
            nameLabel.setMinWidth(130);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            Label emailLabel = new Label(u.getEmail() != null ? u.getEmail() : "—");
            emailLabel.getStyleClass().add("admin-td-muted");
            emailLabel.setMinWidth(170);
            HBox.setHgrow(emailLabel, Priority.ALWAYS);

            Label roleLabel = new Label(u.getRole().name());
            roleLabel.getStyleClass().add(switch (u.getRole()) {
                case BUYER -> "admin-role-buyer";
                case SELLER -> "admin-role-seller";
                case ADMIN -> "admin-role-admin";
            });
            roleLabel.setMinWidth(110); roleLabel.setMaxWidth(110);

            Label statusLabel = new Label(u.isActive() ? "✓ Active" : "✕ Bị khoá");
            statusLabel.getStyleClass().add(u.isActive() ? "admin-badge-active" : "admin-badge-locked");
            HBox statusWrap = new HBox(statusLabel);
            statusWrap.setAlignment(Pos.CENTER_LEFT);
            statusWrap.setMinWidth(100); statusWrap.setMaxWidth(100);
            statusWrap.setStyle("-fx-padding: 0 12 0 12;");

            HBox actionBox = new HBox(6);
            actionBox.setAlignment(Pos.CENTER_LEFT);
            actionBox.setMinWidth(120); actionBox.setMaxWidth(120);
            actionBox.setStyle("-fx-padding: 0 12 0 12;");

            if (u.getId() != currentAdminId && u.getRole() != UserRole.ADMIN) {
                Button actionBtn = new Button(u.isActive() ? "🔒 Khoá" : "🔓 Mở khoá");
                actionBtn.getStyleClass().add(u.isActive() ? "admin-btn-lock" : "admin-btn-unlock");
                final User snapshot = u;
                actionBtn.setOnAction(e -> toggleUserLock(snapshot));
                actionBox.getChildren().add(actionBtn);
            } else {
                Label noAction = new Label("—");
                noAction.getStyleClass().add("admin-td-muted");
                actionBox.getChildren().add(noAction);
            }

            row.getChildren().addAll(idLabel, nameLabel, emailLabel, roleLabel, statusWrap, actionBox);
            view.adminUsersList().getChildren().add(row);
        }
    }

    private void renderAuctionRows(List<ItemDTO> items) {
        view.adminAuctionsList().getChildren().clear();
        boolean empty = items.isEmpty();
        setVisible(view.adminAuctionsEmpty(), empty);
        if (empty) return;

        for (ItemDTO item : items) {
            HBox row = new HBox(0);
            row.getStyleClass().add("admin-row");
            row.setAlignment(Pos.CENTER_LEFT);

            Label idLabel = new Label(String.valueOf(item.getId()));
            idLabel.getStyleClass().add("admin-td-muted");
            idLabel.setMinWidth(50); idLabel.setMaxWidth(50);

            Label titleLabel = new Label(item.getTitle());
            titleLabel.getStyleClass().add("admin-td");
            titleLabel.setMinWidth(160); titleLabel.setMaxWidth(220);
            HBox.setHgrow(titleLabel, Priority.ALWAYS);

            Label sellerLabel = new Label("seller #" + item.getSellerId());
            sellerLabel.getStyleClass().add("admin-td-muted");
            sellerLabel.setMinWidth(110); sellerLabel.setMaxWidth(110);

            String price = item.getCurrentPrice() != null
                    ? CurrencyFormatHelper.formatVnd(item.getCurrentPrice()) : "—";
            Label priceLabel = new Label(price);
            priceLabel.getStyleClass().add("admin-td");
            priceLabel.setMinWidth(120); priceLabel.setMaxWidth(120);

            Label statusLabel = new Label(AuctionCardMapper.statusDisplay(item.getStatus()));
            statusLabel.getStyleClass().add(switch (item.getStatus()) {
                case ACTIVE -> "admin-badge-active";
                case PENDING -> "admin-badge-pending";
                case CLOSED, EXPIRED, CANCELLED -> "admin-badge-closed";
            });
            HBox statusWrap = new HBox(statusLabel);
            statusWrap.setAlignment(Pos.CENTER_LEFT);
            statusWrap.setMinWidth(110); statusWrap.setMaxWidth(110);
            statusWrap.setStyle("-fx-padding: 0 12 0 12;");

            HBox actionBox = new HBox(6);
            actionBox.setAlignment(Pos.CENTER_LEFT);
            actionBox.setMinWidth(130); actionBox.setMaxWidth(130);
            actionBox.setStyle("-fx-padding: 0 12 0 12;");

            if (item.getStatus() == AuctionStatus.ACTIVE) {
                Button closeBtn = new Button("⛔ Đóng ngay");
                closeBtn.getStyleClass().add("admin-btn-close");
                closeBtn.setOnAction(e -> forceCloseAuction(item));
                actionBox.getChildren().add(closeBtn);
            } else {
                Label noAction = new Label("—");
                noAction.getStyleClass().add("admin-td-muted");
                actionBox.getChildren().add(noAction);
            }

            row.getChildren().addAll(idLabel, titleLabel, sellerLabel, priceLabel, statusWrap, actionBox);
            view.adminAuctionsList().getChildren().add(row);
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /**
     * Toggle lock/unlock user qua socket.
     * Server tự toggle trạng thái active và lưu DB.
     * Client chỉ reload lại danh sách sau khi thành công.
     */
    private void toggleUserLock(User u) {
        boolean willLock = u.isActive();
        String confirmMsg = willLock
                ? "Bạn có chắc muốn khoá tài khoản \"" + u.getUsername() + "\"?\nUser này sẽ không thể đăng nhập."
                : "Mở khoá tài khoản \"" + u.getUsername() + "\"?";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText(null);
        confirm.setContentText(confirmMsg);

        confirm.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            Thread t = new Thread(() -> {
                try {
                    String msg = ServerConnection.toggleUserLock(u.getId());
                    Platform.runLater(() -> {
                        AlertHelper.showToast(msg);
                        loadUsers();
                    });
                } catch (Exception e) {
                    LOG.warning("Admin: lỗi khi toggle lock user: " + e.getMessage());
                    Platform.runLater(() ->
                            AlertHelper.showToast("Lỗi: không thể cập nhật trạng thái user"));
                }
            }, "admin-toggle-lock-thread");
            t.setDaemon(true);
            t.start();
        });
    }

    /**
     * Admin đóng phiên đấu giá ngay lập tức.
     * Dùng {@link ServerConnection#cancelAuction(int)} qua socket.
     */
    private void forceCloseAuction(ItemDTO item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận đóng phiên");
        confirm.setHeaderText(null);
        confirm.setContentText("Đóng sớm phiên \"" + item.getTitle() + "\"?\n"
                + "Hệ thống sẽ xử lý thanh toán ngay nếu có người đặt giá.");

        confirm.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;

            Thread t = new Thread(() -> {
                try {
                    ServerConnection.cancelAuction(item.getId());
                    Platform.runLater(() -> {
                        AlertHelper.showToast("Đã đóng phiên: " + item.getTitle());
                        loadAuctions();
                    });
                } catch (Exception e) {
                    LOG.warning("Admin: lỗi đóng phiên #" + item.getId()
                            + " qua socket: " + e.getMessage());
                    Platform.runLater(() ->
                            AlertHelper.showToast("Lỗi: không thể đóng phiên này"));
                }
            }, "admin-force-close-thread");
            t.setDaemon(true);
            t.start();
        });
    }

    // ── Private helper ────────────────────────────────────────────────────────

    private static void setVisible(VBox node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
