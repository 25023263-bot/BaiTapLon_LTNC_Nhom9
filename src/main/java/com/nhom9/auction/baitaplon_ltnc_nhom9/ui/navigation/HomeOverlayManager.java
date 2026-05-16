package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.navigation;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.Objects;

/**
 * Quản lý hiển thị một overlay duy nhất trong {@code mainStack}.
 * Tách khỏi HomeController để tránh lặp setVisible/setManaged.
 */
public final class HomeOverlayManager {

    public enum Screen {
        HOME,
        PROFILE,
        MY_PRODUCTS,
        SELLER_TERMS,
        LIST_PRODUCT,
        RESULTS,
        ADMIN,
        NOTIFICATION_PANEL
    }

    private final Node homeScrollPane;
    private final StackPane profileOverlay;
    private final StackPane myProductsOverlay;
    private final StackPane sellerTermsOverlay;
    private final StackPane listProductOverlay;
    private final StackPane resultsOverlay;
    private final StackPane adminOverlay;
    private final StackPane notifOverlay;

    private Screen current = Screen.HOME;

    public HomeOverlayManager(
            Node homeScrollPane,
            StackPane profileOverlay,
            StackPane myProductsOverlay,
            StackPane sellerTermsOverlay,
            StackPane listProductOverlay,
            StackPane resultsOverlay,
            StackPane adminOverlay,
            StackPane notifOverlay) {
        this.homeScrollPane = Objects.requireNonNull(homeScrollPane);
        this.profileOverlay = Objects.requireNonNull(profileOverlay);
        this.myProductsOverlay = Objects.requireNonNull(myProductsOverlay);
        this.sellerTermsOverlay = Objects.requireNonNull(sellerTermsOverlay);
        this.listProductOverlay = Objects.requireNonNull(listProductOverlay);
        this.resultsOverlay = Objects.requireNonNull(resultsOverlay);
        this.adminOverlay = Objects.requireNonNull(adminOverlay);
        this.notifOverlay = Objects.requireNonNull(notifOverlay);
    }

    public Screen current() {
        return current;
    }

    /** Ẩn mọi lớp nội dung chính; dùng trước khi bật panel chồng (thông báo). */
    public void hideMainLayers() {
        setLayer(homeScrollPane, false);
        setLayer(profileOverlay, false);
        setLayer(myProductsOverlay, false);
        setLayer(sellerTermsOverlay, false);
        setLayer(listProductOverlay, false);
        setLayer(resultsOverlay, false);
        setLayer(adminOverlay, false);
    }

    public void show(Screen screen) {
        hideMainLayers();
        setLayer(notifOverlay, false);

        Node target = switch (screen) {
            case HOME -> homeScrollPane;
            case PROFILE -> profileOverlay;
            case MY_PRODUCTS -> myProductsOverlay;
            case SELLER_TERMS -> sellerTermsOverlay;
            case LIST_PRODUCT -> listProductOverlay;
            case RESULTS -> resultsOverlay;
            case ADMIN -> adminOverlay;
            case NOTIFICATION_PANEL -> throw new IllegalArgumentException(
                    "Dùng showNotificationPanel() cho NOTIFICATION_PANEL");
        };

        setLayer(target, true);
        if (target instanceof StackPane sp) {
            sp.toFront();
        }
        current = screen;
    }

    public void showNotificationPanel() {
        setLayer(notifOverlay, true);
        notifOverlay.toFront();
    }

    public void hideNotificationPanel() {
        setLayer(notifOverlay, false);
    }

    private static void setLayer(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
