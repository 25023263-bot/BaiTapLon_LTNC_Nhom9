package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import java.util.logging.Logger;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.AuctionObserver;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.ServiceLocator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.notification.NotificationService;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator.HomeLoginCoordinator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator.ItemDetailCoordinator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator.SellerItemDetailCoordinator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.navigation.HomeOverlayManager;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.navigation.HomeOverlayManager.Screen;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter.AdminPanelPresenter;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter.AdminPanelView;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter.HomeCatalogPresenter;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter.HomeCatalogView;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter.HomeNotificationPresenter;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter.SellerProductsHost;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter.SellerProductsPresenter;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter.SellerProductsView;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter.ProfilePresenter;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter.ProfileView;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.mapper.AuctionCardMapper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model.AuctionCardModel;

import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * HomeController — quản lý toàn bộ giao diện trang chủ.
 *
 * Các overlay trong mainStack:
 *   1. homeScrollPane        — trang chủ chính (mặc định)
 *   2. profileOverlay        — tab "Cá nhân"
 *   3. myProductsOverlay     — tab "Sản phẩm của tôi" (Seller xem/quản lý SP)
 *   4. sellerTermsOverlay    — điều khoản khi Buyer muốn nâng cấp thành Seller
 *   5. listProductOverlay    — form đăng bán sản phẩm mới
 */
public class HomeController implements Initializable, AuctionObserver {

    private static final Logger LOG = Logger.getLogger(HomeController.class.getName());

    private static final int AVATAR_SIZE = 40;
    private static final String EXTERNAL_AVATAR_URL = "";

    // ── Existing FXML bindings ────────────────────────────────────────────────

    @FXML private BorderPane rootPane;
    @FXML private StackPane mainStack;
    @FXML private ScrollPane homeScrollPane;
    @FXML private HBox hotCardsContainer;
    @FXML private GridPane allProductsGrid;
    @FXML private HBox categoryChipsContainer;
    @FXML private TextField searchField;
    @FXML private Label resultCountLabel;
    @FXML private Button chipAll;
    @FXML private Button btnLogInProminent;
    @FXML private Button btnUserAvatar;

    // Profile overlay
    @FXML private StackPane profileOverlay;
    @FXML private ScrollPane profileScrollPane;
    @FXML private VBox guestProfilePane;
    @FXML private Label profileTitleLabel;
    @FXML private Label profileHintLabel;
    @FXML private Label profileAvatarGlyph;
    @FXML private Button profileTabLoginButton;
    @FXML private Button profileLogoutButton;
    @FXML private VBox  profileInfoSection;
    @FXML private Label infoFullName;
    @FXML private Label infoEmail;
    @FXML private Label infoPhone;
    @FXML private Label infoRole;
    @FXML private Label infoCreatedAt;
    @FXML private Region walletDivider;
    @FXML private VBox  profileWalletSection;
    @FXML private Label walletBalanceLabel;
    @FXML private Label walletTypeLabel;
    @FXML private VBox  depositOverlay;
    @FXML private VBox  depositStatusBox;
    @FXML private Label depositStatusIcon;
    @FXML private Label depositStatusText;
    @FXML private TextField depositAmountField;
    @FXML private Label depositAmountHint;
    @FXML private Button btnConfirmDeposit;

    // ── Admin Panel overlay ──────────────────────────────────────────────────
    @FXML private StackPane adminOverlay;
    @FXML private Label     adminSubtitleLabel;
    @FXML private Label     adminLevelBadge;
    @FXML private Button    adminTabUsers;
    @FXML private Button    adminTabAuctions;
    @FXML private Region    adminTabUsersIndicator;
    @FXML private Region    adminTabAuctionsIndicator;
    @FXML private VBox      adminUsersPanel;
    @FXML private VBox      adminAuctionsPanel;
    @FXML private TextField adminUserSearchField;
    @FXML private ComboBox<String> adminUserRoleFilter;
    @FXML private VBox      adminUsersList;
    @FXML private VBox      adminUsersEmpty;
    @FXML private TextField adminAuctionSearchField;
    @FXML private ComboBox<String> adminAuctionStatusFilter;
    @FXML private VBox      adminAuctionsList;
    @FXML private VBox      adminAuctionsEmpty;

    // Bottom nav — "Danh mục" đã đổi thành "Sản phẩm của tôi"
    @FXML private ToggleButton bottomNavHome;
    @FXML private ToggleButton bottomNavMyProducts;   // Trước là bottomNavCategories
    @FXML private ToggleButton bottomNavProfile;

    // ── "Sản phẩm của tôi" overlay (Seller) ─────────────────────────────────

    @FXML private StackPane myProductsOverlay;
    @FXML private Label myProductsSubtitle;
    @FXML private Button btnListNewProduct;
    @FXML private VBox myProductsList;

    // -- "Ket qua dau gia" overlay --

    @FXML private StackPane resultsOverlay;
    @FXML private Label     resultsSubtitle;
    @FXML private VBox      resultsList;

    // ── "Điều khoản Người bán" overlay (Buyer muốn nâng cấp) ─────────────────

    @FXML private StackPane sellerTermsOverlay;
    @FXML private CheckBox upgradeTermsMerchandise;
    @FXML private CheckBox upgradeTermsContent;
    @FXML private CheckBox upgradeTermsPrivacy;
    @FXML private Label upgradeTermsMerchandiseError;
    @FXML private Label upgradeTermsContentError;
    @FXML private Label upgradeTermsPrivacyError;

    // ── "Đăng bán sản phẩm" overlay (form) ──────────────────────────────────

    @FXML private StackPane listProductOverlay;
    @FXML private TextField listProductTitleField;
    @FXML private ComboBox<String> listProductCategoryCombo;
    @FXML private TextArea listProductDescArea;
    @FXML private TextField listProductPriceField;
    @FXML private DatePicker listProductEndDate;
    @FXML private ComboBox<String> listProductEndHour;    // 00 – 23
    @FXML private ComboBox<String> listProductEndMinute;  // 00, 05, 10 … 55
    @FXML private Label     lblEndTimePreview;            // hiện "Kết thúc: 25/05 lúc 14:30"
    @FXML private StackPane imageUploadBox;
    @FXML private Label listProductTitleError;
    @FXML private Label listProductCategoryError;
    @FXML private Label listProductDescError;
    @FXML private Label listProductPriceError;
    @FXML private Label listProductDateError;
    @FXML private Label listProductImageError;
    @FXML private Label submitProductError;

    // ── Notification bell & panel ─────────────────────────────────────────────
    @FXML private Button    btnBell;        // bell icon (dùng để set style khi panel mở)
    @FXML private Label     lblBellBadge;   // badge đỏ số chưa đọc
    @FXML private StackPane notifOverlay;   // LỚP 7: backdrop + panel
    @FXML private VBox      notifList;      // container danh sách thông báo
    @FXML private Button    btnMarkAllRead; // "Đánh dấu tất cả đã đọc"

    // ── Trạng thái nội bộ ────────────────────────────────────────────────────

    /** NotificationService — lấy từ ServiceLocator (singleton) */
    private final NotificationService notifService =
            ServiceLocator.getInstance().getNotificationService();

    private HomeLoginCoordinator loginCoordinator;
    private ItemDetailCoordinator itemDetailCoordinator;
    private SellerItemDetailCoordinator sellerItemDetailCoordinator;
    private ContextMenu avatarMenu;

    private HomeOverlayManager overlayManager;
    private HomeCatalogPresenter catalogPresenter;
    private ProfilePresenter profilePresenter;
    private SellerProductsPresenter sellerProductsPresenter;
    private AdminPanelPresenter adminPresenter;
    private HomeNotificationPresenter notificationPresenter;

    private boolean suppressBottomNavListener;

    // ─────────────────────────────────────────────────────────────────────────
    // Khởi tạo
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        overlayManager = new HomeOverlayManager(
                homeScrollPane, profileOverlay, myProductsOverlay, sellerTermsOverlay,
                listProductOverlay, resultsOverlay, adminOverlay, notifOverlay);

        catalogPresenter = new HomeCatalogPresenter();
        catalogPresenter.bind(
                new HomeCatalogView(hotCardsContainer, allProductsGrid, searchField, resultCountLabel, chipAll),
                this::handlePlaceBid,
                () -> catalogPresenter.loadResultAuctions(resultsList, resultsSubtitle));
        catalogPresenter.loadHotAuctions();
        catalogPresenter.loadAllAuctions();
        catalogPresenter.startTimers();

        sellerProductsPresenter = new SellerProductsPresenter();
        sellerProductsPresenter.bind(
                new SellerProductsView(
                        bottomNavHome, bottomNavMyProducts, myProductsList,
                        sellerTermsOverlay,
                        upgradeTermsMerchandise, upgradeTermsContent, upgradeTermsPrivacy,
                        upgradeTermsMerchandiseError, upgradeTermsContentError, upgradeTermsPrivacyError,
                        listProductOverlay,
                        listProductTitleField, listProductCategoryCombo, listProductDescArea,
                        listProductPriceField, listProductEndDate, listProductEndHour, listProductEndMinute,
                        lblEndTimePreview, imageUploadBox,
                        listProductTitleError, listProductCategoryError, listProductDescError,
                        listProductPriceError, listProductDateError, listProductImageError,
                        submitProductError
                ),
                new SellerProductsHost(
                        () -> overlayManager.show(Screen.MY_PRODUCTS),
                        () -> overlayManager.show(Screen.SELLER_TERMS),
                        () -> overlayManager.show(Screen.LIST_PRODUCT),
                        () -> overlayManager.show(Screen.HOME),
                        () -> bottomNavHome.setSelected(true),
                        catalogPresenter::refreshAll,
                        this::handleLogin,
                        this::refreshLoginUiChrome,
                        (item, onChanged) -> {
                            bootstrapCoordinatorIfPossible();
                            if (sellerItemDetailCoordinator != null) {
                                sellerItemDetailCoordinator.open(item, onChanged);
                            }
                        },
                        () -> rootPane.getScene() != null
                                ? (Stage) rootPane.getScene().getWindow() : null,
                        () -> rootPane.getScene() != null ? rootPane.getScene().getWindow() : null,
                        this::bootstrapCoordinatorIfPossible
                )
        );
        sellerProductsPresenter.initForm();

        profilePresenter = new ProfilePresenter();
        profilePresenter.bind(
                new ProfileView(
                        profileScrollPane, guestProfilePane,
                        profileTitleLabel, profileHintLabel, profileAvatarGlyph,
                        profileTabLoginButton, profileLogoutButton,
                        profileInfoSection,
                        infoFullName, infoEmail, infoPhone, infoRole, infoCreatedAt,
                        walletDivider, profileWalletSection,
                        walletBalanceLabel, walletTypeLabel,
                        depositOverlay, depositStatusBox,
                        depositStatusIcon, depositStatusText,
                        depositAmountField, depositAmountHint, btnConfirmDeposit
                ),
                () -> {
                    bottomNavHome.setSelected(true);
                    overlayManager.show(Screen.HOME);
                }
        );

        adminPresenter = new AdminPanelPresenter();
        adminPresenter.bind(new AdminPanelView(
                adminOverlay, adminSubtitleLabel, adminTabUsers, adminTabAuctions,
                adminTabUsersIndicator, adminTabAuctionsIndicator,
                adminUsersPanel, adminAuctionsPanel,
                adminUserSearchField, adminUserRoleFilter, adminUsersList, adminUsersEmpty,
                adminAuctionSearchField, adminAuctionStatusFilter, adminAuctionsList, adminAuctionsEmpty));

        notificationPresenter = new HomeNotificationPresenter(notifService);
        notificationPresenter.bind(
                lblBellBadge, notifList, notifOverlay,
                () -> showInfoPlaceholder("Vui lòng đăng nhập để xem thông báo"),
                userId -> overlayManager.showNotificationPanel());

        ToggleGroup tabs = new ToggleGroup();
        bottomNavHome.setToggleGroup(tabs);
        bottomNavMyProducts.setToggleGroup(tabs);
        bottomNavProfile.setToggleGroup(tabs);
        // KHÔNG dùng selectedToggleProperty listener vì ToggleGroup deselect nút khi
        // bấm lại nút đang selected → sel == null → listener bỏ qua → không làm gì.
        // Thay vào đó mỗi tab có onAction riêng trong FXML (handleNavHome,
        // handleMyProductsTab, handleProfileTab) để luôn chạy dù selected hay không.
        bottomNavHome.setSelected(true);

        rootPane.sceneProperty().addListener((obs, os, scene) -> {
            if (scene == null || loginCoordinator != null) return;
            if (scene.getWindow() instanceof Stage stage) {
                loginCoordinator = new HomeLoginCoordinator(stage);
                loginCoordinator.setOnAuthStateChanged(this::refreshLoginUiChrome);
                itemDetailCoordinator = new ItemDetailCoordinator(stage);
                sellerItemDetailCoordinator = new SellerItemDetailCoordinator(stage);
                refreshLoginUiChrome();
            }
        });
        Platform.runLater(this::bootstrapCoordinatorIfPossible);
        buildAvatarMenu();

        ServiceLocator.getInstance().getAuctionHouse().addObserver(this);
        notificationPresenter.start();
    }

    private void bootstrapCoordinatorIfPossible() {
        if ((loginCoordinator != null && itemDetailCoordinator != null)
                || rootPane.getScene() == null) return;
        var scene = rootPane.getScene();
        if (scene.getWindow() instanceof Stage stage) {
            loginCoordinator = new HomeLoginCoordinator(stage);
            loginCoordinator.setOnAuthStateChanged(this::refreshLoginUiChrome);
            itemDetailCoordinator = new ItemDetailCoordinator(stage);
            sellerItemDetailCoordinator = new SellerItemDetailCoordinator(stage);
            refreshLoginUiChrome();
        }
    }

    /** Gọi từ FXML khi bấm tab "Sản phẩm của tôi".
     *  Dùng setSelected(true) để giữ nút luôn active (ToggleGroup không tự deselect). */
    @FXML private void handleMyProductsTab() {
        suppressBottomNavListener = true;
        try { bottomNavMyProducts.setSelected(true); }
        finally { suppressBottomNavListener = false; }
        sellerProductsPresenter.onMyProductsTabSelected();
    }

    /** Gọi từ FXML khi bấm tab "Cá nhân". */
    @FXML private void handleProfileTab() {
        suppressBottomNavListener = true;
        try { bottomNavProfile.setSelected(true); }
        finally { suppressBottomNavListener = false; }
        if (UserSession.getInstance().isAdmin()) {
            overlayManager.show(Screen.ADMIN);
            adminPresenter.refresh();
        } else {
            overlayManager.show(Screen.PROFILE);
            profilePresenter.refresh(UserSession.getInstance().isLoggedIn());
        }
    }

    private void onBottomTabSwitch(ToggleButton sel) {
        if (sel == bottomNavProfile) {
            if (UserSession.getInstance().isAdmin()) {
                overlayManager.show(Screen.ADMIN);
                adminPresenter.refresh();
            } else {
                overlayManager.show(Screen.PROFILE);
                profilePresenter.refresh(UserSession.getInstance().isLoggedIn());
            }
        } else if (sel == bottomNavMyProducts) {
            sellerProductsPresenter.onMyProductsTabSelected();
        } else {
            overlayManager.show(Screen.HOME);
        }
    }

    // Seller / đăng bán — delegate SellerProductsPresenter
    @FXML private void handleListNewProduct() { sellerProductsPresenter.openListProductForm(); }
    @FXML private void handleBackToMyProducts() { sellerProductsPresenter.backToMyProducts(); }
    @FXML private void handleImageUpload() { sellerProductsPresenter.uploadImage(); }
    @FXML private void handleSubmitProduct() { sellerProductsPresenter.submitProduct(); }
    @FXML private void handleAcceptSellerTerms() { sellerProductsPresenter.acceptSellerTerms(); }
    @FXML private void handleCancelSellerTerms() { sellerProductsPresenter.cancelSellerTerms(); }
    @FXML private void onViewMerchandiseTermsUpgrade() { sellerProductsPresenter.viewMerchandiseTerms(); }
    @FXML private void onViewContentTermsUpgrade() { sellerProductsPresenter.viewContentTerms(); }
    @FXML private void onViewPrivacyTermsUpgrade() { sellerProductsPresenter.viewPrivacyTerms(); }


    // ─────────────────────────────────────────────────────────────────────────
    // Avatar menu & Auth chrome
    // ─────────────────────────────────────────────────────────────────────────

    private void buildAvatarMenu() {
        avatarMenu = new ContextMenu();
        avatarMenu.getStyleClass().add("dark-context-menu");

        MenuItem miLogout = new MenuItem("Đăng xuất");
        miLogout.getStyleClass().add("menu-danger");
        miLogout.setOnAction(e -> {
            if (loginCoordinator != null) loginCoordinator.performLogout();
        });

        avatarMenu.getItems().add(miLogout);
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
            String username  = UserSession.getInstance().getCurrentUsername();
            String fullName  = UserSession.getInstance().getCurrentFullName();
            profilePresenter.updateHeroMonogram(username, fullName);
        }
        profilePresenter.refresh(logged);
        catalogPresenter.refreshAll();
        if (bottomNavMyProducts.isSelected()) {
            Platform.runLater(sellerProductsPresenter::onMyProductsTabSelected);
        }
    }

    private void updateAvatarGraphic() {
        String username = UserSession.getInstance().getCurrentUsername();
        String fullName = UserSession.getInstance().getCurrentFullName();
        String monogram = (fullName != null && !fullName.isBlank())
                ? firstLetter(fullName)
                : firstLetter(username);

        StackPane inner = new StackPane();
        inner.getStyleClass().add("avatar-inner-fill");
        inner.setMinSize(AVATAR_SIZE - 6, AVATAR_SIZE - 6);
        inner.setMaxSize(AVATAR_SIZE - 6, AVATAR_SIZE - 6);

        boolean usedImage = false;
        if (EXTERNAL_AVATAR_URL != null && !EXTERNAL_AVATAR_URL.isBlank()) {
            try {
                Image img = new Image(EXTERNAL_AVATAR_URL,
                        AVATAR_SIZE - 8, AVATAR_SIZE - 8, true, true, true);
                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setSmooth(true);
                    iv.setPreserveRatio(true);
                    inner.getChildren().add(iv);
                    usedImage = true;
                }
            } catch (Exception ignored) {}
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

    @FXML private void handleOpenDeposit() { profilePresenter.openDeposit(); }
    @FXML private void handleCloseDeposit() { profilePresenter.closeDeposit(); }
    @FXML private void handleProfileBackdropClick() { profilePresenter.handleBackdropClick(); }
    @FXML
    private void handleQuickDeposit(javafx.event.ActionEvent event) {
        if (!(event.getSource() instanceof Button btn)) return;
        String raw = btn.getUserData() != null ? btn.getUserData().toString() : "0";
        profilePresenter.quickDeposit(raw);
    }
    @FXML private void handleCopyAccountNumber() { profilePresenter.copyAccountNumber(); }
    @FXML private void handleConfirmDeposit() { profilePresenter.confirmDeposit(); }

    // ─────────────────────────────────────────────────────────────────────────
    // FXML Handlers — navbar và misc
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void handleNavHome() {
        // FIX: Gọi trực tiếp overlayManager thay vì chỉ setSelected().
        //
        // Vấn đề cũ: bottomNavHome.setSelected(true) không làm gì nếu nó
        // ĐÃ đang được selected (listener của ToggleGroup không fire khi
        // giá trị không đổi). Điều này xảy ra sau khi user bấm "Kết quả" —
        // tab dưới vẫn ở trạng thái HOME nhưng overlay đang hiện RESULTS.
        // Bấm lại "Trang chủ" trên thanh nav → setSelected(true) → no-op.
        //
        // Giải pháp: luôn show HOME trực tiếp, rồi đồng bộ lại trạng thái
        // bottom nav (có suppress để listener không fire thêm lần nữa).
        overlayManager.show(Screen.HOME);
        suppressBottomNavListener = true;
        try {
            bottomNavHome.setSelected(true);
        } finally {
            suppressBottomNavListener = false;
        }
    }

    @FXML private void handleNavResults() {
        suppressBottomNavListener = true;
        try {
            // FIX: Bỏ chọn tab hiện tại trong ToggleGroup.
            //
            // Vấn đề cũ: bottomNavHome vẫn được "selected" sau khi mở RESULTS.
            // Khi user bấm bottom "Trang chủ" → setSelected(true) → đã selected
            // → listener KHÔNG fire → màn hình không về HOME được.
            //
            // Giải pháp: clear selection trước khi chuyển sang RESULTS.
            // Khi user bấm bottom "Trang chủ", nó sẽ thay đổi từ null → selected
            // → listener fire → onBottomTabSwitch → overlayManager.show(HOME). ✓
            bottomNavHome.getToggleGroup().selectToggle(null);
            overlayManager.show(Screen.RESULTS);
            catalogPresenter.loadResultAuctions(resultsList, resultsSubtitle);
        } finally {
            suppressBottomNavListener = false;
        }
    }

    @FXML private void handleSearch()     { searchField.requestFocus(); }
    @FXML private void handleSearchQuery() { catalogPresenter.search(); }
    @FXML private void handleNotifications() { notificationPresenter.openPanel(); }
    @FXML private void handleCloseNotifications() { notificationPresenter.closePanel(); }
    @FXML private void handleMarkAllRead() { notificationPresenter.markAllRead(); }
    @FXML private void handleNotifBackdropClick(javafx.scene.input.MouseEvent e) {
        notificationPresenter.handleBackdropClick(e);
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
        if (avatarMenu != null) avatarMenu.show(btnUserAvatar, Side.BOTTOM, 0, 0);
    }

    @FXML private void handleCategoryAll() { catalogPresenter.showAllCategories(); }

    @FXML
    private void handleCategoryFilter(javafx.event.ActionEvent event) {
        if (event.getSource() instanceof Button clickedChip) {
            catalogPresenter.filterByCategory(clickedChip);
        }
    }

    private void handlePlaceBid(String auctionId) {
        if (!UserSession.getInstance().isLoggedIn()) {
            showLoginRequiredDialog();
            return;
        }
        if (itemDetailCoordinator == null) bootstrapCoordinatorIfPossible();
        AuctionCardModel item = catalogPresenter.findById(auctionId);
        if (item == null) { showInfoPlaceholder("Đấu giá " + auctionId); return; }

        // ── Chặn người bán tự đặt giá sản phẩm của mình ─────────────────────
        //
        // Tại sao kiểm tra lại ở đây, dù UI đã ẩn nút "Đặt giá"?
        // → Nguyên tắc "Defense in Depth" (bảo vệ nhiều lớp):
        //   UI chỉ là lớp hiển thị, có thể bị bypass (race condition, bug render...).
        //   Logic nghiệp vụ PHẢI được kiểm tra ở tầng controller/service,
        //   không được chỉ dựa vào việc ẩn/hiện nút trên UI.
        // ── Phân luồng: chủ sản phẩm → SellerItemDetail; buyer → ItemDetail ──
        //
        // FIX: Trước đây nếu owner bấm vào card của mình, code hiện warning
        // "không thể đặt giá" rồi return. Điều này khiến seller không thể
        // xem/quản lý sản phẩm từ trang chủ.
        //
        // Giải pháp: khi phát hiện user là chủ sản phẩm, mở SellerItemDetail
        // thay vì chặn. Defense-in-depth vẫn giữ nguyên vì SellerItemDetail
        // không có chức năng đặt giá.
        if (UserSession.getInstance().getCurrentUserId() == item.sellerId()) {
            if (sellerItemDetailCoordinator == null) bootstrapCoordinatorIfPossible();
            if (sellerItemDetailCoordinator != null) {
                sellerItemDetailCoordinator.open(item, catalogPresenter::refreshAll);
            }
            return;
        }

        if (itemDetailCoordinator != null) {
            itemDetailCoordinator.openForAuction(item);
            catalogPresenter.refreshAll();
        }
    }

    private void showLoginRequiredDialog() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("UBid");
        a.setHeaderText(null);
        a.setContentText("Vui lòng đăng nhập để tiếp tục");
        ButtonType login = new ButtonType("Đăng nhập", ButtonBar.ButtonData.OK_DONE);
        ButtonType close = new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(login, close);
        a.showAndWait()
                .filter(response -> response == login)
                .ifPresent(r -> handleLogin());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AuctionObserver — nhận sự kiện từ AuctionHouse và refresh UI
    //
    // Các method này được gọi từ background thread (AuctionScheduler),
    // nên BẮT BUỘC phải dùng Platform.runLater() để chạm vào UI.
    // Không dùng Platform.runLater() → crash hoặc hành vi không xác định.
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onAuctionClosed(
            com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem item,
            Integer winnerId) {
        Platform.runLater(() -> {
            catalogPresenter.refreshAll();
            if (!UserSession.getInstance().isLoggedIn()) return;
            int currentUserId = UserSession.getInstance().getCurrentUserId();
            boolean isWinner = winnerId != null && currentUserId == winnerId;
            boolean isSeller = currentUserId == item.getSellerId();
            // Reload profile/wallet nếu user liên quan đến phiên vừa đóng
            if (isWinner || isSeller) {
                profilePresenter.refresh(true);
            }
        });
    }

    @Override
    public void onAuctionStarted(
            com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem item) {
        // Phiên mới chuyển PENDING → ACTIVE → hiện lên trang chủ
        Platform.runLater(catalogPresenter::refreshAll);
    }

    @Override
    public void onNewBid(
            com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem item,
            Bid bid) {
        // Có bid mới → tuỳ chọn refresh giá trên card nếu muốn real-time
        // Tạm để trống — badge thông báo đã được xử lý bởi NotificationService
    }

    @Override
    public void onAuctionCancelled(
            com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem item) {
        // Phiên bị huỷ → xóa khỏi danh sách
        Platform.runLater(catalogPresenter::refreshAll);
    }

    public void shutdown() {
        catalogPresenter.shutdown();
        notificationPresenter.shutdown();
    }

    @FXML private void handleAdminTabUsers() { adminPresenter.showUsersTab(); }
    @FXML private void handleAdminTabAuctions() { adminPresenter.showAuctionsTab(); }
    @FXML private void handleAdminLoadUsers() { adminPresenter.loadUsers(); }
    @FXML private void handleAdminLoadAuctions() { adminPresenter.loadAuctions(); }
    @FXML private void handleAdminUserSearch(javafx.event.Event event) { adminPresenter.searchUsers(); }
    @FXML private void handleAdminAuctionSearch(javafx.event.Event event) { adminPresenter.searchAuctions(); }
}