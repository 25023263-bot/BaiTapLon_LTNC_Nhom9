package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Notification;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.notification.NotificationService;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;

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

/** Chuông thông báo và panel danh sách. */
public final class HomeNotificationPresenter {

    private final NotificationService notifService;

    private Label lblBellBadge;
    private VBox notifList;
    private StackPane notifOverlay;
    private Runnable showLoginRequired;
    private Consumer<Integer> onOpenPanel;

    private ScheduledExecutorService badgePoller;

    public HomeNotificationPresenter(NotificationService notifService) {
        this.notifService = notifService;
    }

    public void bind(
            Label lblBellBadge,
            VBox notifList,
            StackPane notifOverlay,
            Runnable showLoginRequired,
            Consumer<Integer> onOpenPanel) {
        this.lblBellBadge = lblBellBadge;
        this.notifList = notifList;
        this.notifOverlay = notifOverlay;
        this.showLoginRequired = showLoginRequired;
        this.onOpenPanel = onOpenPanel;
    }

    public void start() {
        notifService.addUiListener(event -> Platform.runLater(() -> {
            User user = UserSession.getInstance().getCurrentUser();
            if (user == null) return;
            refreshBadge(notifService.unreadCountFresh(user.getId()));
        }));

        badgePoller = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "notif-badge-poll");
            t.setDaemon(true);
            return t;
        });
        badgePoller.scheduleAtFixedRate(() -> {
            User user = UserSession.getInstance().getCurrentUser();
            if (user == null) return;
            int count = notifService.unreadCount(user.getId());
            Platform.runLater(() -> refreshBadge(count));
        }, 5, 30, TimeUnit.SECONDS);
    }

    public void openPanel() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null) {
            showLoginRequired.run();
            return;
        }
        notifService.markAllRead(user.getId());
        refreshBadge(0);
        render(user.getId());
        onOpenPanel.accept(user.getId());
    }

    public void closePanel() {
        notifOverlay.setVisible(false);
        notifOverlay.setManaged(false);
    }

    public void handleBackdropClick(MouseEvent e) {
        if (e.getTarget() == notifOverlay) closePanel();
    }

    public void markAllRead() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null) return;
        notifService.clearAll(user.getId());
        refreshBadge(0);
        render(user.getId());
    }

    public void shutdown() {
        if (badgePoller != null && !badgePoller.isShutdown()) {
            badgePoller.shutdownNow();
        }
    }

    private void render(int userId) {
        notifList.getChildren().clear();
        List<Notification> items = notifService.getNotifications(userId);

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
            notifService.markRead(n.getId(), userId);
            row.getStyleClass().remove("notif-item-unread");
            refreshBadge(notifService.unreadCount(userId));
        });
        return row;
    }

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
