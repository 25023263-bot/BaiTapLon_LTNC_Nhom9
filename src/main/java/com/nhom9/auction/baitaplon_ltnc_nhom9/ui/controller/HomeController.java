package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import java.util.logging.Logger;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Notification;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.PhysicalItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.AuctionRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.BidRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.UserRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.AuctionObserver;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.ServiceLocator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.notification.NotificationService;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator.HomeLoginCoordinator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator.ItemDetailCoordinator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator.SellerItemDetailCoordinator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private final AuctionRepository auctionRepo =
            ServiceLocator.getInstance().getAuctionRepo();
    private final BidRepository bidRepo =
            ServiceLocator.getInstance().getBidRepo();
    private final UserRepository userRepo = new UserRepository();

    /** NotificationService — lấy từ ServiceLocator (singleton) */
    private final NotificationService notifService =
            ServiceLocator.getInstance().getNotificationService();

    private ScheduledExecutorService timerScheduler;
    /** Scheduler riêng cho badge polling — tách khỏi timerScheduler của countdown */
    private ScheduledExecutorService badgePoller;
    private final Map<String, Label> timerLabels = new HashMap<>();
    private final Set<String> expiredHandled = new HashSet<>();
    private final List<AuctionItem> displayedItems = new ArrayList<>();
    private Button activeChipButton;
    /** Danh mục đang lọc; null = tất cả */
    private String activeCategory = null;

    private HomeLoginCoordinator loginCoordinator;
    private ItemDetailCoordinator itemDetailCoordinator;
    private SellerItemDetailCoordinator sellerItemDetailCoordinator;
    private ContextMenu avatarMenu;

    /** Đường dẫn ảnh người bán đã chọn */
    private File selectedProductImage = null;

    // ─────────────────────────────────────────────────────────────────────────
    // Domain record — ánh xạ giữa DB model và UI
    // ─────────────────────────────────────────────────────────────────────────

    public record AuctionItem(
            String id,
            String title,
            String category,
            String categoryEmoji,
            double currentBid,
            double startingPrice,
            String description,
            int bidCount,
            boolean isLive,
            LocalDateTime endTime,
            String imagePlaceholderEmoji,
            String imageUrl,            // đường dẫn ảnh thật lưu trong DB
            int sellerId                // ID người bán — dùng để ẩn nút "Đặt giá" với chủ SP
    ) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Khởi tạo
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupListProductForm();
        activeChipButton = chipAll;
        // Lưu ý: KHÔNG gọi auctionRepo.closeExpiredAuctions() trực tiếp ở đây.
        // Lý do: auctionRepo.closeExpiredAuctions() chỉ UPDATE database, bỏ qua
        // toàn bộ AuctionHouse — không trigger Observer, không gửi notification.
        // AuctionScheduler (daemon thread) sẽ tự gọi auctionHouse.closeExpiredAuctions()
        // sau vài giây khởi động, đảm bảo đúng pipeline và gửi thông báo.
        loadHotAuctions();
        loadAllAuctions();
        startCountdownTimers();

        // Bottom nav toggle group
        ToggleGroup tabs = new ToggleGroup();
        bottomNavHome.setToggleGroup(tabs);
        bottomNavMyProducts.setToggleGroup(tabs);
        bottomNavProfile.setToggleGroup(tabs);

        tabs.selectedToggleProperty().addListener((obs, oldT, sel) -> {
            if (!(sel instanceof ToggleButton tb)) return;
            onBottomTabSwitch(tb);
        });
        bottomNavHome.setSelected(true);

        // Bootstrap coordinators sau khi Scene và Stage sẵn sàng
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

        // Đăng ký HomeController làm AuctionObserver để nhận sự kiện real-time
        // từ AuctionHouse (phiên kết thúc, phiên mở, huỷ...) và tự động refresh UI.
        ServiceLocator.getInstance().getAuctionHouse().addObserver(this);

        initNotifications();
    }

    /**
     * Thiết lập ComboBox danh mục, giờ, phút và DatePicker trong form đăng bán.
     *
     * Tại sao populate ComboBox giờ/phút ở đây thay vì trong FXML?
     * → Dữ liệu động (0–23 cho giờ, bước 5 phút cho phút) cần vòng lặp — không
     *   thể khai báo tĩnh trong FXML. Controller là nơi đúng để làm điều này.
     *
     * Tại sao bước phút là 5 phút thay vì từng phút (0–59)?
     * → 60 option trong dropdown rất khó dùng. Bước 5 phút cho 12 lựa chọn gọn,
     *   đủ độ chính xác cho một phiên đấu giá (không ai cần kết thúc đúng lúc 14:37).
     */
    private void setupListProductForm() {
        // ── Danh mục sản phẩm ────────────────────────────────────────────────
        if (listProductCategoryCombo != null) {
            listProductCategoryCombo.getItems().addAll(
                    "⌚ Đồng hồ", "💎 Trang sức", "🎨 Nghệ thuật", "🏺 Đồ cổ",
                    "🚗 Xe hơi", "🏡 Nội thất", "🏆 Sưu tầm", "🏢 Bất động sản",
                    "📱 Điện tử", "👗 Thời trang", "📦 Khác"
            );
        }

        // ── Giờ: 00 → 23 ─────────────────────────────────────────────────────
        if (listProductEndHour != null) {
            for (int h = 0; h < 24; h++) {
                listProductEndHour.getItems().add(String.format("%02d", h));
            }
            // Mặc định: giờ hiện tại + 1, capped ở 23
            int defaultHour = Math.min(java.time.LocalTime.now().getHour() + 1, 23);
            listProductEndHour.setValue(String.format("%02d", defaultHour));
        }

        // ── Phút: 00, 05, 10, … 55 ───────────────────────────────────────────
        if (listProductEndMinute != null) {
            for (int m = 0; m < 60; m += 5) {
                listProductEndMinute.getItems().add(String.format("%02d", m));
            }
            listProductEndMinute.setValue("00");
        }

        // ── Preview thời gian kết thúc (cập nhật realtime) ───────────────────
        //
        // Mỗi khi user thay đổi ngày, giờ hoặc phút → preview cập nhật ngay,
        // giúp user xác nhận thời gian mà không cần submit rồi mới biết sai.
        // Đây là UX pattern "immediate feedback" — phổ biến trong form phức tạp.
        Runnable updatePreview = () -> {
            if (lblEndTimePreview == null) return;
            var date = listProductEndDate != null ? listProductEndDate.getValue() : null;
            var hour = listProductEndHour != null ? listProductEndHour.getValue() : null;
            var min  = listProductEndMinute != null ? listProductEndMinute.getValue() : null;
            if (date != null && hour != null && min != null) {
                String preview = String.format("→ Kết thúc: %02d/%02d lúc %s:%s",
                        date.getDayOfMonth(), date.getMonthValue(), hour, min);
                lblEndTimePreview.setText(preview);
                lblEndTimePreview.setVisible(true);
                lblEndTimePreview.setManaged(true);
            } else {
                lblEndTimePreview.setVisible(false);
                lblEndTimePreview.setManaged(false);
            }
        };

        if (listProductEndDate != null)
            listProductEndDate.valueProperty().addListener((obs, o, n) -> updatePreview.run());
        if (listProductEndHour != null)
            listProductEndHour.valueProperty().addListener((obs, o, n) -> updatePreview.run());
        if (listProductEndMinute != null)
            listProductEndMinute.valueProperty().addListener((obs, o, n) -> updatePreview.run());
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

    // ─────────────────────────────────────────────────────────────────────────
    // Overlay management helpers
    // Tất cả "màn hình" đều nằm trong mainStack; chỉ một overlay được visible
    // ─────────────────────────────────────────────────────────────────────────

    /** Ẩn tất cả overlay, hiện lại scroll chính */
    private void hideAllOverlays() {
        setVisible(homeScrollPane,     true);
        setVisible(profileOverlay,     false);
        setVisible(myProductsOverlay,  false);
        setVisible(sellerTermsOverlay, false);
        setVisible(listProductOverlay, false);
        setVisible(resultsOverlay,     false);
    }

    /** Ẩn tất cả rồi chỉ hiện overlay được chỉ định */
    private void showOnly(javafx.scene.Node node) {
        homeScrollPane.setVisible(false);
        homeScrollPane.setManaged(false);
        profileOverlay.setVisible(false);
        profileOverlay.setManaged(false);
        myProductsOverlay.setVisible(false);
        myProductsOverlay.setManaged(false);
        sellerTermsOverlay.setVisible(false);
        sellerTermsOverlay.setManaged(false);
        listProductOverlay.setVisible(false);
        listProductOverlay.setManaged(false);
        resultsOverlay.setVisible(false);
        resultsOverlay.setManaged(false);

        node.setVisible(true);
        node.setManaged(true);
    }

    private static void setVisible(javafx.scene.Node node, boolean v) {
        node.setVisible(v);
        node.setManaged(v);
    }

    /**
     * Hiện một overlay *chồng lên* nội dung hiện tại (không ẩn view khác).
     * Dùng cho notification panel — xuất hiện như dropdown, không thay view.
     * Khác với showOnly() vốn ẩn toàn bộ rồi chỉ hiện 1 layer.
     */
    private static void showOverlay(StackPane overlay) {
        overlay.setVisible(true);
        overlay.setManaged(true);
        overlay.toFront();
    }

    private static void hideOverlay(StackPane overlay) {
        overlay.setVisible(false);
        overlay.setManaged(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bottom nav switching
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Xử lý khi user chuyển tab bottom nav.
     *
     * Pattern: ẩn hết, rồi chỉ hiện overlay phù hợp.
     * Đây là "Single Responsibility" — mỗi tab có một handler riêng.
     */
    private void onBottomTabSwitch(ToggleButton sel) {
        if (sel == bottomNavProfile) {
            showOnly(profileOverlay);
            refreshProfileTabContent(UserSession.getInstance().isLoggedIn());

        } else if (sel == bottomNavMyProducts) {
            handleMyProductsTab();

        } else {
            // Tab Trang chủ — hiện lại content chính
            showOnly(homeScrollPane);
        }
    }

    /**
     * Logic khi user bấm "Sản phẩm của tôi":
     *   - Chưa đăng nhập  → yêu cầu đăng nhập
     *   - Đã đăng nhập là BUYER   → hiện điều khoản để nâng cấp thành Seller
     *   - Đã đăng nhập là SELLER  → hiện danh sách sản phẩm
     *   - ADMIN           → cũng có thể xem (tùy thiết kế)
     *
     * Tại sao tách logic này ra?
     * → Controller nên "thin" (gọn). Mỗi hành động = 1 method rõ nghĩa.
     */
    private void handleMyProductsTab() {
        UserSession session = UserSession.getInstance();

        if (!session.isLoggedIn()) {
            // Reset tab về Home vì chưa đăng nhập
            bottomNavHome.setSelected(true);
            showLoginRequiredDialog();
            return;
        }

        if (session.isSeller() || session.isAdmin()) {
            showMyProductsPanel();
        } else {
            // BUYER: phải đồng ý điều khoản mới được bán hàng
            showSellerTermsPanel();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // "Sản phẩm của tôi" — Panel dành cho Seller
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Hiện panel "Sản phẩm của tôi" và tải danh sách SP của Seller hiện tại.
     */
    private void showMyProductsPanel() {
        showOnly(myProductsOverlay);
        loadMyProducts();
    }

    /**
     * Load sản phẩm của Seller từ DB và hiện trong myProductsList.
     *
     * Nếu DB chưa sẵn sàng hoặc chưa có SP nào, hiện "empty state" thay vì crash.
     * Đây là defensive programming — bảo vệ UI khỏi lỗi tầng dưới.
     */
    private void loadMyProducts() {
        myProductsList.getChildren().clear();

        int sellerId = UserSession.getInstance().getCurrentUserId();
        List<com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem> items;

        try {
            items = auctionRepo.findBySellerId(sellerId);
        } catch (Exception e) {
            System.err.println("[MyProducts] Lỗi load từ DB: " + e.getMessage());
            items = List.of();
        }

        if (items.isEmpty()) {
            showMyProductsEmptyState();
        } else {
            for (var dbItem : items) {
                VBox card = buildMyProductCard(dbItem);
                myProductsList.getChildren().add(card);
            }
        }
    }

    /** Hiện trạng thái rỗng khi Seller chưa có sản phẩm nào */
    private void showMyProductsEmptyState() {
        VBox emptyState = new VBox(16);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setStyle("-fx-padding: 60 0 60 0;");

        Label icon = new Label("🛍");
        icon.setStyle("-fx-font-size: 48px;");

        Label title = new Label("Chưa có sản phẩm nào");
        title.setStyle("-fx-text-fill: #f4f4f4; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label hint = new Label("Bấm \"Đăng bán sản phẩm\" để tạo phiên đấu giá đầu tiên của bạn.");
        hint.setStyle("-fx-text-fill: #888888; -fx-font-size: 13px;");
        hint.setWrapText(true);
        hint.setAlignment(Pos.CENTER);

        emptyState.getChildren().addAll(icon, title, hint);
        myProductsList.getChildren().add(emptyState);
    }

    /**
     * Tạo card hiển thị một sản phẩm trong danh sách "Sản phẩm của tôi".
     *
     * Chú ý: card này khác với card ở trang chủ — nó hiện thêm trạng thái
     * và không có nút "Đặt giá" (người bán không tự đấu SP của mình).
     */
    private VBox buildMyProductCard(
            com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem item) {

        VBox card = new VBox(10);
        card.getStyleClass().add("my-product-card");

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        // Emoji placeholder thay cho ảnh thật (sau này sẽ load từ image_url)
        String emoji = getCategoryEmoji(item.getCategory());
        // Ưu tiên ảnh thật từ imageUrl, fallback emoji nếu không có
        javafx.scene.Node imgLabel = buildImageNode(
                item.getImageUrl(), emoji, 60, 60);
        if (imgLabel instanceof Label lbl) lbl.getStyleClass().add("my-product-card-img");
        else {
            // ImageView — set kích thước
            ((ImageView) imgLabel).setFitWidth(60);
            ((ImageView) imgLabel).setFitHeight(60);
        }

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label titleLabel = new Label(item.getTitle());
        titleLabel.getStyleClass().add("my-product-card-title");
        titleLabel.setWrapText(true);

        Label categoryLabel = new Label(emoji + "  " + item.getCategory());
        categoryLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

        Label priceLabel = new Label("Giá khởi: " + formatPrice(item.getStartingPrice().doubleValue()));
        priceLabel.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 13px; -fx-font-weight: bold;");

        info.getChildren().addAll(titleLabel, categoryLabel, priceLabel);

        // Badge trạng thái
        Label statusBadge = new Label(getStatusDisplay(item.getStatus()));
        statusBadge.getStyleClass().add(
                item.getStatus() == AuctionStatus.ACTIVE ? "my-product-badge-live"
                        : "my-product-badge-ended");

        row.getChildren().addAll(imgLabel, info, statusBadge);
        card.getChildren().add(row);

        // Card có thể click → mở màn hình chi tiết/quản lý sản phẩm cho Seller.
        //
        // Tại sao phải convert sang UI record ở đây?
        // → buildMyProductCard() nhận domain AuctionItem (có getter/setter, từ DB),
        //   nhưng coordinator và SellerItemDetailController dùng HomeController.AuctionItem
        //   (UI record, immutable). Phải convert rõ ràng để tránh lỗi kiểu dữ liệu.
        //   Dùng toUiItem() đã có sẵn — không cần viết lại logic.
        int bidCountForCard = 0;
        try { bidCountForCard = bidRepo.countByAuctionId(item.getId()); }
        catch (Exception ignored) {}
        final AuctionItem uiItem = toUiItem(item, bidCountForCard);

        card.setOnMouseClicked(e -> {
            if (sellerItemDetailCoordinator == null) bootstrapCoordinatorIfPossible();
            if (sellerItemDetailCoordinator != null) {
                sellerItemDetailCoordinator.open(uiItem, () -> {
                    // Callback sau khi Seller đóng màn quản lý (đã edit hoặc xóa):
                    // Reload cả Home lẫn "Sản phẩm của tôi" để đồng bộ dữ liệu mới nhất
                    loadHotAuctions();
                    loadAllAuctions();
                    loadMyProducts();
                });
            }
        });
        card.setStyle(card.getStyle() + "; -fx-cursor: hand;");

        return card;
    }

    private String getCategoryEmoji(String category) {
        if (category == null) return "📦";
        return switch (category) {
            case "Điện thoại", "Điện tử" -> "📱";
            case "Laptop"     -> "💻";
            case "Phần mềm"   -> "💿";
            case "Game"       -> "🎮";
            case "Phụ kiện"   -> "🎧";
            case "Đồng hồ"    -> "⌚";
            case "Trang sức"  -> "💎";
            case "Nghệ thuật" -> "🎨";
            case "Đồ cổ"      -> "🏺";
            case "Xe hơi"     -> "🚗";
            case "Nội thất"   -> "🏡";
            case "Sưu tầm"    -> "🏆";
            case "Bất động sản" -> "🏢";
            default           -> "📦";
        };
    }

    /**
     * Tạo node hiển thị ảnh sản phẩm.
     *
     * Ưu tiên:
     *   1. Load ảnh thật từ đường dẫn file (imageUrl)
     *   2. Nếu không có / lỗi → fallback hiện emoji
     *
     * Tại sao cần try-catch?
     * → new Image() với đường dẫn sai sẽ không throw Exception ngay,
     *   nhưng img.isError() = true. Vì vậy luôn kiểm tra isError() trước khi dùng.
     *
     * @param imageUrl   đường dẫn file ảnh (tuyệt đối hoặc rỗng)
     * @param emoji      emoji fallback nếu ảnh không load được
     * @param width      chiều rộng mong muốn (px)
     * @param height     chiều cao mong muốn (px)
     */
    private javafx.scene.Node buildImageNode(String imageUrl, String emoji,
                                             double width, double height) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                // Chuyển đường dẫn file hệ thống → URI mà JavaFX hiểu
                // Ví dụ: "C:\photos\watch.jpg" → "file:///C:/photos/watch.jpg"
                String uri = new File(imageUrl).toURI().toString();
                Image img = new Image(uri, width, height, true, true, false);

                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(width);
                    iv.setFitHeight(height);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    return iv;
                }
            } catch (Exception ignored) {
                // File không tồn tại hoặc không phải ảnh → fallback emoji
            }
        }
        // Fallback: emoji
        Label fallback = new Label(emoji);
        fallback.getStyleClass().add("card-image-icon");
        return fallback;
    }

    private String getStatusDisplay(AuctionStatus status) {
        if (status == null) return "Không rõ";
        return switch (status) {
            case ACTIVE    -> "● Đang đấu giá";
            case CLOSED    -> "✓ Đã kết thúc";
            case EXPIRED   -> "⏰ Hết hạn";
            case PENDING   -> "◷ Chờ duyệt";
            case CANCELLED -> "✕ Đã hủy";
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // "Điều khoản Người bán" — Buyer nâng cấp thành Seller
    // ─────────────────────────────────────────────────────────────────────────

    /** Hiện overlay điều khoản; reset lại 3 checkbox về unchecked */
    private void showSellerTermsPanel() {
        // Reset checkboxes về trạng thái ban đầu
        upgradeTermsMerchandise.setSelected(false);
        upgradeTermsContent.setSelected(false);
        upgradeTermsPrivacy.setSelected(false);
        hideError(upgradeTermsMerchandiseError);
        hideError(upgradeTermsContentError);
        hideError(upgradeTermsPrivacyError);

        showOnly(sellerTermsOverlay);
    }

    /**
     * Buyer bấm "Đồng ý & Trở thành Người bán".
     * Validate 3 checkbox, rồi nâng cấp role lên SELLER.
     *
     * TODO: Khi có DB, cần gọi UserRepository để update role trong DB.
     * Hiện tại chỉ cập nhật trong UserSession (in-memory).
     */
    @FXML
    private void handleAcceptSellerTerms() {
        boolean valid = true;

        if (!upgradeTermsMerchandise.isSelected()) {
            showError(upgradeTermsMerchandiseError);
            valid = false;
        } else hideError(upgradeTermsMerchandiseError);

        if (!upgradeTermsContent.isSelected()) {
            showError(upgradeTermsContentError);
            valid = false;
        } else hideError(upgradeTermsContentError);

        if (!upgradeTermsPrivacy.isSelected()) {
            showError(upgradeTermsPrivacyError);
            valid = false;
        } else hideError(upgradeTermsPrivacyError);

        if (!valid) return;

        // ── Nâng cấp role: lưu vào DB trước, rồi cập nhật UserSession ────────
        //
        // Thứ tự quan trọng:
        //   1. Gọi DB trước — nếu DB lỗi thì UserSession không đổi → user biết có lỗi
        //   2. Sau khi DB thành công → mới cập nhật UserSession trong RAM
        //   3. Tạo đối tượng Seller mới thay thế Buyer cũ trong session
        //      vì domain model dùng inheritance (Seller extends User),
        //      không thể chỉ setRole() trên Buyer object
        User currentUser = UserSession.getInstance().getCurrentUser();
        try {
            userRepo.upgradeToSeller(currentUser.getId());
        } catch (Exception e) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("UBid");
            err.setHeaderText("Lỗi nâng cấp tài khoản");
            err.setContentText("Không thể lưu vào cơ sở dữ liệu: " + e.getMessage()
                    + "\nVui lòng thử lại.");
            err.showAndWait();
            return;
        }

        // DB đã cập nhật thành công → cập nhật object trong RAM
        // Tạo Seller mới kế thừa toàn bộ thông tin từ Buyer cũ
        com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller newSeller =
                new com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller();
        newSeller.setId(currentUser.getId());
        newSeller.setUsername(currentUser.getUsername());
        newSeller.setEmail(currentUser.getEmail());
        newSeller.setFullName(currentUser.getFullName());
        newSeller.setPhone(currentUser.getPhone());
        newSeller.setRole(UserRole.SELLER);
        newSeller.setActive(currentUser.isActive());
        UserSession.getInstance().login(newSeller);  // thay thế Buyer bằng Seller trong session

        // Thông báo thành công
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("UBid");
        alert.setHeaderText("🎉 Chào mừng bạn trở thành Người bán!");
        alert.setContentText("Tài khoản của bạn đã được nâng cấp thành Người bán. "
                + "Bạn có thể bắt đầu đăng bán sản phẩm ngay bây giờ.");
        alert.showAndWait();

        // Chuyển sang panel "Sản phẩm của tôi" sau khi upgrade thành công
        showMyProductsPanel();
    }

    @FXML
    private void handleCancelSellerTerms() {
        // Quay về trang chủ, bỏ chọn tab "Sản phẩm của tôi"
        bottomNavHome.setSelected(true);
        showOnly(homeScrollPane);
    }

    // Handlers cho nút "Xem" điều khoản trong overlay upgrade
    @FXML private void onViewMerchandiseTermsUpgrade() { showTermsDialog("Chính sách Hàng hóa",
            "Người bán chỉ được đăng bán những sản phẩm hợp pháp, không vi phạm pháp luật Việt Nam. "
                    + "Hàng giả, hàng nhái, hàng cấm, vũ khí, chất nổ, chất ma túy... đều bị nghiêm cấm. "
                    + "UBid có quyền gỡ bất kỳ sản phẩm nào vi phạm mà không cần báo trước."); }

    @FXML private void onViewContentTermsUpgrade() { showTermsDialog("Chính sách Nội dung",
            "Hình ảnh và mô tả sản phẩm phải trung thực, không gây hiểu nhầm. "
                    + "Không được sử dụng hình ảnh có bản quyền mà chưa được cấp phép. "
                    + "Nội dung không lành mạnh, kích động hoặc phân biệt đối xử sẽ bị xóa."); }

    @FXML private void onViewPrivacyTermsUpgrade() { showTermsDialog("Chính sách Bảo mật",
            "UBid thu thập thông tin cá nhân để vận hành dịch vụ đấu giá. "
                    + "Chúng tôi không bán thông tin của bạn cho bên thứ ba. "
                    + "Thông tin giao dịch được mã hóa và lưu trữ an toàn theo tiêu chuẩn quốc tế."); }

    private void showTermsDialog(String title, String content) {
        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.initOwner(rootPane.getScene().getWindow());
        dialog.setTitle("UBid \u2014 " + title);
        dialog.setResizable(false);

        Label lblIcon = new Label("\ud83d\udccb");
        lblIcon.setStyle("-fx-font-size: 28px;");
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f0e6c8;");
        lblTitle.setWrapText(true);
        VBox header = new VBox(10, lblIcon, lblTitle);
        header.setAlignment(javafx.geometry.Pos.CENTER);
        header.setStyle("-fx-padding: 28 32 20 32;");

        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
        sep.setStyle("-fx-background-color: #2a2a3e;");

        Label lblContent = new Label(content);
        lblContent.setWrapText(true);
        lblContent.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #a0a0c0; -fx-line-spacing: 4;");
        VBox contentBox = new VBox(lblContent);
        contentBox.setStyle("-fx-padding: 0 32 0 32;");
        ScrollPane scroll = new ScrollPane(contentBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPrefHeight(220);
        scroll.setStyle("-fx-background-color: #13131f; -fx-background: #13131f; -fx-border-color: transparent;");

        Button btnClose = new Button("  \u0110\u00e3 hi\u1ec3u");
        btnClose.setMaxWidth(Double.MAX_VALUE);
        btnClose.setStyle(
                "-fx-background-color: linear-gradient(to right,#c9a84c,#d9b65c);" +
                        "-fx-background-radius:10;-fx-text-fill:#0e0e18;" +
                        "-fx-font-size:14px;-fx-font-weight:bold;-fx-padding:13 0 13 0;-fx-cursor:hand;");
        btnClose.setOnAction(e -> dialog.close());
        VBox footer = new VBox(btnClose);
        footer.setStyle("-fx-padding: 20 32 28 32;");

        VBox root = new VBox(header, sep, scroll, footer);
        root.setStyle(
                "-fx-background-color:#0e0e18;" +
                        "-fx-background-radius:16;-fx-border-radius:16;" +
                        "-fx-border-color:#252538;-fx-border-width:1.5;");
        root.setPrefWidth(500);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // "Đăng bán sản phẩm" — Form tạo phiên đấu giá mới
    // ─────────────────────────────────────────────────────────────────────────

    /** Seller bấm "＋ Đăng bán sản phẩm" → mở form */
    @FXML
    private void handleListNewProduct() {
        resetListProductForm();
        showOnly(listProductOverlay);
    }

    /** Quay lại danh sách sản phẩm từ form đăng bán */
    @FXML
    private void handleBackToMyProducts() {
        showMyProductsPanel();
    }

    /**
     * User bấm vào vùng upload ảnh.
     * FileChooser mở cửa sổ chọn file; đường dẫn lưu vào selectedProductImage.
     *
     * Tại sao dùng FileChooser thay vì URL?
     * → App là desktop (JavaFX); FileChooser là cách chuẩn để chọn file local.
     *   Sau này khi có backend, sẽ upload file này lên server/cloud storage.
     */
    @FXML
    private void handleImageUpload() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn hình ảnh sản phẩm");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Hình ảnh (JPG, PNG, GIF)", "*.jpg", "*.jpeg", "*.png", "*.gif"));

        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedProductImage = file;
            hideError(listProductImageError);

            // ── Hiện preview ảnh thật trong ô upload ──────────────────────────
            // Xóa nội dung cũ (icon + label cũ) và thêm preview vào
            imageUploadBox.getChildren().clear();

            try {
                String uri = file.toURI().toString();
                Image previewImg = new Image(uri, 280, 140, true, true, false);

                if (!previewImg.isError()) {
                    // Có ảnh hợp lệ → hiện ImageView + tên file bên dưới
                    ImageView preview = new ImageView(previewImg);
                    preview.setFitWidth(280);
                    preview.setFitHeight(140);
                    preview.setPreserveRatio(true);
                    preview.setSmooth(true);

                    Label nameLabel = new Label("✓  " + file.getName());
                    nameLabel.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 11px;");

                    VBox previewBox = new VBox(8, preview, nameLabel);
                    previewBox.setAlignment(Pos.CENTER);
                    imageUploadBox.getChildren().add(previewBox);
                } else {
                    // File chọn nhưng không phải ảnh hợp lệ → hiện tên file
                    showUploadFallbackLabel(file.getName());
                }
            } catch (Exception e) {
                showUploadFallbackLabel(file.getName());
            }

            imageUploadBox.setStyle(
                    "-fx-border-color: #c9a84c; -fx-background-color: rgba(201,168,76,0.08);");
        }
    }

    /**
     * Validate form và tạo sản phẩm mới.
     *
     * Hiện tại: chỉ validate và thông báo thành công (chưa có DB).
     * TODO: Gọi AuctionRepository để insert vào DB.
     *
     * Tại sao validate ở controller chứ không ở service?
     * → Validation UI (field trống, định dạng) thuộc về Controller.
     *   Validation business (giá hợp lệ, ngày không quá khứ) thuộc về Service.
     *   Cách này tách biệt trách nhiệm rõ ràng.
     */
    @FXML
    private void handleSubmitProduct() {
        boolean valid = validateListProductForm();
        if (!valid) return;

        // ── Đọc dữ liệu từ form ────────────────────────────────────────────────
        String title      = listProductTitleField.getText().trim();
        String category   = listProductCategoryCombo.getValue();
        String desc       = listProductDescArea.getText().trim();
        BigDecimal price  = new BigDecimal(listProductPriceField.getText().trim());
        // Kết thúc vào đúng giờ:phút mà người bán chọn
        int endHour   = Integer.parseInt(listProductEndHour.getValue());
        int endMinute = Integer.parseInt(listProductEndMinute.getValue());
        LocalDateTime endTime = listProductEndDate.getValue().atTime(endHour, endMinute, 0);
        // Đường dẫn ảnh (null-safe: nếu chưa chọn thì lưu chuỗi rỗng)
        String imagePath = selectedProductImage != null
                ? selectedProductImage.getAbsolutePath() : "";

        // ── Tạo PhysicalItem với status ACTIVE ngay (Luồng A) ─────────────────
        //
        // Tại sao dùng PhysicalItem thay vì AuctionItem?
        // → AuctionItem là abstract — không thể new trực tiếp.
        //   PhysicalItem là loại hàng hoá thật, phù hợp với hầu hết sản phẩm
        //   mà người bán đăng lên. DigitalItem dùng cho phần mềm, key game...
        //
        // Các field "PHYSICAL" mình không hỏi trong form (condition, weight...)
        // được set giá trị mặc định an toàn. Sau này có thể thêm vào form.
        PhysicalItem newItem = new PhysicalItem();
        newItem.setSellerId(UserSession.getInstance().getCurrentUserId());
        newItem.setTitle(title);
        newItem.setCategory(category);
        newItem.setDescription(desc);
        newItem.setImageUrl(imagePath);
        newItem.setStartingPrice(price);
        newItem.setCurrentPrice(price);                          // ban đầu = giá khởi điểm
        newItem.setMinBidIncrement(new BigDecimal("1000"));      // bước giá tối thiểu 1.000đ
        newItem.setBuyNowPrice(null);                            // không có giá mua ngay
        newItem.setStartTime(LocalDateTime.now());
        newItem.setEndTime(endTime);
        newItem.setStatus(AuctionStatus.ACTIVE);                 // ← ACTIVE thẳng, không qua PENDING

        // Giá trị mặc định cho PhysicalItem
        newItem.setCondition("GOOD");                            // tình trạng mặc định: Tốt
        newItem.setWeightGrams(0);
        newItem.setDimensions("");
        newItem.setLocation("Việt Nam");
        newItem.setShippingCost(BigDecimal.ZERO);                // miễn phí ship (mặc định)
        newItem.setAllowPickup(false);

        // ── Lưu vào DB qua AuctionRepository ─────────────────────────────────
        try {
            auctionRepo.save(newItem);

            // Reload danh sách trang chủ để Buyer thấy SP mới ngay lập tức
            loadHotAuctions();
            loadAllAuctions();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("UBid");
            alert.setHeaderText("✓ Đăng bán thành công!");
            alert.setContentText("Sản phẩm \"" + title + "\" đã được đăng. "
                    + "Người mua có thể thấy và đặt giá ngay!");
            alert.showAndWait();

            handleBackToMyProducts();

        } catch (Exception e) {
            // Hiện lỗi trực tiếp trong form thay vì crash app
            submitProductError.setText("⚠  Lỗi khi lưu: " + e.getMessage());
            showError(submitProductError);
            System.err.println("[ListProduct] Lỗi save: " + e.getMessage());
        }
    }

    /**
     * Validate tất cả các trường trong form đăng bán.
     * Trả về true nếu hợp lệ, false nếu có lỗi.
     */
    private boolean validateListProductForm() {
        boolean valid = true;

        // Tên sản phẩm
        if (listProductTitleField.getText().isBlank()) {
            showError(listProductTitleError);
            valid = false;
        } else hideError(listProductTitleError);

        // Danh mục
        if (listProductCategoryCombo.getValue() == null) {
            showError(listProductCategoryError);
            valid = false;
        } else hideError(listProductCategoryError);

        // Mô tả (tối thiểu 20 ký tự — business rule đơn giản)
        if (listProductDescArea.getText().trim().length() < 20) {
            showError(listProductDescError);
            valid = false;
        } else hideError(listProductDescError);

        // Giá — phải là số dương
        try {
            double price = Double.parseDouble(listProductPriceField.getText().trim());
            if (price <= 0) throw new NumberFormatException();
            hideError(listProductPriceError);
        } catch (NumberFormatException e) {
            listProductPriceError.setText("⚠  Giá phải là số dương (ví dụ: 500000)");
            showError(listProductPriceError);
            valid = false;
        }

        // Ngày + Giờ kết thúc — phải sau thời điểm hiện tại
        if (listProductEndDate.getValue() == null) {
            listProductDateError.setText("⚠  Vui lòng chọn ngày kết thúc");
            showError(listProductDateError);
            valid = false;
        } else if (listProductEndHour.getValue() == null
                || listProductEndMinute.getValue() == null) {
            listProductDateError.setText("⚠  Vui lòng chọn giờ kết thúc");
            showError(listProductDateError);
            valid = false;
        } else {
            // Kết hợp ngày + giờ + phút → so sánh với thời điểm hiện tại
            // Tại sao phải kiểm tra LocalDateTime thay vì chỉ LocalDate?
            // → Người bán có thể chọn HÔM NAY nhưng giờ đã qua (ví dụ: 10h sáng
            //   nhưng bây giờ đã 14h) → phải kiểm tra đủ ngày + giờ + phút.
            int h = Integer.parseInt(listProductEndHour.getValue());
            int m = Integer.parseInt(listProductEndMinute.getValue());
            LocalDateTime chosenEnd = listProductEndDate.getValue().atTime(h, m, 0);
            // Yêu cầu tối thiểu: còn ít nhất 5 phút nữa mới kết thúc
            if (!chosenEnd.isAfter(LocalDateTime.now().plusMinutes(5))) {
                listProductDateError.setText("⚠  Thời gian kết thúc phải sau thời điểm hiện tại ít nhất 5 phút");
                showError(listProductDateError);
                valid = false;
            } else {
                hideError(listProductDateError);
            }
        }

        // Hình ảnh (bắt buộc)
        if (selectedProductImage == null) {
            showError(listProductImageError);
            valid = false;
        } else hideError(listProductImageError);

        return valid;
    }

    /**
     * Hiện label tên file khi ảnh không preview được.
     * Tách ra method riêng để tránh lặp code.
     */
    private void showUploadFallbackLabel(String fileName) {
        imageUploadBox.getChildren().clear();
        VBox fallback = new VBox(8);
        fallback.setAlignment(Pos.CENTER);
        Label icon = new Label("🖼");
        icon.setStyle("-fx-font-size: 32px;");
        Label name = new Label("✓  " + fileName);
        name.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 12px;");
        fallback.getChildren().addAll(icon, name);
        imageUploadBox.getChildren().add(fallback);
    }

    /** Reset toàn bộ form về trạng thái trống */
    private void resetListProductForm() {
        listProductTitleField.clear();
        listProductCategoryCombo.setValue(null);
        listProductDescArea.clear();
        listProductPriceField.clear();
        listProductEndDate.setValue(null);
        // Reset giờ/phút về default (giờ hiện tại + 1, phút = 00)
        if (listProductEndHour != null) {
            int defaultHour = Math.min(java.time.LocalTime.now().getHour() + 1, 23);
            listProductEndHour.setValue(String.format("%02d", defaultHour));
        }
        if (listProductEndMinute != null) listProductEndMinute.setValue("00");
        if (lblEndTimePreview != null) {
            lblEndTimePreview.setVisible(false);
            lblEndTimePreview.setManaged(false);
        }
        selectedProductImage = null;
        // Khôi phục lại ô upload về trạng thái ban đầu (xóa preview nếu có)
        imageUploadBox.getChildren().clear();
        VBox defaultContent = new VBox(10);
        defaultContent.setAlignment(Pos.CENTER);
        Label defaultIcon = new Label("📷");
        defaultIcon.getStyleClass().add("image-upload-icon");
        Label defaultHint = new Label("Bấm để chọn hình ảnh");
        defaultHint.getStyleClass().add("image-upload-hint");
        Label defaultSub = new Label("JPG, PNG, GIF · Tối đa 10MB");
        defaultSub.getStyleClass().add("image-upload-sub");
        defaultContent.getChildren().addAll(defaultIcon, defaultHint, defaultSub);
        imageUploadBox.getChildren().add(defaultContent);
        imageUploadBox.setStyle("");

        // Ẩn tất cả lỗi
        hideError(listProductTitleError);
        hideError(listProductCategoryError);
        hideError(listProductDescError);
        hideError(listProductPriceError);
        hideError(listProductDateError);
        hideError(listProductImageError);
        hideError(submitProductError);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: show/hide error labels
    // Tách ra thành method để giảm lặp code
    // ─────────────────────────────────────────────────────────────────────────

    private static void showError(Label errorLabel) {
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private static void hideError(Label errorLabel) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Auction data loading (giữ nguyên từ version cũ)
    // ─────────────────────────────────────────────────────────────────────────


    private AuctionItem toUiItem(
            com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem dbItem,
            int bidCount) {
        String emoji = getCategoryEmoji(dbItem.getCategory());
        boolean isLive = dbItem.getStatus() == AuctionStatus.ACTIVE;
        // imageUrl lấy từ DB; nếu null hoặc rỗng thì để chuỗi rỗng
        String imageUrl = dbItem.getImageUrl() != null ? dbItem.getImageUrl() : "";
        String description = dbItem.getDescription() != null ? dbItem.getDescription() : "";
        double startingPrice = dbItem.getStartingPrice() != null ? dbItem.getStartingPrice().doubleValue() : 0;
        return new AuctionItem(
                String.valueOf(dbItem.getId()),
                dbItem.getTitle(),
                dbItem.getCategory(),
                emoji,
                dbItem.getCurrentPrice().doubleValue(),
                startingPrice,
                description,
                bidCount,
                isLive,
                dbItem.getEndTime(),
                emoji,
                imageUrl,
                dbItem.getSellerId()   // ← truyền sellerId để UI ẩn nút "Đặt giá" với chủ sản phẩm
        );
    }

    private List<AuctionItem> loadFromDb(AuctionStatus status) {
        try {
            return auctionRepo.findByStatus(status).stream()
                    .map(dbItem -> {
                        int bidCount = 0;
                        try { bidCount = bidRepo.countByAuctionId(dbItem.getId()); }
                        catch (Exception ignored) {}
                        return toUiItem(dbItem, bidCount);
                    })
                    .toList();
        } catch (Exception e) {
            System.err.println("Lỗi load auction từ DB: " + e.getMessage());
            return List.of();
        }
    }

    private void loadHotAuctions() {
        hotCardsContainer.getChildren().clear();
        synchronized (displayedItems) { displayedItems.clear(); }
        List<AuctionItem> items = loadFromDb(AuctionStatus.ACTIVE);
        items = items.stream()
                .sorted((a, b) -> Integer.compare(b.bidCount(), a.bidCount()))
                .limit(3)
                .toList();
        synchronized (displayedItems) { displayedItems.addAll(items); }
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
        List<AuctionItem> items = loadFromDb(AuctionStatus.ACTIVE);
        synchronized (displayedItems) {
            for (AuctionItem item : items) {
                boolean alreadyCached = displayedItems.stream().anyMatch(c -> c.id().equals(item.id()));
                if (!alreadyCached) displayedItems.add(item);
            }
        }
        for (int i = 0; i < items.size(); i++) {
            VBox card = buildSmallCard(items.get(i));
            allProductsGrid.add(card, i % columns, i / columns);
        }
        resultCountLabel.setText(items.size() + " kết quả");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Card builders (giữ nguyên từ version cũ)
    // ─────────────────────────────────────────────────────────────────────────

    private VBox buildHotCard(AuctionItem item) {
        VBox card = new VBox();
        card.getStyleClass().add("auction-card");
        card.setSpacing(0);

        StackPane imageStack = new StackPane();
        imageStack.getStyleClass().add("card-image-placeholder");
        // Thử load ảnh thật; nếu không có thì fallback emoji
        javafx.scene.Node imgNode = buildImageNode(item.imageUrl(), item.imagePlaceholderEmoji(), 300, 200);
        imageStack.getChildren().add(imgNode);

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
            timerLabel.setText("Đã kết thúc");
        }
        timerRow.getChildren().addAll(timerIcon, timerLabel);
        imageStack.getChildren().add(timerRow);

        VBox cardBody = new VBox();
        cardBody.getStyleClass().add("card-body");
        HBox priceRow = new HBox();
        priceRow.getStyleClass().add("card-price-row");
        VBox priceLeft = new VBox(2);
        Label bidLbl = new Label("Giá hiện tại");
        bidLbl.getStyleClass().add("price-label-small");
        Label priceValue = new Label(formatPrice(item.currentBid()));
        priceValue.getStyleClass().add("price-value");
        priceLeft.getChildren().addAll(bidLbl, priceValue);
        Region priceSpacer = new Region();
        HBox.setHgrow(priceSpacer, Priority.ALWAYS);
        VBox bidsRight = new VBox(2);
        bidsRight.setAlignment(Pos.TOP_RIGHT);
        Label bidsLbl = new Label("📈  Lượt đấu");
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

        // Người bán không được đặt giá cho sản phẩm của chính mình
        // → kiểm tra sellerId của sản phẩm với userId hiện tại
        boolean isOwner = UserSession.getInstance().isLoggedIn()
                && UserSession.getInstance().getCurrentUserId() == item.sellerId();

        if (isOwner) {
            Label ownerLabel = new Label("🏷  Sản phẩm của bạn");
            ownerLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px; " +
                    "-fx-padding: 10 0 10 0; -fx-alignment: center;");
            ownerLabel.setMaxWidth(Double.MAX_VALUE);
            ownerLabel.setAlignment(javafx.geometry.Pos.CENTER);
            cardFooter.getChildren().add(ownerLabel);
        } else {
            Button bidBtn = new Button("Đặt giá");
            bidBtn.getStyleClass().add("btn-bid");
            bidBtn.setMaxWidth(Double.MAX_VALUE);
            bidBtn.setOnAction(e -> handlePlaceBid(item.id()));
            cardFooter.getChildren().add(bidBtn);
        }

        card.getChildren().addAll(imageStack, cardBody, cardFooter);
        return card;
    }

    private VBox buildSmallCard(AuctionItem item) {
        VBox card = new VBox();
        card.getStyleClass().add("product-card-sm");
        StackPane imgPane = new StackPane();
        imgPane.getStyleClass().add("card-sm-image");
        javafx.scene.Node imgNode = buildImageNode(item.imageUrl(), item.imagePlaceholderEmoji(), 160, 120);
        imgPane.getChildren().add(imgNode);

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
            statusLabel.setText("Đã kết thúc");
            statusLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        }
        body.getChildren().addAll(categoryLabel, titleLabel, priceLabel, statusLabel);

        VBox footer = new VBox();
        footer.getStyleClass().add("card-sm-footer");

        boolean isOwner = UserSession.getInstance().isLoggedIn()
                && UserSession.getInstance().getCurrentUserId() == item.sellerId();

        if (isOwner) {
            Label ownerLabel = new Label("🏷  Của bạn");
            ownerLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11px; " +
                    "-fx-padding: 8 0 8 0; -fx-alignment: center;");
            ownerLabel.setMaxWidth(Double.MAX_VALUE);
            ownerLabel.setAlignment(javafx.geometry.Pos.CENTER);
            footer.getChildren().add(ownerLabel);
        } else {
            Button bidBtn = new Button("Đặt giá");
            bidBtn.getStyleClass().add("btn-bid-sm");
            bidBtn.setMaxWidth(Double.MAX_VALUE);
            bidBtn.setOnAction(e -> handlePlaceBid(item.id()));
            footer.getChildren().add(bidBtn);
        }

        card.getChildren().addAll(imgPane, body, footer);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Timer
    // ─────────────────────────────────────────────────────────────────────────

    // ── Ket qua dau gia ──────────────────────────────────────────────────

    /**
     * Load tat ca phien da ket thuc (CLOSED hoac EXPIRED) va hien thi
     * trong resultsOverlay.
     *
     * CLOSED  : co nguoi thang (highest bidder)
     * EXPIRED : het gio nhung khong ai bid
     */
    private void loadResultAuctions() {
        resultsList.getChildren().clear();

        List<AuctionItem> closed  = loadFromDb(AuctionStatus.CLOSED);
        List<AuctionItem> expired = loadFromDb(AuctionStatus.EXPIRED);
        List<AuctionItem> all = new ArrayList<>();
        all.addAll(closed);
        all.addAll(expired);

        // Sap xep: moi ket thuc nhat len tren
        all.sort((a, b) -> {
            if (a.endTime() == null) return 1;
            if (b.endTime() == null) return -1;
            return b.endTime().compareTo(a.endTime());
        });

        if (all.isEmpty()) {
            Label empty = new Label("Chưa có phiên đấu giá nào kết thúc.");
            empty.setStyle("-fx-text-fill: #666; -fx-font-size: 14px; -fx-padding: 32;");
            resultsList.getChildren().add(empty);
        } else {
            for (AuctionItem item : all) {
                String winnerName  = null;
                double finalAmount = item.currentBid();
                try {
                    var leadingBid = bidRepo.findLeadingBid(Integer.parseInt(item.id()));
                    if (leadingBid.isPresent()) {
                        winnerName  = leadingBid.get().getBuyerUsername();
                        finalAmount = leadingBid.get().getAmount().doubleValue();
                    }
                } catch (Exception ignored) {}

                HBox card = buildResultCard(item, winnerName, finalAmount);
                resultsList.getChildren().add(card);
            }
        }

        resultsSubtitle.setText(all.size() + " phiên đấu giá đã kết thúc");
    }

    /**
     * Xay dung mot card ket qua dang ngang: [anh | thong tin san pham + nguoi thang].
     *
     * @param item        AuctionItem da ket thuc
     * @param winner      username nguoi thang (null neu khong co bid)
     * @param finalAmount gia cuoi cung
     */
    private HBox buildResultCard(AuctionItem item, String winner, double finalAmount) {
        HBox card = new HBox();
        card.getStyleClass().add("result-card");

        // -- Anh ben trai --
        StackPane imagePane = new StackPane();
        imagePane.getStyleClass().add("result-card-image");
        javafx.scene.Node imgNode = buildImageNode(item.imageUrl(), item.imagePlaceholderEmoji(), 140, 120);
        imagePane.getChildren().add(imgNode);

        // -- Body ben phai --
        VBox body = new VBox(6);
        body.getStyleClass().add("result-card-body");
        HBox.setHgrow(body, Priority.ALWAYS);

        Label categoryLabel = new Label(item.categoryEmoji() + "  " + item.category());
        categoryLabel.getStyleClass().add("result-card-category");

        Label titleLabel = new Label(item.title());
        titleLabel.getStyleClass().add("result-card-title");
        titleLabel.setWrapText(true);

        VBox priceBox = new VBox(2);
        Label priceLbl = new Label("Giá cuối cùng");
        priceLbl.getStyleClass().add("result-final-price-label");
        Label priceVal = new Label(formatPrice(finalAmount));
        priceVal.getStyleClass().add("result-final-price-value");
        priceBox.getChildren().addAll(priceLbl, priceVal);

        Label winnerBadge;
        if (winner != null) {
            winnerBadge = new Label("&#55356;&#57286;  Người thắng: " + winner);
            winnerBadge.getStyleClass().add("result-winner-badge");
        } else {
            winnerBadge = new Label("⏰  Hết hạn – không có lượt đấu");
            winnerBadge.getStyleClass().add("result-expired-badge");
        }

        body.getChildren().addAll(categoryLabel, titleLabel, priceBox, winnerBadge);
        card.getChildren().addAll(imagePane, body);
        return card;
    }

    private void startCountdownTimers() {
        timerScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ubid-timer");
            t.setDaemon(true);
            return t;
        });

        // ─────────────────────────────────────────────────────────────────
        // THIẾT KẾ MỚI: tách "tick đồng hồ" khỏi "kiểm tra expire"
        //
        // Vấn đề của thiết kế cũ:
        //   - Mỗi giây gọi loadFromDb(ACTIVE) → tốn kết nối DB liên tục
        //   - Race condition: khi item expire, closeExpiredAuctions() chưa
        //     kịp commit xong thì loadFromDb() kế tiếp vẫn trả về item đó
        //     → expiredHandled đã có id đó → needsRefresh = false → không reload
        //
        // Giải pháp:
        //   1. Đếm ngược DỰA TRÊN endTime đã load vào displayedItems (trong RAM)
        //      → không cần gọi DB mỗi giây
        //   2. Khi secondsLeft <= 0: close DB TRƯỚC (synchronous trên timer thread)
        //      rồi reload UI sau trên FX thread — đảm bảo thứ tự đúng
        //   3. reload() được gọi MỘT LẦN duy nhất nhờ expiredHandled
        // ─────────────────────────────────────────────────────────────────
        timerScheduler.scheduleAtFixedRate(() -> {
            // Snapshot displayedItems để tránh ConcurrentModificationException
            // (displayedItems có thể bị FX thread modify khi loadHotAuctions chạy)
            List<AuctionItem> snapshot;
            synchronized (displayedItems) {
                snapshot = List.copyOf(displayedItems);
            }

            boolean needsRefresh = false;

            for (AuctionItem item : snapshot) {
                if (item.endTime() == null) continue;

                long secondsLeft = java.time.Duration.between(
                        LocalDateTime.now(), item.endTime()).getSeconds();

                if (secondsLeft <= 0) {
                    // ── Item vừa hết hạn ─────────────────────────────────────
                    // expiredHandled đảm bảo chỉ trigger 1 lần cho mỗi item
                    if (!expiredHandled.contains(item.id())) {
                        expiredHandled.add(item.id());
                        needsRefresh = true;
                    }
                    // Cập nhật label "Đã kết thúc" ngay lập tức
                    final String endedText = "Đã kết thúc";
                    Label hot = timerLabels.get(item.id());
                    if (hot != null) Platform.runLater(() -> hot.setText(endedText));
                    Label sm = timerLabels.get(item.id() + "_sm");
                    if (sm != null) Platform.runLater(() -> sm.setText("⏱  " + endedText));
                } else {
                    // ── Cập nhật đồng hồ đếm ngược bình thường ──────────────
                    long h = secondsLeft / 3600;
                    long m = (secondsLeft % 3600) / 60;
                    long s = secondsLeft % 60;
                    final String fd = String.format("%02d:%02d:%02d", h, m, s);
                    Label hot = timerLabels.get(item.id());
                    if (hot != null) Platform.runLater(() -> hot.setText(fd));
                    Label sm = timerLabels.get(item.id() + "_sm");
                    if (sm != null) Platform.runLater(() -> sm.setText("⏱  " + fd));
                }
            }

            if (needsRefresh) {
                // ── Thứ tự quan trọng: close DB TRƯỚC trên timer thread ──────
                // Phải gọi qua AuctionHouse để kích hoạt observer → gửi notification!
                // Gọi auctionRepo.closeExpiredAuctions() trực tiếp sẽ bypass observer.
                try {
                    ServiceLocator.getInstance().getAuctionHouse().closeExpiredAuctions();
                } catch (Exception e) {
                    LOG.warning("closeExpiredAuctions lỗi: " + e.getMessage());
                }

                Platform.runLater(() -> {
                    // clear timerLabels trước khi build lại card (tránh label cũ mồ côi)
                    timerLabels.clear();
                    loadHotAuctions();
                    loadAllAuctions();
                    loadResultAuctions();
                });
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Avatar menu & Auth chrome
    // ─────────────────────────────────────────────────────────────────────────

    private void buildAvatarMenu() {
        avatarMenu = new ContextMenu();
        avatarMenu.getStyleClass().add("dark-context-menu");

        MenuItem miProfile    = new MenuItem("Thông tin cá nhân");
        MenuItem miMyAuctions = new MenuItem("Đấu giá của tôi");
        MenuItem miSettings   = new MenuItem("Cài đặt");
        MenuItem miLogout     = new MenuItem("Đăng xuất");
        miLogout.getStyleClass().add("menu-danger");

        miProfile.setOnAction(e    -> showInfoPlaceholder("Thông tin cá nhân"));
        miMyAuctions.setOnAction(e -> showInfoPlaceholder("Đấu giá của tôi"));
        miSettings.setOnAction(e   -> showInfoPlaceholder("Cài đặt"));
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

        // Khi auth state thay đổi (đăng nhập / đăng xuất / đổi tài khoản),
        // PHẢI rebuild lại toàn bộ card vì mỗi card tính isOwner tại thời điểm build:
        //
        //   boolean isOwner = UserSession.getCurrentUserId() == item.sellerId()
        //
        // Nếu không rebuild → card từ phiên Yana vẫn hiện "Sản phẩm của bạn"
        // dù buyer mới đăng nhập có userId khác hoàn toàn.
        // loadHotAuctions() + loadAllAuctions() build lại card từ đầu → isOwner
        // được tính lại với userId của user hiện tại → đúng.
        timerLabels.clear();
        loadHotAuctions();
        loadAllAuctions();

        // Nếu đang ở tab "Sản phẩm của tôi" và auth state thay đổi → refresh
        if (bottomNavMyProducts.isSelected()) {
            Platform.runLater(this::handleMyProductsTab);
        }
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

    private void updateProfileHeroMonogram(String username, String fullName) {
        if (profileAvatarGlyph != null)
            profileAvatarGlyph.setText(
                    fullName != null && !fullName.isBlank()
                            ? firstLetter(fullName)
                            : firstLetter(username));
    }

    private void refreshProfileTabContent(boolean logged) {
        if (profileTitleLabel == null) return;
        if (logged) {
            setVisible(profileScrollPane, true);
            setVisible(guestProfilePane, false);
            User u = UserSession.getInstance().getCurrentUser();
            profileTitleLabel.setText(u.getFullName() != null && !u.getFullName().isBlank()
                    ? u.getFullName() : u.getUsername());
            profileHintLabel.setText("Quyền: " + u.getRole() + " · " + u.getEmail());
            profileTabLoginButton.setVisible(false);
            profileTabLoginButton.setManaged(false);
            profileLogoutButton.setVisible(true);
            profileLogoutButton.setManaged(true);
            updateProfileHeroMonogram(u.getUsername(), u.getFullName());
            refreshProfileInfo(u);
            refreshWalletSection(u);
        } else {
            setVisible(profileScrollPane, false);
            setVisible(guestProfilePane, true);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
// A. CẬP NHẬT THÔNG TIN CÁ NHÂN
// ─────────────────────────────────────────────────────────────────────────

    /**
     * Điền thông tin cá nhân của user vào các Label trong profile tab.
     *
     * Tại sao tách thành method riêng?
     * → refreshProfileTabContent() đã đủ dài. Mỗi "khối" UI (info, wallet)
     *   nên có 1 method riêng — dễ maintain và test từng phần.
     */
    private void refreshProfileInfo(User u) {
        boolean show = u != null;
        setVisible(profileInfoSection, show);
        if (!show) return;

        infoFullName.setText(
                u.getFullName() != null && !u.getFullName().isBlank()
                        ? u.getFullName() : "Chưa cập nhật");
        infoEmail.setText(
                u.getEmail() != null ? u.getEmail() : "—");
        infoPhone.setText(
                u.getPhone() != null && !u.getPhone().isBlank()
                        ? u.getPhone() : "Chưa cập nhật");
        infoRole.setText(
                u.getRole() != null ? formatRole(u.getRole().name()) : "—");
        infoCreatedAt.setText(
                u.getCreatedAt() != null
                        ? u.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : "—");
    }

    /** Chuyển tên role thành tiếng Việt thân thiện hơn. */
    private String formatRole(String roleName) {
        return switch (roleName.toUpperCase()) {
            case "BUYER"  -> "Người mua";
            case "SELLER" -> "Người bán";
            case "ADMIN"  -> "Quản trị viên";
            default       -> roleName;
        };
    }

// ─────────────────────────────────────────────────────────────────────────
// B. CẬP NHẬT SỐ DƯ VÍ
// ─────────────────────────────────────────────────────────────────────────

    /**
     * Hiển thị số dư tương ứng với role của user.
     *
     * - Buyer  → walletBalance
     * - Seller → earningsBalance
     *
     * Tại sao phân biệt?
     * → Buyer và Seller dùng 2 trường tiền khác nhau (thiết kế hiện tại).
     *   Hiển thị đúng trường giúp user không nhầm lẫn.
     */
    private void refreshWalletSection(User u) {
        boolean show = u != null;
        setVisible(walletDivider, show);
        setVisible(profileWalletSection, show);
        if (!show) return;

        if (u instanceof com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer buyer) {
            BigDecimal balance = buyer.getWalletBalance() != null
                    ? buyer.getWalletBalance() : BigDecimal.ZERO;
            walletBalanceLabel.setText(formatVnd(balance));
            walletTypeLabel.setText("Ví Người mua");

        } else if (u instanceof com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller seller) {
            BigDecimal balance = seller.getEarningsBalance() != null
                    ? seller.getEarningsBalance() : BigDecimal.ZERO;
            walletBalanceLabel.setText(formatVnd(balance));
            walletTypeLabel.setText("Thu nhập Người bán");

        } else {
            walletBalanceLabel.setText("—");
            walletTypeLabel.setText("");
        }
    }

    /** Format số tiền thành dạng "1.000.000 ₫" */
    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        // Dùng NumberFormat để có dấu chấm ngăn cách hàng nghìn
        java.text.NumberFormat fmt = java.text.NumberFormat.getIntegerInstance(
                new java.util.Locale("vi", "VN"));
        return fmt.format(amount.longValue()) + " ₫";
    }

// ─────────────────────────────────────────────────────────────────────────
// C. MỞ / ĐÓNG DEPOSIT DIALOG
// ─────────────────────────────────────────────────────────────────────────

    /** Bấm nút "Nạp tiền" → hiện dialog nạp tiền. */
    @FXML
    private void handleOpenDeposit() {
        // Reset trạng thái dialog mỗi lần mở
        depositAmountField.clear();
        depositAmountHint.setText("Tối thiểu: 10.000 ₫");
        depositAmountHint.setStyle("");
        setVisible(depositStatusBox, false);
        btnConfirmDeposit.setDisable(false);
        btnConfirmDeposit.setText("Xác nhận đã chuyển khoản");

        setVisible(depositOverlay, true);
    }

    /** Bấm nút "✕" → đóng dialog nạp tiền. */
    @FXML
    private void handleCloseDeposit() {
        setVisible(depositOverlay, false);
    }

    /** Đóng profile overlay khi bấm vào backdrop. */
    @FXML
    private void handleProfileBackdropClick() {
        // Đóng deposit dialog trước nếu đang mở
        if (depositOverlay.isVisible()) {
            setVisible(depositOverlay, false);
            return;
        }
        // Về tab Home nếu bấm ngoài card profile
        bottomNavHome.setSelected(true);
        showOnly(homeScrollPane);
    }

// ─────────────────────────────────────────────────────────────────────────
// D. NẠP TIỀN NHANH (Quick Amount)
// ─────────────────────────────────────────────────────────────────────────

    /**
     * Bấm nút gợi ý (50K / 100K / 500K / 1M) → tự điền số tiền và mở dialog.
     *
     * userData của Button lưu số tiền (đặt trong FXML: userData="50000").
     */
    @FXML
    private void handleQuickDeposit(javafx.event.ActionEvent event) {
        if (!(event.getSource() instanceof Button btn)) return;
        String raw = btn.getUserData() != null ? btn.getUserData().toString() : "0";
        depositAmountField.setText(raw);
        handleOpenDeposit();
    }

// ─────────────────────────────────────────────────────────────────────────
// E. SAO CHÉP SỐ TÀI KHOẢN
// ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleCopyAccountNumber() {
        ClipboardContent content = new ClipboardContent();
        content.putString("0366855207");
        Clipboard.getSystemClipboard().setContent(content);

        // Phản hồi ngắn — đổi text nút thành "✅ Đã sao chép"
        // Tìm nút copy qua scene (đơn giản hơn inject @FXML thêm)
        // Trong thực tế bạn có thể thêm @FXML private Button btnCopyAccount;
        // Ở đây hiện toast hoặc alert nhẹ
        showToast("Đã sao chép số tài khoản!");
    }

// ─────────────────────────────────────────────────────────────────────────
// F. XÁC NHẬN NẠP TIỀN — Logic chính
// ─────────────────────────────────────────────────────────────────────────

    /**
     * Bấm "Xác nhận đã chuyển khoản":
     *   1. Validate số tiền nhập
     *   2. Hiện trạng thái "Đang xác nhận..."
     *   3. Giả lập 2 giây xử lý (PauseTransition)
     *   4. Cộng tiền vào session + cập nhật UI
     *
     * ⚠️  LƯU Ý CHO HỌC VIÊN:
     * Hiện tại hệ thống chưa có backend thật, nên chúng ta giả lập 2 giây
     * rồi tự động cộng tiền. Khi có backend thật, bước 3 sẽ là:
     *   - Gọi API/DB để lưu giao dịch nạp tiền
     *   - Server xác nhận → trả về số dư mới
     *   - Update UI từ response
     * Pattern PauseTransition → Platform.runLater() vẫn giữ nguyên,
     * chỉ thay nội dung bên trong.
     */
    @FXML
    private void handleConfirmDeposit() {
        // ── Validate ─────────────────────────────────────────────────
        String raw = depositAmountField.getText().trim().replaceAll("[^0-9]", "");
        if (raw.isEmpty()) {
            depositAmountHint.setText("⚠  Vui lòng nhập số tiền.");
            depositAmountHint.setStyle("-fx-text-fill: #e05555;");
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(raw);
        } catch (NumberFormatException e) {
            depositAmountHint.setText("⚠  Số tiền không hợp lệ.");
            depositAmountHint.setStyle("-fx-text-fill: #e05555;");
            return;
        }

        BigDecimal minimum = new BigDecimal("10000");
        if (amount.compareTo(minimum) < 0) {
            depositAmountHint.setText("⚠  Số tiền tối thiểu là 10.000 ₫.");
            depositAmountHint.setStyle("-fx-text-fill: #e05555;");
            return;
        }

        // ── Hiện trạng thái "đang xử lý" ────────────────────────────
        btnConfirmDeposit.setDisable(true);
        depositAmountField.setDisable(true);
        depositStatusIcon.setText("⏳");
        depositStatusText.setText("Đang xác nhận giao dịch...\nVui lòng chờ trong giây lát.");
        setVisible(depositStatusBox, true);

        // ── Giả lập 2 giây xử lý ────────────────────────────────────
        // PauseTransition là cách đúng để delay trong JavaFX mà không block UI thread.
        // KHÔNG dùng Thread.sleep() — nó sẽ đóng băng toàn bộ UI!
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(event -> Platform.runLater(() -> {
            applyDepositSuccess(amount);
        }));
        pause.play();
    }

    /**
     * Được gọi sau khi "xử lý" thành công.
     * Cộng tiền vào User trong session và refresh UI.
     */
    private void applyDepositSuccess(BigDecimal amount) {
        User u = UserSession.getInstance().getCurrentUser();
        if (u == null) return;

        // ── Bước 1: Cộng tiền vào object trong session (RAM) ──────────────
        if (u instanceof com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer buyer) {
            buyer.deposit(amount);

            // ── Bước 2: Lưu số dư mới xuống DB ──────────────────────────
            // Đây là bước quan trọng nhất! Nếu không gọi dòng này:
            //   - Số dư chỉ tồn tại trong RAM (session)
            //   - Khi restart app, DB vẫn = 0 → mất tiền
            //   - AuctionHouse.loadBuyer() đọc từ DB → luôn thấy 0 đ → từ chối đặt giá
            try {
                userRepo.updateWalletBalance(buyer.getId(), buyer.getWalletBalance());
            } catch (java.sql.SQLException e) {
                // Rollback RAM: trừ lại số tiền vừa cộng
                buyer.setWalletBalance(buyer.getWalletBalance().subtract(amount));
                depositStatusIcon.setText("❌");
                depositStatusText.setText("Lỗi lưu số dư. Vui lòng thử lại.\n" + e.getMessage());
                btnConfirmDeposit.setDisable(false);
                depositAmountField.setDisable(false);
                return;
            }

        } else if (u instanceof com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller seller) {
            BigDecimal newBalance = (seller.getEarningsBalance() != null
                    ? seller.getEarningsBalance() : BigDecimal.ZERO).add(amount);
            seller.setEarningsBalance(newBalance);

            // Lưu xuống DB cho Seller
            try {
                userRepo.updateEarningsBalance(seller.getId(), newBalance);
            } catch (java.sql.SQLException e) {
                seller.setEarningsBalance(newBalance.subtract(amount)); // rollback
                depositStatusIcon.setText("❌");
                depositStatusText.setText("Lỗi lưu số dư. Vui lòng thử lại.\n" + e.getMessage());
                btnConfirmDeposit.setDisable(false);
                depositAmountField.setDisable(false);
                return;
            }
        }

        // Hiện thông báo thành công
        depositStatusIcon.setText("✅");
        depositStatusText.setText("Nạp tiền thành công!\n+" + formatVnd(amount) + " đã được cộng vào tài khoản.");

        // Cho phép nhập lại / đóng
        depositAmountField.setDisable(false);
        btnConfirmDeposit.setText("Đóng");
        btnConfirmDeposit.setDisable(false);
        btnConfirmDeposit.setOnAction(e -> {
            handleCloseDeposit();
            // Refresh lại số dư hiển thị
            refreshWalletSection(UserSession.getInstance().getCurrentUser());
            // Reset nút về default
            btnConfirmDeposit.setOnAction(ev -> handleConfirmDeposit());
        });

        // Refresh số dư ngay lập tức
        refreshWalletSection(u);
    }

// ─────────────────────────────────────────────────────────────────────────
// G. TOAST NOTIFICATION nhẹ (helper)
// ─────────────────────────────────────────────────────────────────────────

    /**
     * Hiện một thông báo nhỏ tạm thời ở góc màn hình.
     *
     * Nếu project đã có AlertHelper.showToast() thì dùng cái đó thay thế.
     * Method này dùng Alert đơn giản để không phụ thuộc thêm.
     */
    private void showToast(String message) {
        // Cách đơn giản: dùng Alert rồi tự đóng sau 1.5s
        // Nếu muốn toast thật sự (không có nút OK), cần thêm Stage tuỳ chỉnh.
        javafx.scene.control.Alert toast = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        toast.setTitle("Thông báo");
        toast.setHeaderText(null);
        toast.setContentText(message);
        toast.show();

        PauseTransition close = new PauseTransition(Duration.millis(1500));
        close.setOnFinished(e -> toast.close());
        close.play();
    }

// ─────────────────────────────────────────────────────────────────────────
// H. HELPER setVisible (nếu chưa có trong HomeController)
// ─────────────────────────────────────────────────────────────────────────

// Kiểm tra HomeController đã có method này chưa.
// Nếu chưa, thêm vào:
//
// private void setVisible(Node node, boolean visible) {
//     if (node == null) return;
//     node.setVisible(visible);
//     node.setManaged(visible);
// }


    // ─────────────────────────────────────────────────────────────────────────
    // FXML Handlers — navbar và misc
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void handleNavHome()    { bottomNavHome.setSelected(true); }
    @FXML private void handleNavResults() {
        showOnly(resultsOverlay);
        bottomNavHome.setSelected(false);
        bottomNavMyProducts.setSelected(false);
        bottomNavProfile.setSelected(false);
        loadResultAuctions();
    }
    @FXML private void handleSearch()     { searchField.requestFocus(); }

    @FXML
    private void handleSearchQuery() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) { applyFilters(); return; }
        List<AuctionItem> base = (activeCategory == null || activeCategory.isEmpty())
                ? displayedItems
                : displayedItems.stream()
                .filter(item -> item.category() != null
                        && item.category().toLowerCase().contains(activeCategory.toLowerCase()))
                .toList();
        renderFilteredItems(
                base.stream()
                        .filter(item -> item.title().toLowerCase().contains(query)
                                || item.category().toLowerCase().contains(query))
                        .toList()
        );
    }

    @FXML private void handleNotifications() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null) {
            showInfoPlaceholder("Vui lòng đăng nhập để xem thông báo");
            return;
        }
        // Đánh dấu đã đọc ngay khi mở panel → badge về 0
        notifService.markAllRead(user.getId());
        refreshBadge(0);

        renderNotifications(user.getId());
        showOverlay(notifOverlay);
    }

    @FXML private void handleCloseNotifications() {
        hideOverlay(notifOverlay);
    }

    @FXML private void handleMarkAllRead() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user == null) return;
        // Xóa hẳn khỏi DB thay vì chỉ đánh dấu đã đọc — giữ DB gọn nhẹ
        notifService.clearAll(user.getId());
        refreshBadge(0);
        renderNotifications(user.getId()); // re-render → panel hiện trống
    }

    /**
     * Đóng panel khi user click vào backdrop (bên ngoài panel).
     * MouseEvent target là notifOverlay (backdrop) chứ không phải panel con.
     */
    @FXML private void handleNotifBackdropClick(javafx.scene.input.MouseEvent e) {
        if (e.getTarget() == notifOverlay) hideOverlay(notifOverlay);
    }

    // ── Notification core logic ───────────────────────────────────────────────

    /**
     * Khởi tạo hệ thống thông báo:
     *   1. Đăng ký uiListener → badge cập nhật ngay khi có bid mới (same JVM)
     *   2. Polling badge mỗi 30s → đảm bảo đồng bộ nếu có session khác ghi DB
     */
    private void initNotifications() {
        // 1. Real-time: nhận push từ AuctionHouse trong cùng JVM
        // Dùng unreadCountFresh() thay vì unreadCount() để tránh race condition:
        //   persist() xóa cache → broadcast() gọi listener → listener gọi unreadCount()
        //   Đôi khi computeIfAbsent() vẫn trả cache cũ do timing. Fresh() bỏ qua cache.
        notifService.addUiListener(event -> Platform.runLater(() -> {
            User user = UserSession.getInstance().getCurrentUser();
            if (user == null) return;
            // Chỉ cập nhật badge, không mở panel tự động (không muốn làm phiền)
            int count = notifService.unreadCountFresh(user.getId());
            refreshBadge(count);
        }));

        // 2. Polling mỗi 30s — đồng bộ từ DB phòng trường hợp nhiều máy
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
        }, 5, 30, TimeUnit.SECONDS); // delay 5s để app load xong
    }

    /**
     * Cập nhật badge số trên bell icon.
     * Badge ẩn khi count = 0, hiện và hiển thị số khi count > 0.
     * Hiển thị "99+" khi count > 99 để badge không bị tràn chữ.
     */
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

    /**
     * Render danh sách thông báo vào notifList VBox.
     * Mỗi item: [icon] [message + time] với style khác nhau cho read/unread.
     */
    private void renderNotifications(int userId) {
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
            HBox row = buildNotifRow(n, userId);
            notifList.getChildren().add(row);
        }
    }

    /**
     * Tạo 1 row thông báo.
     * Layout: [icon 32px] [VBox: message + time]
     * Click vào row → markRead + navigate đến phiên (nếu có auctionId)
     */
    private HBox buildNotifRow(Notification n, int userId) {
        // Icon loại thông báo
        Label icon = new Label(n.getIcon());
        icon.getStyleClass().add("notif-item-icon");

        // Message
        Label msg = new Label(n.getMessage());
        msg.getStyleClass().add("notif-item-message");
        msg.setWrapText(true);
        msg.setMaxWidth(270);

        // Time
        Label time = new Label(n.getFormattedTime());
        time.getStyleClass().add("notif-item-time");

        VBox content = new VBox(4, msg, time);
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox row = new HBox(12, icon, content);
        row.getStyleClass().add("notif-item");
        if (!n.isRead()) row.getStyleClass().add("notif-item-unread");

        // Click handler: đánh dấu đã đọc
        row.setOnMouseClicked(e -> {
            if (n.isRead()) return;
            notifService.markRead(n.getId(), userId);
            row.getStyleClass().remove("notif-item-unread");
            // Refresh badge (giảm 1, nhưng gọi lại service cho chính xác)
            int newCount = notifService.unreadCount(userId);
            refreshBadge(newCount);
        });

        return row;
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

    @FXML
    private void handleCategoryAll() {
        setActiveChip(chipAll);
        activeCategory = null;
        searchField.clear();
        applyFilters();
    }

    @FXML
    private void handleCategoryFilter(javafx.event.ActionEvent event) {
        if (!(event.getSource() instanceof Button clickedChip)) return;
        setActiveChip(clickedChip);
        String chipText = clickedChip.getText().trim();
        activeCategory = chipText.replaceAll("^\\S+\\s*", "").trim();
        searchField.clear();
        applyFilters();
    }

    private void applyFilters() {
        List<AuctionItem> filtered;
        if (activeCategory == null || activeCategory.isEmpty()) {
            filtered = displayedItems;
        } else {
            final String cat = activeCategory;
            filtered = displayedItems.stream()
                    .filter(item -> item.category() != null
                            && item.category().toLowerCase().contains(cat.toLowerCase()))
                    .toList();
        }
        renderFilteredItems(filtered);
    }

    private void renderFilteredItems(List<AuctionItem> items) {
        allProductsGrid.getChildren().clear();
        allProductsGrid.getColumnConstraints().clear();
        int columns = 3;
        for (int i = 0; i < columns; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columns);
            cc.setHgrow(Priority.ALWAYS);
            allProductsGrid.getColumnConstraints().add(cc);
        }
        for (int i = 0; i < items.size(); i++) {
            VBox card = buildSmallCard(items.get(i));
            allProductsGrid.add(card, i % columns, i / columns);
        }
        if (items.isEmpty()) {
            Label empty = new Label("Không tìm thấy kết quả nào 🔍");
            empty.setStyle("-fx-text-fill: #888; -fx-font-size: 14px; -fx-padding: 40 0;");
            allProductsGrid.add(empty, 0, 0);
            GridPane.setColumnSpan(empty, columns);
        }
        if (resultCountLabel != null)
            resultCountLabel.setText(items.size() + " kết quả");
    }

    private void handlePlaceBid(String auctionId) {
        if (!UserSession.getInstance().isLoggedIn()) {
            showLoginRequiredDialog();
            return;
        }
        if (itemDetailCoordinator == null) bootstrapCoordinatorIfPossible();
        AuctionItem item = findAuctionById(auctionId);
        if (item == null) { showInfoPlaceholder("Đấu giá " + auctionId); return; }

        // ── Chặn người bán tự đặt giá sản phẩm của mình ─────────────────────
        //
        // Tại sao kiểm tra lại ở đây, dù UI đã ẩn nút "Đặt giá"?
        // → Nguyên tắc "Defense in Depth" (bảo vệ nhiều lớp):
        //   UI chỉ là lớp hiển thị, có thể bị bypass (race condition, bug render...).
        //   Logic nghiệp vụ PHẢI được kiểm tra ở tầng controller/service,
        //   không được chỉ dựa vào việc ẩn/hiện nút trên UI.
        if (UserSession.getInstance().getCurrentUserId() == item.sellerId()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("UBid");
            alert.setHeaderText("Không thể đặt giá");
            alert.setContentText("Bạn không thể đặt giá cho sản phẩm của chính mình.");
            alert.showAndWait();
            return;
        }

        if (itemDetailCoordinator != null) {
            itemDetailCoordinator.openForAuction(item); // showAndWait — block đến khi đóng
            // Sau khi đóng màn chi tiết, reload Home để cập nhật giá + lượt bid
            loadHotAuctions();
            loadAllAuctions();
        }
    }

    private AuctionItem findAuctionById(String id) {
        return displayedItems.stream()
                .filter(it -> it.id().equals(id))
                .findFirst()
                .orElse(null);
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

    private void setActiveChip(Button newActive) {
        if (activeChipButton != null) {
            activeChipButton.getStyleClass().remove("chip-active");
            if (!activeChipButton.getStyleClass().contains("chip"))
                activeChipButton.getStyleClass().add("chip");
        }
        newActive.getStyleClass().remove("chip");
        if (!newActive.getStyleClass().contains("chip-active"))
            newActive.getStyleClass().add("chip-active");
        activeChipButton = newActive;
    }

    private String formatPrice(double price) {
        if (price >= 1_000_000)
            return String.format("đ%.2fM", price / 1_000_000);
        return String.format("đ%,.0f", price);
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
        // Phiên kết thúc → xóa khỏi trang chủ, cập nhật mục kết quả
        Platform.runLater(() -> {
            timerLabels.clear();
            loadHotAuctions();
            loadAllAuctions();

            // ── FIX: Sync số dư hiển thị ngay sau khi phiên kết thúc ──────────
            //
            // Vấn đề trước đây:
            //   processPayment() cập nhật DB đúng, nhưng UserSession đang giữ
            //   object currentUser trong RAM với số dư CŨ. refreshWalletSection()
            //   đọc từ UserSession nên luôn hiện số cũ cho đến khi đăng xuất.
            //
            // Giải pháp:
            //   Sau khi phiên đóng, nếu người dùng hiện tại là người thắng (buyer)
            //   hoặc người bán (seller), reload fresh data từ DB vào UserSession
            //   rồi refresh UI. Các người dùng không liên quan không bị ảnh hưởng.
            if (!UserSession.getInstance().isLoggedIn()) return;

            int currentUserId = UserSession.getInstance().getCurrentUserId();
            boolean isWinner = winnerId != null && currentUserId == winnerId;
            boolean isSeller = currentUserId == item.getSellerId();

            if (isWinner || isSeller) {
                try {
                    // Đọc lại từ DB để lấy số dư mới nhất sau khi thanh toán
                    userRepo.findById(currentUserId).ifPresent(freshUser -> {
                        UserSession.getInstance().login(freshUser); // cập nhật RAM
                        refreshWalletSection(freshUser);            // cập nhật UI
                    });
                } catch (Exception e) {
                    LOG.warning("Không thể refresh balance sau khi phiên đóng: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void onAuctionStarted(
            com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem item) {
        // Phiên mới chuyển PENDING → ACTIVE → hiện lên trang chủ
        Platform.runLater(() -> {
            loadHotAuctions();
            loadAllAuctions();
        });
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
        Platform.runLater(() -> {
            timerLabels.clear();
            loadHotAuctions();
            loadAllAuctions();
        });
    }

    public void shutdown() {
        if (timerScheduler != null && !timerScheduler.isShutdown())
            timerScheduler.shutdownNow();
        if (badgePoller != null && !badgePoller.isShutdown())
            badgePoller.shutdownNow();
        notifService.removeUiListener(null); // cleanup listener (no-op nếu null)
    }
}