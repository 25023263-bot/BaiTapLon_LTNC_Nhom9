package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Notification;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.network.ServerConnection;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Chuông thông báo và panel danh sách.
 *
 * <p>Phiên bản này không còn phụ thuộc vào {@code NotificationService} hay {@code ServiceLocator}.
 * Mọi thao tác (lấy danh sách, đánh dấu đọc, xóa) đều đi qua {@link ServerConnection} qua socket.
 *
 * <p>Badge được cập nhật qua 2 cơ chế:
 * <ul>
 *   <li><b>Polling:</b> mỗi 30 giây gọi {@code GET_NOTIFICATIONS} để đếm unread.</li>
 *   <li><b>Push (realtime):</b> server gửi {@code NOTIFICATION} qua socket khi có sự kiện mới →
 *       {@link #onServerNotification()} được gọi để tăng badge ngay lập tức.</li>
 * </ul>
 */
public final class HomeNotificationPresenter {

    private static final Logger LOG = Logger.getLogger(HomeNotificationPresenter.class.getName());

    private Label lblBellBadge;
    private VBox notifList;
    private StackPane notifOverlay;
    private Runnable showLoginRequired;
    private Consumer<Integer> onOpenPanel;

    private ScheduledExecutorService badgePoller;

    // ── Bind ──────────────────────────────────────────────────────────────────

    public void bind(
            Label lblBellBadge,
            VBox notifList,
            StackPane notifOverlay,
            Runnable showLoginRequired,
            Consumer<Integer> onOpenPanel) {
        this.lblBellBadge      = lblBellBadge;
        this.notifList         = notifList;
        this.notifOverlay      = notifOverlay;
        this.showLoginRequired = showLoginRequired;
        this.onOpenPanel       = onOpenPanel;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Khởi động polling badge mỗi 30 giây.
     * Gọi sau khi bind() và sau khi user đăng nhập.
     */
    public void start() {
        badgePoller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "notif-badge-poll");
            t.setDaemon(true);
            return t;
        });
        badgePoller.scheduleAtFixedRate(() -> {
            if (!UserSession.getInstance().isLoggedIn()) return;
            int userId = UserSession.getInstance().getCurrentUserId();
            try {
                List<Notification> notifs = ServerConnection.getNotifications(userId);
                long unread = notifs.stream().filter(n -> !n.isRead()).count();
                Platform.runLater(() -> refreshBadge((int) unread));
            } catch (Exception e) {
                LOG.warning("Lỗi poll badge thông báo: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    public void shutdown() {
        if (badgePoller != null && !badgePoller.isShutdown()) {
            badgePoller.shutdownNow();
        }
    }

    // ── Realtime push từ socket ───────────────────────────────────────────────

    /**
     * Gọi khi server push NOTIFICATION qua socket (từ HomeController).
     * Tăng badge ngay lập tức mà không cần chờ polling.
     * Phải gọi trên JavaFX thread (đã được đảm bảo bởi Platform.runLater trong SocketClient).
     */
    public void onServerNotification() {
        if (!UserSession.getInstance().isLoggedIn()) return;
        // Fetch lại count từ server để chính xác
        int userId = UserSession.getInstance().getCurrentUserId();
        new Thread(() -> {
            try {
                List<Notification> notifs = ServerConnection.getNotifications(userId);
                long unread = notifs.stream().filter(n -> !n.isRead()).count();
                Platform.runLater(() -> refreshBadge((int) unread));
            } catch (Exception e) {
                LOG.warning("Lỗi refresh badge sau notification push: " + e.getMessage());
            }
        }, "notif-push-refresh").start();
    }

    // ── Panel actions ─────────────────────────────────────────────────────────

    public void openPanel() {
        if (!UserSession.getInstance().isLoggedIn()) {
            showLoginRequired.run();
            return;
        }
        int userId = UserSession.getInstance().getCurrentUserId();
        // Đánh dấu tất cả đã đọc khi mở panel
        new Thread(() -> {
            try {
                ServerConnection.markAllNotificationsRead(userId);
                Platform.runLater(() -> {
                    refreshBadge(0);
                    renderAsync(userId);
                    onOpenPanel.accept(userId);
                });
            } catch (Exception e) {
                LOG.warning("Lỗi markAllRead khi mở panel: " + e.getMessage());
                Platform.runLater(() -> {
                    renderAsync(userId);
                    onOpenPanel.accept(userId);
                });
            }
        }, "notif-open-panel").start();
    }

    public void closePanel() {
        notifOverlay.setVisible(false);
        notifOverlay.setManaged(false);
    }

    public void handleBackdropClick(MouseEvent e) {
        if (e.getTarget() == notifOverlay) closePanel();
    }

    public void markAllRead() {
        if (!UserSession.getInstance().isLoggedIn()) return;
        int userId = UserSession.getInstance().getCurrentUserId();
        new Thread(() -> {
            try {
                ServerConnection.clearNotifications(userId);
                Platform.runLater(() -> {
                    refreshBadge(0);
                    renderAsync(userId);
                });
            } catch (Exception e) {
                LOG.warning("Lỗi clearNotifications: " + e.getMessage());
            }
        }, "notif-clear").start();
    }

    // ── Render ────────────────────────────────────────────────────────────────

    /**
     * Load danh sách thông báo từ server rồi render (background thread → UI thread).
     */
    private void renderAsync(int userId) {
        new Thread(() -> {
            try {
                List<Notification> items = ServerConnection.getNotifications(userId);
                Platform.runLater(() -> render(items, userId));
            } catch (Exception e) {
                LOG.warning("Lỗi load notifications: " + e.getMessage());
                Platform.runLater(() -> render(List.of(), userId));
            }
        }, "notif-render").start();
    }

    private void render(List<Notification> items, int userId) {
        notifList.getChildren().clear();

        if (items.isEmpty()) {
            Label empty = new Label("Không có thông báo nào");
            empty.getStyleClass().add("notif-empty-label");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            notifList.getChildren().add(empty);
            return;
        }

        for (Notification n : items) {
            notifList.getChildren().add(buildRow(n, userId));
        }
    }

    private HBox buildRow(Notification n, int userId) {
        Label icon = new Label(n.getIcon());
        icon.getStyleClass().add("notif-item-icon");

        Label msg = new Label(n.getMessage());
        msg.getStyleClass().add("notif-item-message");
        msg.setWrapText(true);
        msg.setMaxWidth(270);

        Label time = new Label(n.getFormattedTime());
        time.getStyleClass().add("notif-item-time");

        VBox content = new VBox(4, msg, time);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox row = new HBox(12, icon, content);
        row.getStyleClass().add("notif-item");
        if (!n.isRead()) row.getStyleClass().add("notif-item-unread");

        row.setOnMouseClicked(e -> {
            if (n.isRead()) return;
            row.getStyleClass().remove("notif-item-unread");
            // Đánh dấu đọc trên server (fire-and-forget)
            new Thread(() -> {
                try {
                    ServerConnection.markNotificationRead(n.getId(), userId);
                    // Refresh badge sau khi đánh dấu
                    List<Notification> updated = ServerConnection.getNotifications(userId);
                    long unread = updated.stream().filter(x -> !x.isRead()).count();
                    Platform.runLater(() -> refreshBadge((int) unread));
                } catch (Exception ex) {
                    LOG.warning("Lỗi markRead #" + n.getId() + ": " + ex.getMessage());
                }
            }, "notif-mark-read").start();
        });
        return row;
    }

    // ── Badge ─────────────────────────────────────────────────────────────────

    private void refreshBadge(int count) {
        if (count <= 0) {
            lblBellBadge.setVisible(false);
            lblBellBadge.setManaged(false);
        } else {
            lblBellBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            lblBellBadge.setVisible(true);
            lblBellBadge.setManaged(true);
        }
    }
}
