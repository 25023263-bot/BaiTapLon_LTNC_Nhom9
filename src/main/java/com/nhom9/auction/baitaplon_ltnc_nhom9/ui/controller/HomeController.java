package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator.HomeLoginCoordinator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;

import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * HomeController — navbar theo phiên đăng nhập, bottom nav có tab “Cá nhân”,
 * Place Bid gated bằng {@link UserSession}. Luồng login được nối qua {@link HomeLoginCoordinator}.
 */
public class HomeController implements Initializable {

    private static final int AVATAR_SIZE = 40;
    /** Tuỳ chọn URL ảnh đại diện demo; để trống để chỉ hiện monogram */
    private static final String EXTERNAL_AVATAR_URL = "";

    @FXML private BorderPane rootPane;
    @FXML private StackPane mainStack;
    @FXML private ScrollPane homeScrollPane;
    @FXML private StackPane profileOverlay;
    @FXML private HBox hotCardsContainer;
    @FXML private GridPane allProductsGrid;
    @FXML private HBox categoryChipsContainer;
    @FXML private TextField searchField;
    @FXML private Label resultCountLabel;
    @FXML private ComboBox<String> filterDropdown;
    @FXML private Button chipAll;
    @FXML private Button btnLogInProminent;
    @FXML private Button btnUserAvatar;
    @FXML private Label profileTitleLabel;
    @FXML private Label profileHintLabel;
    @FXML private Label profileAvatarGlyph;
    @FXML private Button profileTabLoginButton;
    @FXML private Button profileLogoutButton;
    @FXML private ToggleButton bottomNavHome;
    @FXML private ToggleButton bottomNavCategories;
    @FXML private ToggleButton bottomNavProfile;

    private ScheduledExecutorService timerScheduler;
    private final Map<String, Label> timerLabels = new HashMap<>();
    private Button activeChipButton;

    private HomeLoginCoordinator loginCoordinator;
    private ContextMenu avatarMenu;

    public record AuctionItem(
            String id,
            String title,
            String category,
            String categoryEmoji,
            double currentBid,
            int bidCount,
            boolean isLive,
            LocalDateTime endTime,
            String imagePlaceholderEmoji
    ) {}

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilterDropdown();
        activeChipButton = chipAll;
        loadHotAuctions();
        loadAllAuctions();
        startCountdownTimers();

        ToggleGroup tabs = new ToggleGroup();
        bottomNavHome.setToggleGroup(tabs);
        bottomNavCategories.setToggleGroup(tabs);
        bottomNavProfile.setToggleGroup(tabs);

        tabs.selectedToggleProperty().addListener((obs, oldT, sel) -> {
            if (!(sel instanceof ToggleButton)) return;
            onBottomTabSwitch((ToggleButton) sel);
        });
        bottomNavHome.setSelected(true);

        rootPane.sceneProperty().addListener((obs, os, scene) -> {
            if (scene == null || loginCoordinator != null) return;
            if (scene.getWindow() instanceof Stage stage) {
                loginCoordinator = new HomeLoginCoordinator(stage);
                loginCoordinator.setOnAuthStateChanged(this::refreshLoginUiChrome);
                refreshLoginUiChrome();
            }
        });
        Platform.runLater(this::bootstrapCoordinatorIfPossible);
        buildAvatarMenu();
    }

    private void bootstrapCoordinatorIfPossible() {
        if (loginCoordinator != null || rootPane.getScene() == null) return;
        var scene = rootPane.getScene();
        if (scene.getWindow() instanceof Stage stage) {
            loginCoordinator = new HomeLoginCoordinator(stage);
            loginCoordinator.setOnAuthStateChanged(this::refreshLoginUiChrome);
            refreshLoginUiChrome();
        }
    }

    private void setupFilterDropdown() {
        filterDropdown.getItems().addAll(
                "Ending Soon",
                "Most Bids",
                "Lowest Price",
                "Highest Price",
                "Newest First");
        filterDropdown.setValue("Ending Soon");
        filterDropdown.setOnAction(e -> handleFilterChange());
    }

    private List<AuctionItem> getMockHotAuctions() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                new AuctionItem("HOT-001",
                        "Patek Philippe Nautilus 5711 – Rare Edition",
                        "Watches", "⌚",
                        195_000.00, 212, false, null, "🕐"),
                new AuctionItem("HOT-002",
                        "Pink Diamond 3.5 Carat – GIA Certified",
                        "Jewelry", "💎",
                        124_000.00, 143, false, null, "💎"),
                new AuctionItem("HOT-003",
                        "Rolex Submariner 1965 – Limited Edition",
                        "Watches", "⌚",
                        32_500.00, 87,
                        true, now.plusSeconds(13 * 3600 + 26 * 60 + 28), "⌚"));
    }

    private List<AuctionItem> getMockAllAuctions() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                new AuctionItem("ALL-001", "Vintage Cartier Tank – 1972",
                        "Watches", "⌚", 18_500.0, 54, true, now.plusHours(2), "🕑"),
                new AuctionItem("ALL-002", "18K Gold Emerald Necklace",
                        "Jewelry", "💚", 8_900.0, 31, true, now.plusHours(5), "💍"),
                new AuctionItem("ALL-003", "Monet Impression – Signed Print",
                        "Art", "🎨", 45_000.0, 98, true, now.plusHours(1), "🖼"),
                new AuctionItem("ALL-004", "Ming Dynasty Vase – Authenticated",
                        "Antiques", "🏺", 62_000.0, 77, false, null, "🏺"),
                new AuctionItem("ALL-005", "Ferrari 250 GTO – 1962 Replica",
                        "Cars", "🚗", 280_000.0, 211, true, now.plusHours(9), "🏎"),
                new AuctionItem("ALL-006", "Rare Penny Black Stamp – 1840",
                        "Collectibles", "🏆", 14_200.0, 42, true, now.plusHours(3), "📮"));
    }

    private void loadHotAuctions() {
        hotCardsContainer.getChildren().clear();
        List<AuctionItem> items = getMockHotAuctions();
        for (AuctionItem item : items) {
            VBox card = buildHotCard(item);
            HBox.setHgrow(card, Priority.ALWAYS);
            hotCardsContainer.getChildren().add(card);
        }
    }

    private void loadAllAuctions() {
        allProductsGrid.getChildren().clear();
        allProductsGrid.getColumnConstraints().clear();
        int columns = 3;
        for (int i = 0; i < columns; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columns);
            cc.setHgrow(Priority.ALWAYS);
            allProductsGrid.getColumnConstraints().add(cc);
        }
        List<AuctionItem> items = getMockAllAuctions();
        for (int i = 0; i < items.size(); i++) {
            VBox card = buildSmallCard(items.get(i));
            allProductsGrid.add(card, i % columns, i / columns);
        }
        resultCountLabel.setText(items.size() + " results");
    }

    private VBox buildHotCard(AuctionItem item) {
        VBox card = new VBox();
        card.getStyleClass().add("auction-card");
        card.setSpacing(0);

        StackPane imageStack = new StackPane();
        imageStack.getStyleClass().add("card-image-placeholder");
        Label imgIcon = new Label(item.imagePlaceholderEmoji());
        imgIcon.getStyleClass().add("card-image-icon");
        imageStack.getChildren().add(imgIcon);

        HBox topOverlay = new HBox();
        topOverlay.setAlignment(Pos.TOP_CENTER);
        topOverlay.setSpacing(8);
        StackPane.setAlignment(topOverlay, Pos.TOP_CENTER);

        Label categoryBadge = new Label(item.categoryEmoji() + "  " + item.category());
        categoryBadge.getStyleClass().add("badge-category");
        Region badgeSpacer = new Region();
        HBox.setHgrow(badgeSpacer, Priority.ALWAYS);

        Label liveBadge = new Label(item.isLive() ? "● LIVE AUCTION" : "● AUCTION ENDED");
        liveBadge.getStyleClass().add(item.isLive() ? "badge-live" : "badge-ended");
        topOverlay.getChildren().addAll(categoryBadge, badgeSpacer, liveBadge);
        imageStack.getChildren().add(topOverlay);

        HBox timerRow = new HBox();
        timerRow.getStyleClass().add("timer-row");
        timerRow.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(timerRow, Pos.BOTTOM_CENTER);
        Label timerIcon = new Label("⏱");
        Label timerLabel = new Label();
        if (item.isLive() && item.endTime() != null) {
            timerLabel.getStyleClass().add("timer-label-live");
            timerLabel.setText("--:--:--");
            timerLabels.put(item.id(), timerLabel);
        } else {
            timerLabel.getStyleClass().add("timer-label-ended");
            timerLabel.setText("Auction Ended");
        }
        timerRow.getChildren().addAll(timerIcon, timerLabel);
        imageStack.getChildren().add(timerRow);

        VBox cardBody = new VBox();
        cardBody.getStyleClass().add("card-body");
        HBox priceRow = new HBox();
        priceRow.getStyleClass().add("card-price-row");
        VBox priceLeft = new VBox(2);
        Label bidLbl = new Label("Current Bid");
        bidLbl.getStyleClass().add("price-label-small");
        Label priceValue = new Label(formatPrice(item.currentBid()));
        priceValue.getStyleClass().add("price-value");
        priceLeft.getChildren().addAll(bidLbl, priceValue);
        Region priceSpacer = new Region();
        HBox.setHgrow(priceSpacer, Priority.ALWAYS);
        VBox bidsRight = new VBox(2);
        bidsRight.setAlignment(Pos.TOP_RIGHT);
        Label bidsLbl = new Label("📈  Bids");
        bidsLbl.getStyleClass().add("bid-count-label");
        Label bidsVal = new Label(String.valueOf(item.bidCount()));
        bidsVal.getStyleClass().add("bid-count-value");
        bidsRight.getChildren().addAll(bidsLbl, bidsVal);
        priceRow.getChildren().addAll(priceLeft, priceSpacer, bidsRight);

        Label title = new Label(item.title());
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        cardBody.getChildren().addAll(title, priceRow);

        VBox cardFooter = new VBox();
        cardFooter.getStyleClass().add("card-footer");
        Button bidBtn = new Button("Place Bid");
        bidBtn.getStyleClass().add("btn-bid");
        bidBtn.setMaxWidth(Double.MAX_VALUE);
        bidBtn.setOnAction(e -> handlePlaceBid(item.id()));
        cardFooter.getChildren().add(bidBtn);

        card.getChildren().addAll(imageStack, cardBody, cardFooter);
        return card;
    }

    private VBox buildSmallCard(AuctionItem item) {
        VBox card = new VBox();
        card.getStyleClass().add("product-card-sm");
        StackPane imgPane = new StackPane();
        imgPane.getStyleClass().add("card-sm-image");
        Label imgLabel = new Label(item.imagePlaceholderEmoji());
        imgPane.getChildren().add(imgLabel);

        VBox body = new VBox(4);
        body.getStyleClass().add("card-sm-body");
        Label categoryLabel = new Label(item.categoryEmoji() + "  " + item.category());
        categoryLabel.getStyleClass().add("card-sm-category");
        Label titleLabel = new Label(item.title());
        titleLabel.getStyleClass().add("card-sm-title");
        titleLabel.setWrapText(true);
        Label priceLabel = new Label(formatPrice(item.currentBid()));
        priceLabel.getStyleClass().add("card-sm-price");
        Label statusLabel = new Label();
        if (item.isLive()) {
            statusLabel.setText("⏱  LIVE");
            statusLabel.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 11px; -fx-font-weight: bold;");
            timerLabels.put(item.id() + "_sm", statusLabel);
        } else {
            statusLabel.setText("Ended");
            statusLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        }
        body.getChildren().addAll(categoryLabel, titleLabel, priceLabel, statusLabel);

        VBox footer = new VBox();
        footer.getStyleClass().add("card-sm-footer");
        Button bidBtn = new Button("Place Bid");
        bidBtn.getStyleClass().add("btn-bid-sm");
        bidBtn.setMaxWidth(Double.MAX_VALUE);
        bidBtn.setOnAction(e -> handlePlaceBid(item.id()));
        footer.getChildren().add(bidBtn);

        card.getChildren().addAll(imgPane, body, footer);
        return card;
    }

    private void startCountdownTimers() {
        timerScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ubid-timer");
            t.setDaemon(true);
            return t;
        });
        timerScheduler.scheduleAtFixedRate(() -> {
            List<AuctionItem> allItems = new ArrayList<>(getMockHotAuctions());
            allItems.addAll(getMockAllAuctions());
            for (AuctionItem item : allItems) {
                if (!item.isLive() || item.endTime() == null) continue;
                long secondsLeft = java.time.Duration.between(
                        LocalDateTime.now(), item.endTime()).getSeconds();
                String display;
                if (secondsLeft <= 0) {
                    display = "Auction Ended";
                } else {
                    long h = secondsLeft / 3600;
                    long m = (secondsLeft % 3600) / 60;
                    long s = secondsLeft % 60;
                    display = String.format("%02d:%02d:%02d", h, m, s);
                }
                String fd = display;
                Label hot = timerLabels.get(item.id());
                if (hot != null) Platform.runLater(() -> hot.setText(fd));
                Label sm = timerLabels.get(item.id() + "_sm");
                if (sm != null) Platform.runLater(() -> sm.setText("⏱  " + fd));
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void buildAvatarMenu() {
        avatarMenu = new ContextMenu();
        avatarMenu.getStyleClass().add("dark-context-menu");

        MenuItem miProfile = new MenuItem("Profile Information");
        MenuItem miMyAuctions = new MenuItem("My Auctions");
        MenuItem miSettings = new MenuItem("Settings");
        MenuItem miLogout = new MenuItem("Log Out");
        miLogout.getStyleClass().add("menu-danger");

        miProfile.setOnAction(e -> showInfoPlaceholder("Profile Information"));
        miMyAuctions.setOnAction(e -> showInfoPlaceholder("My Auctions"));
        miSettings.setOnAction(e -> showInfoPlaceholder("Settings"));
        miLogout.setOnAction(e -> {
            if (loginCoordinator != null) loginCoordinator.performLogout();
        });

        avatarMenu.getItems().addAll(
                miProfile, miMyAuctions, miSettings, new SeparatorMenuItem(), miLogout);
    }

    private static void showInfoPlaceholder(String title) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(title);
        a.setTitle("UBid");
        a.setContentText("Đang được ghép vào các màn hình chi tiết trong đồ án.");
        a.show();
    }

    private void refreshLoginUiChrome() {
        boolean logged = UserSession.getInstance().isLoggedIn();
        btnLogInProminent.setVisible(!logged);
        btnLogInProminent.setManaged(!logged);

        btnUserAvatar.setVisible(logged);
        btnUserAvatar.setManaged(logged);
        if (logged) {
            updateAvatarGraphic();
            User u = UserSession.getInstance().getCurrentUser();
            updateProfileHeroMonogram(u.getUsername(), u.getFullName());
        }

        refreshProfileTabContent(logged);

        if (bottomNavCategories.isSelected()) {
            Platform.runLater(this::scrollCategoriesIntoViewIfNeeded);
        }
    }

    private void scrollCategoriesIntoViewIfNeeded() {
        ScrollPane sp = homeScrollPane;
        if (sp == null || categoryChipsContainer == null) return;
        double y = categoryChipsContainer.localToScene(0, 0).getY()
                - sp.localToScene(0, 0).getY();
        sp.setVvalue(Math.min(1, Math.max(0, y / Math.max(1, sp.getContent().getBoundsInLocal().getHeight()
                - sp.getViewportBounds().getHeight()))));
    }

    private void updateAvatarGraphic() {
        User u = UserSession.getInstance().getCurrentUser();
        String monogram = (u.getFullName() != null && !u.getFullName().isBlank())
                ? firstLetter(u.getFullName())
                : firstLetter(u.getUsername());

        StackPane inner = new StackPane();
        inner.getStyleClass().add("avatar-inner-fill");
        inner.setMinSize(AVATAR_SIZE - 6, AVATAR_SIZE - 6);
        inner.setMaxSize(AVATAR_SIZE - 6, AVATAR_SIZE - 6);

        boolean usedImage = false;
        if (EXTERNAL_AVATAR_URL != null && !EXTERNAL_AVATAR_URL.isBlank()) {
            try {
                Image img = new Image(EXTERNAL_AVATAR_URL, AVATAR_SIZE - 8, AVATAR_SIZE - 8, true, true, true);
                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setSmooth(true);
                    iv.setPreserveRatio(true);
                    inner.getChildren().add(iv);
                    usedImage = true;
                }
            } catch (Exception ignored) { /* fallback */ }
        }
        if (!usedImage) {
            Label g = new Label(monogram);
            g.getStyleClass().add("avatar-monogram");
            inner.getChildren().add(g);
        }

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

    private static String firstLetter(String s) {
        String t = s.trim();
        if (t.isEmpty()) return "?";
        return t.substring(0, 1).toUpperCase();
    }

    private void updateProfileHeroMonogram(String username, String fullName) {
        if (profileAvatarGlyph != null)
            profileAvatarGlyph.setText(
                    fullName != null && !fullName.isBlank() ? firstLetter(fullName) : firstLetter(username));
    }

    private void refreshProfileTabContent(boolean logged) {
        if (profileTitleLabel == null) return;
        if (logged) {
            User u = UserSession.getInstance().getCurrentUser();
            profileTitleLabel.setText(u.getFullName() != null && !u.getFullName().isBlank()
                    ? u.getFullName()
                    : u.getUsername());
            profileHintLabel.setText("Quyền: " + u.getRole() + " · " + u.getEmail());
            profileTabLoginButton.setVisible(false);
            profileTabLoginButton.setManaged(false);
            profileLogoutButton.setVisible(true);
            profileLogoutButton.setManaged(true);
            updateProfileHeroMonogram(u.getUsername(), u.getFullName());
        } else {
            profileTitleLabel.setText("Chưa đăng nhập");
            profileHintLabel.setText("Đăng nhập để xem hồ sơ, đấu giá của bạn và cài đặt tài khoản.");
            profileTabLoginButton.setVisible(true);
            profileTabLoginButton.setManaged(true);
            profileLogoutButton.setVisible(false);
            profileLogoutButton.setManaged(false);
            profileAvatarGlyph.setText("👤");
        }
    }

    private void onBottomTabSwitch(ToggleButton sel) {
        if (sel == bottomNavProfile) {
            profileOverlay.setVisible(true);
            profileOverlay.setManaged(true);
            homeScrollPane.setVisible(false);
            homeScrollPane.setManaged(false);
            refreshProfileTabContent(UserSession.getInstance().isLoggedIn());
        } else {
            profileOverlay.setVisible(false);
            profileOverlay.setManaged(false);
            homeScrollPane.setVisible(true);
            homeScrollPane.setManaged(true);
            if (sel == bottomNavCategories) {
                Platform.runLater(this::scrollCategoriesIntoViewIfNeeded);
            }
        }
    }

    @FXML
    private void handleNavHome() {
        bottomNavHome.setSelected(true);
    }

    @FXML
    private void handleNavCategories() {
        bottomNavCategories.setSelected(true);
    }

    @FXML
    private void handleNavResults() {
        showInfoPlaceholder("Results");
    }

    @FXML
    private void handleSearch() {
        searchField.requestFocus();
    }

    @FXML
    private void handleSearchQuery() {
        String q = searchField.getText().trim();
        if (!q.isEmpty()) {
            System.out.println("[Search] " + q);
        }
    }

    @FXML
    private void handleNotifications() {
        showInfoPlaceholder("Notifications");
    }

    @FXML
    private void handleLogin() {
        if (loginCoordinator == null) bootstrapCoordinatorIfPossible();
        if (loginCoordinator != null) loginCoordinator.openLoginWindow();
    }

    @FXML
    private void handleLogoutFromProfile() {
        if (loginCoordinator == null) bootstrapCoordinatorIfPossible();
        if (loginCoordinator != null) loginCoordinator.performLogout();
        bottomNavHome.setSelected(true);
    }

    @FXML
    private void handleAvatarPressed() {
        if (avatarMenu != null) {
            avatarMenu.show(btnUserAvatar, Side.BOTTOM, 0, 0);
        }
    }

    @FXML
    private void handleCategoryAll() {
        setActiveChip(chipAll);
        loadAllAuctions();
    }

    @FXML
    private void handleCategoryFilter() {
        System.out.println("[Category] chip");
    }

    private void handleFilterChange() {
        System.out.println("[Filter] " + filterDropdown.getValue());
    }

    private void handlePlaceBid(String auctionId) {
        if (!UserSession.getInstance().isLoggedIn()) {
            showLoginRequiredDialog();
            return;
        }
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("UBid");
        a.setHeaderText("Đặt giá");
        a.setContentText("Luồng đấu giá cho " + auctionId + " — ghép ItemDetail / BidDialog tại đây.");
        a.show();
    }

    private void showLoginRequiredDialog() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("UBid");
        a.setHeaderText(null);
        a.setContentText("Vui lòng đăng nhập để tiếp tục");
        ButtonType login = new ButtonType("Đăng nhập", ButtonBar.ButtonData.OK_DONE);
        ButtonType close = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(login, close);
        a.showAndWait().filter(response -> response == login)
                .ifPresent(r -> handleLogin());
    }

    private void setActiveChip(Button newActive) {
        if (activeChipButton != null) {
            activeChipButton.getStyleClass().remove("chip-active");
            if (!activeChipButton.getStyleClass().contains("chip")) {
                activeChipButton.getStyleClass().add("chip");
            }
        }
        newActive.getStyleClass().remove("chip");
        if (!newActive.getStyleClass().contains("chip-active")) {
            newActive.getStyleClass().add("chip-active");
        }
        activeChipButton = newActive;
    }

    private String formatPrice(double price) {
        if (price >= 1_000_000) {
            return String.format("$%.2fM", price / 1_000_000);
        }
        return String.format("$%,.0f", price);
    }

    public void shutdown() {
        if (timerScheduler != null && !timerScheduler.isShutdown()) {
            timerScheduler.shutdownNow();
        }
    }
}
