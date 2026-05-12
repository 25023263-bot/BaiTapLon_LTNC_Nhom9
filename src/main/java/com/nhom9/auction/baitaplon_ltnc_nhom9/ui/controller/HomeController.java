package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.PhysicalItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.AuctionRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.BidRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.ServiceLocator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator.HomeLoginCoordinator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator.ItemDetailCoordinator;
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

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
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
 * HomeController — quản lý toàn bộ giao diện trang chủ.
 *
 * Các overlay trong mainStack:
 *   1. homeScrollPane        — trang chủ chính (mặc định)
 *   2. profileOverlay        — tab "Cá nhân"
 *   3. myProductsOverlay     — tab "Sản phẩm của tôi" (Seller xem/quản lý SP)
 *   4. sellerTermsOverlay    — điều khoản khi Buyer muốn nâng cấp thành Seller
 *   5. listProductOverlay    — form đăng bán sản phẩm mới
 */
public class HomeController implements Initializable {

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
    @FXML private ComboBox<String> filterDropdown;
    @FXML private Button chipAll;
    @FXML private Button btnLogInProminent;
    @FXML private Button btnUserAvatar;

    // Profile overlay
    @FXML private StackPane profileOverlay;
    @FXML private Label profileTitleLabel;
    @FXML private Label profileHintLabel;
    @FXML private Label profileAvatarGlyph;
    @FXML private Button profileTabLoginButton;
    @FXML private Button profileLogoutButton;

    // Bottom nav — "Danh mục" đã đổi thành "Sản phẩm của tôi"
    @FXML private ToggleButton bottomNavHome;
    @FXML private ToggleButton bottomNavMyProducts;   // Trước là bottomNavCategories
    @FXML private ToggleButton bottomNavProfile;

    // ── "Sản phẩm của tôi" overlay (Seller) ─────────────────────────────────

    @FXML private StackPane myProductsOverlay;
    @FXML private Label myProductsSubtitle;
    @FXML private Button btnListNewProduct;
    @FXML private VBox myProductsList;

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
    @FXML private StackPane imageUploadBox;
    @FXML private Label listProductTitleError;
    @FXML private Label listProductCategoryError;
    @FXML private Label listProductDescError;
    @FXML private Label listProductPriceError;
    @FXML private Label listProductDateError;
    @FXML private Label listProductImageError;
    @FXML private Label submitProductError;

    // ── Trạng thái nội bộ ────────────────────────────────────────────────────

    private final AuctionRepository auctionRepo =
            ServiceLocator.getInstance().getAuctionRepo();
    private final BidRepository bidRepo =
            ServiceLocator.getInstance().getBidRepo();

    private ScheduledExecutorService timerScheduler;
    private final Map<String, Label> timerLabels = new HashMap<>();
    private final List<AuctionItem> displayedItems = new ArrayList<>();
    private Button activeChipButton;

    private HomeLoginCoordinator loginCoordinator;
    private ItemDetailCoordinator itemDetailCoordinator;
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
            int bidCount,
            boolean isLive,
            LocalDateTime endTime,
            String imagePlaceholderEmoji,
            String imageUrl             // đường dẫn ảnh thật lưu trong DB
    ) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Khởi tạo
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilterDropdown();
        setupListProductForm();
        activeChipButton = chipAll;
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
                refreshLoginUiChrome();
            }
        });
        Platform.runLater(this::bootstrapCoordinatorIfPossible);
        buildAvatarMenu();
    }

    /**
     * Thiết lập ComboBox danh mục và DatePicker trong form đăng bán.
     *
     * Tại sao làm ở đây thay vì trong FXML?
     * → FXML chỉ định nghĩa cấu trúc layout; dữ liệu động (danh sách options)
     *   nên được điền từ controller để dễ bảo trì và sau này có thể load từ DB.
     */
    private void setupListProductForm() {
        if (listProductCategoryCombo != null) {
            listProductCategoryCombo.getItems().addAll(
                    "⌚ Đồng hồ",
                    "💎 Trang sức",
                    "🎨 Nghệ thuật",
                    "🏺 Đồ cổ",
                    "🚗 Xe hơi",
                    "🏡 Nội thất",
                    "🏆 Sưu tầm",
                    "🏢 Bất động sản",
                    "📱 Điện tử",
                    "👗 Thời trang",
                    "📦 Khác"
            );
        }
    }

    private void bootstrapCoordinatorIfPossible() {
        if ((loginCoordinator != null && itemDetailCoordinator != null)
                || rootPane.getScene() == null) return;
        var scene = rootPane.getScene();
        if (scene.getWindow() instanceof Stage stage) {
            loginCoordinator = new HomeLoginCoordinator(stage);
            loginCoordinator.setOnAuthStateChanged(this::refreshLoginUiChrome);
            itemDetailCoordinator = new ItemDetailCoordinator(stage);
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

        node.setVisible(true);
        node.setManaged(true);
    }

    private static void setVisible(javafx.scene.Node node, boolean v) {
        node.setVisible(v);
        node.setManaged(v);
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

        // ── Nâng cấp role (in-memory; sau này gọi DB) ────────────────────────
        User currentUser = UserSession.getInstance().getCurrentUser();
        currentUser.setRole(UserRole.SELLER);
        // TODO: userRepository.updateRole(currentUser.getId(), UserRole.SELLER);

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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("UBid — " + title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
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
        // Kết thúc vào 23:59:59 của ngày người bán chọn
        LocalDateTime endTime = listProductEndDate.getValue().atTime(23, 59, 59);
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

        // Ngày kết thúc — phải sau hôm nay
        if (listProductEndDate.getValue() == null) {
            showError(listProductDateError);
            valid = false;
        } else if (!listProductEndDate.getValue().isAfter(LocalDate.now())) {
            listProductDateError.setText("⚠  Ngày kết thúc phải sau hôm nay");
            showError(listProductDateError);
            valid = false;
        } else hideError(listProductDateError);

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

    private void setupFilterDropdown() {
        filterDropdown.getItems().addAll(
                "Sắp hết hạn",
                "Nhiều lượt đấu nhất",
                "Giá thấp nhất",
                "Giá cao nhất",
                "Mới nhất");
        filterDropdown.setValue("Sắp hết hạn");
        filterDropdown.setOnAction(e -> handleFilterChange());
    }

    private AuctionItem toUiItem(
            com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem dbItem,
            int bidCount) {
        String emoji = getCategoryEmoji(dbItem.getCategory());
        boolean isLive = dbItem.getStatus() == AuctionStatus.ACTIVE;
        // imageUrl lấy từ DB; nếu null hoặc rỗng thì để chuỗi rỗng
        String imageUrl = dbItem.getImageUrl() != null ? dbItem.getImageUrl() : "";
        return new AuctionItem(
                String.valueOf(dbItem.getId()),
                dbItem.getTitle(),
                dbItem.getCategory(),
                emoji,
                dbItem.getCurrentPrice().doubleValue(),
                bidCount,
                isLive,
                dbItem.getEndTime(),
                emoji,
                imageUrl
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
        displayedItems.clear();
        List<AuctionItem> items = loadFromDb(AuctionStatus.ACTIVE);
        items = items.stream()
                .sorted((a, b) -> Double.compare(b.currentBid(), a.currentBid()))
                .limit(3)
                .toList();
        displayedItems.addAll(items);
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
        for (AuctionItem item : items) {
            boolean alreadyCached = displayedItems.stream().anyMatch(c -> c.id().equals(item.id()));
            if (!alreadyCached) displayedItems.add(item);
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
        Button bidBtn = new Button("Đặt giá");
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
        Button bidBtn = new Button("Đặt giá");
        bidBtn.getStyleClass().add("btn-bid-sm");
        bidBtn.setMaxWidth(Double.MAX_VALUE);
        bidBtn.setOnAction(e -> handlePlaceBid(item.id()));
        footer.getChildren().add(bidBtn);

        card.getChildren().addAll(imgPane, body, footer);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Timer
    // ─────────────────────────────────────────────────────────────────────────

    private void startCountdownTimers() {
        timerScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ubid-timer");
            t.setDaemon(true);
            return t;
        });
        timerScheduler.scheduleAtFixedRate(() -> {
            List<AuctionItem> allItems = loadFromDb(AuctionStatus.ACTIVE);
            for (AuctionItem item : allItems) {
                if (!item.isLive() || item.endTime() == null) continue;
                long secondsLeft = java.time.Duration.between(
                        LocalDateTime.now(), item.endTime()).getSeconds();
                String display;
                if (secondsLeft <= 0) {
                    display = "Đã kết thúc";
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
            User u = UserSession.getInstance().getCurrentUser();
            profileTitleLabel.setText(u.getFullName() != null && !u.getFullName().isBlank()
                    ? u.getFullName() : u.getUsername());
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

    // ─────────────────────────────────────────────────────────────────────────
    // FXML Handlers — navbar và misc
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void handleNavHome()    { bottomNavHome.setSelected(true); }
    @FXML private void handleNavResults() { showInfoPlaceholder("Kết quả đấu giá"); }
    @FXML private void handleSearch()     { searchField.requestFocus(); }

    @FXML
    private void handleSearchQuery() {
        String q = searchField.getText().trim();
        if (!q.isEmpty()) System.out.println("[Search] " + q);
    }

    @FXML private void handleNotifications() { showInfoPlaceholder("Thông báo"); }

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
        loadAllAuctions();
    }

    @FXML private void handleCategoryFilter() { System.out.println("[Category] chip"); }
    private void handleFilterChange() { System.out.println("[Filter] " + filterDropdown.getValue()); }

    private void handlePlaceBid(String auctionId) {
        if (!UserSession.getInstance().isLoggedIn()) {
            showLoginRequiredDialog();
            return;
        }
        if (itemDetailCoordinator == null) bootstrapCoordinatorIfPossible();
        AuctionItem item = findAuctionById(auctionId);
        if (item == null) { showInfoPlaceholder("Đấu giá " + auctionId); return; }
        if (itemDetailCoordinator != null) itemDetailCoordinator.openForAuction(item);
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

    public void shutdown() {
        if (timerScheduler != null && !timerScheduler.isShutdown())
            timerScheduler.shutdownNow();
    }
}