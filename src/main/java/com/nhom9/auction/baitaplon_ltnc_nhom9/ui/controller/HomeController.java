package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import java.util.logging.Logger;
import com.nhom9.auction.baitaplon_ltnc_nhom9.client.SocketClient;
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

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * HomeController — wiring va navigation trang chu.
 *
 * Trach nhiem sau khi tach:
 *   - Khoi tao va ket noi cac Presenter voi View objects (FXML fields)
 *   - Quan ly dieu huong giua cac overlay qua HomeOverlayManager
 *   - Xu ly cac FXML @handler don gian (delegate cho Presenter/Manager)
 *   - Bootstrap coordinators khi Stage san sang
 *
 * Logic da duoc tach ra:
 *   - HomeAvatarManager  -- avatar button, monogram, logout menu
 *   - HomeBidHandler     -- routing khi user click vao auction card
 */
public class HomeController implements Initializable {

    private static final Logger LOG = Logger.getLogger(HomeController.class.getName());

    // -- FXML bindings --------------------------------------------------------

    @FXML private BorderPane rootPane;
    @FXML private StackPane  mainStack;
    @FXML private ScrollPane homeScrollPane;
    @FXML private HBox       hotCardsContainer;
    @FXML private GridPane   allProductsGrid;
    @FXML private HBox       categoryChipsContainer;
    @FXML private TextField  searchField;
    @FXML private Label      resultCountLabel;
    @FXML private Button     chipAll;
    @FXML private Button     btnLogInProminent;
    @FXML private Button     btnUserAvatar;

    // Profile overlay
    @FXML private StackPane  profileOverlay;
    @FXML private ScrollPane profileScrollPane;
    @FXML private VBox       guestProfilePane;
    @FXML private Label      profileTitleLabel;
    @FXML private Label      profileHintLabel;
    @FXML private Label      profileAvatarGlyph;
    @FXML private Button     profileTabLoginButton;
    @FXML private Button     profileLogoutButton;
    @FXML private VBox       profileInfoSection;
    @FXML private Label      infoFullName;
    @FXML private Label      infoEmail;
    @FXML private Label      infoPhone;
    @FXML private Label      infoRole;
    @FXML private Label      infoCreatedAt;
    @FXML private Region     walletDivider;
    @FXML private VBox       profileWalletSection;
    @FXML private Label      walletBalanceLabel;
    @FXML private Label      walletTypeLabel;
    @FXML private VBox       depositOverlay;
    @FXML private VBox       depositStatusBox;
    @FXML private Label      depositStatusIcon;
    @FXML private Label      depositStatusText;
    @FXML private TextField  depositAmountField;
    @FXML private Label      depositAmountHint;
    @FXML private Button     btnConfirmDeposit;

    // Admin overlay
    @FXML private StackPane  adminOverlay;
    @FXML private Label      adminSubtitleLabel;
    @FXML private Label      adminLevelBadge;
    @FXML private Button     adminTabUsers;
    @FXML private Button     adminTabAuctions;
    @FXML private Region     adminTabUsersIndicator;
    @FXML private Region     adminTabAuctionsIndicator;
    @FXML private VBox       adminUsersPanel;
    @FXML private VBox       adminAuctionsPanel;
    @FXML private TextField  adminUserSearchField;
    @FXML private ComboBox<String> adminUserRoleFilter;
    @FXML private VBox       adminUsersList;
    @FXML private VBox       adminUsersEmpty;
    @FXML private TextField  adminAuctionSearchField;
    @FXML private ComboBox<String> adminAuctionStatusFilter;
    @FXML private VBox       adminAuctionsList;
    @FXML private VBox       adminAuctionsEmpty;

    // Bottom nav
    @FXML private ToggleButton bottomNavHome;
    @FXML private ToggleButton bottomNavMyProducts;
    @FXML private ToggleButton bottomNavProfile;

    // Seller overlay
    @FXML private StackPane myProductsOverlay;
    @FXML private Label     myProductsSubtitle;
    @FXML private Button    btnListNewProduct;
    @FXML private VBox      myProductsList;

    // Results overlay
    @FXML private StackPane resultsOverlay;
    @FXML private Label     resultsSubtitle;
    @FXML private VBox      resultsList;

    // Seller terms overlay
    @FXML private StackPane sellerTermsOverlay;
    @FXML private CheckBox  upgradeTermsMerchandise;
    @FXML private CheckBox  upgradeTermsContent;
    @FXML private CheckBox  upgradeTermsPrivacy;
    @FXML private Label     upgradeTermsMerchandiseError;
    @FXML private Label     upgradeTermsContentError;
    @FXML private Label     upgradeTermsPrivacyError;

    // List product overlay
    @FXML private StackPane  listProductOverlay;
    @FXML private TextField  listProductTitleField;
    @FXML private ComboBox<String> listProductCategoryCombo;
    @FXML private TextArea   listProductDescArea;
    @FXML private TextField  listProductPriceField;
    @FXML private DatePicker listProductEndDate;
    @FXML private ComboBox<String> listProductEndHour;
    @FXML private ComboBox<String> listProductEndMinute;
    @FXML private Label      lblEndTimePreview;
    @FXML private StackPane  imageUploadBox;
    @FXML private Label      listProductTitleError;
    @FXML private Label      listProductCategoryError;
    @FXML private Label      listProductDescError;
    @FXML private Label      listProductPriceError;
    @FXML private Label      listProductDateError;
    @FXML private Label      listProductImageError;
    @FXML private Label      submitProductError;

    // Notification
    @FXML private Button     btnBell;
    @FXML private Label      lblBellBadge;
    @FXML private StackPane  notifOverlay;
    @FXML private VBox       notifList;
    @FXML private Button     btnMarkAllRead;

    // -- Trang thai noi bo ----------------------------------------------------

    private HomeLoginCoordinator        loginCoordinator;
    private ItemDetailCoordinator       itemDetailCoordinator;
    private SellerItemDetailCoordinator sellerItemDetailCoordinator;

    private HomeOverlayManager        overlayManager;
    private HomeCatalogPresenter      catalogPresenter;
    private ProfilePresenter          profilePresenter;
    private SellerProductsPresenter   sellerProductsPresenter;
    private AdminPanelPresenter       adminPresenter;
    private HomeNotificationPresenter notificationPresenter;

    // Cac class moi tach ra
    private HomeAvatarManager avatarManager;
    private HomeBidHandler    bidHandler;

    private boolean suppressBottomNavListener;

    // =========================================================================
    // Khoi tao
    // =========================================================================

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        overlayManager = new HomeOverlayManager(
                homeScrollPane, profileOverlay, myProductsOverlay, sellerTermsOverlay,
                listProductOverlay, resultsOverlay, adminOverlay, notifOverlay);

        initCatalogPresenter();
        initSellerProductsPresenter();
        initProfilePresenter();
        initAdminPresenter();
        initNotificationPresenter();

        // profilePresenter phai duoc init truoc avatarManager
        avatarManager = new HomeAvatarManager(btnLogInProminent, btnUserAvatar, profilePresenter);
        avatarManager.setOnLogoutRequest(this::performLogout);

        bidHandler = new HomeBidHandler(catalogPresenter, this::handleLogin);
        // Coordinators chua co Stage luc nay, se set sau qua bootstrapCoordinatorIfPossible()

        setupBottomNav();
        setupSceneListener();

        Platform.runLater(this::bootstrapCoordinatorIfPossible);

        SocketClient.getInstance().setNotificationHandler(response -> {
            notificationPresenter.onServerNotification();
            catalogPresenter.refreshAll();
        });

        notificationPresenter.start();
    }

    // -- Init tung Presenter --------------------------------------------------

    private void initCatalogPresenter() {
        catalogPresenter = new HomeCatalogPresenter();
        catalogPresenter.bind(
                new HomeCatalogView(hotCardsContainer, allProductsGrid, searchField, resultCountLabel, chipAll),
                this::handlePlaceBid,
                () -> catalogPresenter.loadResultAuctions(resultsList, resultsSubtitle));
        catalogPresenter.loadHotAuctions();
        catalogPresenter.loadAllAuctions();
        catalogPresenter.startTimers();
    }

    private void initSellerProductsPresenter() {
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
                            if (sellerItemDetailCoordinator != null)
                                sellerItemDetailCoordinator.open(item, onChanged);
                        },
                        () -> rootPane.getScene() != null
                                ? (Stage) rootPane.getScene().getWindow() : null,
                        () -> rootPane.getScene() != null ? rootPane.getScene().getWindow() : null,
                        this::bootstrapCoordinatorIfPossible
                )
        );
        sellerProductsPresenter.initForm();
    }

    private void initProfilePresenter() {
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
                () -> { bottomNavHome.setSelected(true); overlayManager.show(Screen.HOME); }
        );
    }

    private void initAdminPresenter() {
        adminPresenter = new AdminPanelPresenter();
        adminPresenter.bind(new AdminPanelView(
                adminOverlay, adminSubtitleLabel, adminTabUsers, adminTabAuctions,
                adminTabUsersIndicator, adminTabAuctionsIndicator,
                adminUsersPanel, adminAuctionsPanel,
                adminUserSearchField, adminUserRoleFilter, adminUsersList, adminUsersEmpty,
                adminAuctionSearchField, adminAuctionStatusFilter, adminAuctionsList, adminAuctionsEmpty));
    }

    private void initNotificationPresenter() {
        notificationPresenter = new HomeNotificationPresenter();
        notificationPresenter.bind(
                lblBellBadge, notifList, notifOverlay,
                () -> showInfoPlaceholder("Vui long dang nhap de xem thong bao"),
                userId -> overlayManager.showNotificationPanel());
    }

    private void setupBottomNav() {
        ToggleGroup tabs = new ToggleGroup();
        bottomNavHome.setToggleGroup(tabs);
        bottomNavMyProducts.setToggleGroup(tabs);
        bottomNavProfile.setToggleGroup(tabs);
        bottomNavHome.setSelected(true);
    }

    private void setupSceneListener() {
        rootPane.sceneProperty().addListener((obs, os, scene) -> {
            if (scene == null || loginCoordinator != null) return;
            if (scene.getWindow() instanceof Stage stage) bootstrapWithStage(stage);
        });
    }

    // -- Coordinator bootstrap (lazy -- can Stage) ----------------------------

    private void bootstrapCoordinatorIfPossible() {
        if (loginCoordinator != null || rootPane.getScene() == null) return;
        if (rootPane.getScene().getWindow() instanceof Stage stage) bootstrapWithStage(stage);
    }

    private void bootstrapWithStage(Stage stage) {
        loginCoordinator            = new HomeLoginCoordinator(stage);
        loginCoordinator.setOnAuthStateChanged(this::refreshLoginUiChrome);
        itemDetailCoordinator       = new ItemDetailCoordinator(stage);
        sellerItemDetailCoordinator = new SellerItemDetailCoordinator(stage);
        bidHandler.setCoordinators(itemDetailCoordinator, sellerItemDetailCoordinator);
        refreshLoginUiChrome();
    }

    // -- Refresh UI sau khi login state thay doi ------------------------------

    private void refreshLoginUiChrome() {
        avatarManager.refresh();
        profilePresenter.refresh(UserSession.getInstance().isLoggedIn());
        catalogPresenter.refreshAll();
        if (bottomNavMyProducts.isSelected())
            Platform.runLater(sellerProductsPresenter::onMyProductsTabSelected);
    }

    private void performLogout() {
        if (loginCoordinator == null) bootstrapCoordinatorIfPossible();
        if (loginCoordinator != null) loginCoordinator.performLogout();
    }

    // =========================================================================
    // FXML Handlers
    // =========================================================================

    @FXML private void handleNavHome() {
        overlayManager.show(Screen.HOME);
        suppressBottomNavListener = true;
        try { bottomNavHome.setSelected(true); }
        finally { suppressBottomNavListener = false; }
    }

    @FXML private void handleMyProductsTab() {
        suppressBottomNavListener = true;
        try { bottomNavMyProducts.setSelected(true); }
        finally { suppressBottomNavListener = false; }
        sellerProductsPresenter.onMyProductsTabSelected();
    }

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

    @FXML private void handleNavResults() {
        suppressBottomNavListener = true;
        try {
            bottomNavHome.getToggleGroup().selectToggle(null);
            overlayManager.show(Screen.RESULTS);
            catalogPresenter.loadResultAuctions(resultsList, resultsSubtitle);
        } finally { suppressBottomNavListener = false; }
    }

    @FXML private void handleLogin() {
        if (loginCoordinator == null) bootstrapCoordinatorIfPossible();
        if (loginCoordinator != null) loginCoordinator.openLoginWindow();
    }

    @FXML private void handleLogoutFromProfile() {
        performLogout();
        bottomNavHome.setSelected(true);
    }

    @FXML
    private void handleAvatarPressed() {
        // TODO: xử lý sau
        System.out.println("Avatar pressed");
    }

    @FXML private void handleSearch()      { searchField.requestFocus(); }
    @FXML private void handleSearchQuery() { catalogPresenter.search(); }
    @FXML private void handleCategoryAll() { catalogPresenter.showAllCategories(); }

    @FXML private void handleCategoryFilter(javafx.event.ActionEvent event) {
        if (event.getSource() instanceof Button clickedChip)
            catalogPresenter.filterByCategory(clickedChip);
    }

    // Delegate sang HomeBidHandler
    private void handlePlaceBid(String auctionId) { bidHandler.handle(auctionId); }

    // Seller
    @FXML private void handleListNewProduct()          { sellerProductsPresenter.openListProductForm(); }
    @FXML private void handleBackToMyProducts()        { sellerProductsPresenter.backToMyProducts(); }
    @FXML private void handleImageUpload()             { sellerProductsPresenter.uploadImage(); }
    @FXML private void handleSubmitProduct()           { sellerProductsPresenter.submitProduct(); }
    @FXML private void handleAcceptSellerTerms()       { sellerProductsPresenter.acceptSellerTerms(); }
    @FXML private void handleCancelSellerTerms()       { sellerProductsPresenter.cancelSellerTerms(); }
    @FXML private void onViewMerchandiseTermsUpgrade() { sellerProductsPresenter.viewMerchandiseTerms(); }
    @FXML private void onViewContentTermsUpgrade()     { sellerProductsPresenter.viewContentTerms(); }
    @FXML private void onViewPrivacyTermsUpgrade()     { sellerProductsPresenter.viewPrivacyTerms(); }

    // Profile / Deposit
    @FXML private void handleOpenDeposit()          { profilePresenter.openDeposit(); }
    @FXML private void handleCloseDeposit()         { profilePresenter.closeDeposit(); }
    @FXML private void handleProfileBackdropClick() { profilePresenter.handleBackdropClick(); }
    @FXML private void handleCopyAccountNumber()    { profilePresenter.copyAccountNumber(); }
    @FXML private void handleConfirmDeposit()       { profilePresenter.confirmDeposit(); }

    @FXML private void handleQuickDeposit(javafx.event.ActionEvent event) {
        if (!(event.getSource() instanceof Button btn)) return;
        String raw = btn.getUserData() != null ? btn.getUserData().toString() : "0";
        profilePresenter.quickDeposit(raw);
    }

    // Notification
    @FXML private void handleNotifications()      { notificationPresenter.openPanel(); }
    @FXML private void handleCloseNotifications() { notificationPresenter.closePanel(); }
    @FXML private void handleMarkAllRead()        { notificationPresenter.markAllRead(); }

    @FXML private void handleNotifBackdropClick(javafx.scene.input.MouseEvent e) {
        notificationPresenter.handleBackdropClick(e);
    }

    // Admin
    @FXML private void handleAdminTabUsers()                               { adminPresenter.showUsersTab(); }
    @FXML private void handleAdminTabAuctions()                            { adminPresenter.showAuctionsTab(); }
    @FXML private void handleAdminLoadUsers()                              { adminPresenter.loadUsers(); }
    @FXML private void handleAdminLoadAuctions()                           { adminPresenter.loadAuctions(); }
    @FXML private void handleAdminUserSearch(javafx.event.Event event)     { adminPresenter.searchUsers(); }
    @FXML private void handleAdminAuctionSearch(javafx.event.Event event)  { adminPresenter.searchAuctions(); }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    public void refreshProfile() {
        Platform.runLater(() -> profilePresenter.refresh(true));
    }

    public void shutdown() {
        catalogPresenter.shutdown();
        notificationPresenter.shutdown();
    }

    private static void showInfoPlaceholder(String title) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(title);
        a.setTitle("UBid");
        a.setContentText("Dang duoc ghep vao cac man hinh chi tiet trong do an.");
        a.show();
    }
}
