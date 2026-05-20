package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.UserDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.ItemDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.listing.ListingRequest;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.CurrencyFormatHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.ProductImageHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.mapper.AuctionCardMapper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model.AuctionCardModel;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.network.ServerConnection;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tab "Sản phẩm của tôi": danh sách SP, nâng cấp Seller, form đăng bán.
 *
 * <h3>Thay đổi so với phiên bản cũ:</h3>
 * <ul>
 *   <li>Bỏ hoàn toàn constructor nhận {@code AuctionRepository, BidRepository,
 *       AuctionCardMapper, ListingService, UserRepository} — không còn inject DB repo.</li>
 *   <li>{@link #loadMyProducts()} lấy danh sách phiên qua
 *       {@link ServerConnection#getAuctions()} trên background thread, lọc theo sellerId.</li>
 *   <li>{@link #submitProduct()} gửi {@code CREATE_LISTING} lên server qua socket
 *       (background thread). Không còn dùng ServiceLocator.</li>
 *   <li>{@link #acceptSellerTerms()} gửi {@code UPGRADE_TO_SELLER} lên server qua socket.</li>
 *   <li>{@code UserSession} đọc bằng {@code getCurrentUserDTO()} (socket path) thay vì
 *       {@code getCurrentUser()} (local path).</li>
 * </ul>
 */
public final class SellerProductsPresenter {

    private static final Logger LOG = Logger.getLogger(SellerProductsPresenter.class.getName());

    private SellerProductsView view;
    private SellerProductsHost host;
    private File selectedProductImage;

    public SellerProductsPresenter() {}

    public void bind(SellerProductsView view, SellerProductsHost host) {
        this.view = view;
        this.host = host;
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    public void initForm() {
        if (view.listProductCategoryCombo() != null) {
            view.listProductCategoryCombo().getItems().addAll(
                    "⌚ Đồng hồ", "💎 Trang sức", "🎨 Nghệ thuật", "🏺 Đồ cổ",
                    "🚗 Xe hơi", "🏡 Nội thất", "🏆 Sưu tầm", "🏢 Bất động sản",
                    "📱 Điện tử", "👗 Thời trang", "📦 Khác"
            );
        }
        if (view.listProductEndHour() != null) {
            for (int h = 0; h < 24; h++)
                view.listProductEndHour().getItems().add(String.format("%02d", h));
            view.listProductEndHour().getSelectionModel().select("18");
        }
        if (view.listProductEndMinute() != null) {
            view.listProductEndMinute().getItems().addAll("00", "15", "30", "45");
            view.listProductEndMinute().getSelectionModel().select("00");
        }

        // Live preview ngày kết thúc
        Runnable updatePreview = () -> {
            if (view.listProductEndDate().getValue() == null
                    || view.listProductEndHour().getValue() == null
                    || view.listProductEndMinute().getValue() == null) return;
            try {
                int h = Integer.parseInt(view.listProductEndHour().getValue());
                int m = Integer.parseInt(view.listProductEndMinute().getValue());
                var date = view.listProductEndDate().getValue();
                view.lblEndTimePreview().setText(
                        String.format("Kết thúc: %02d/%02d/%04d %02d:%02d",
                                date.getDayOfMonth(), date.getMonthValue(), date.getYear(), h, m));
            } catch (Exception ignored) {}
        };
        if (view.listProductEndDate() != null)
            view.listProductEndDate().valueProperty().addListener((obs, o, n) -> updatePreview.run());
        if (view.listProductEndHour() != null)
            view.listProductEndHour().valueProperty().addListener((obs, o, n) -> updatePreview.run());
        if (view.listProductEndMinute() != null)
            view.listProductEndMinute().valueProperty().addListener((obs, o, n) -> updatePreview.run());
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    /** Alias giữ tương thích với HomeController. */
    public void onMyProductsTabSelected() { showMyProductsPanel(); }

    public void showMyProductsPanel() {
        host.showMyProductsOverlay().run();
        loadMyProducts();
    }

    public void openListProductForm() {
        resetListProductForm();
        host.showListProductOverlay().run();
    }

    public void backToMyProducts() {
        showMyProductsPanel();
    }

    public void cancelSellerTerms() {
        view.bottomNavHome().setSelected(true);
        host.showHomeOverlay().run();
    }

    // ── Load my products (qua socket) ─────────────────────────────────────────

    /**
     * Load sản phẩm của Seller hiện tại từ server qua socket (background thread).
     * Lọc theo sellerId vì server trả toàn bộ danh sách.
     *
     * TODO: Khi server hỗ trợ GET_MY_AUCTIONS (có sellerId filter) thì thay
     *       ServerConnection.getAuctions() bằng ServerConnection.getMyAuctions(sellerId).
     */
    private void loadMyProducts() {
        view.myProductsList().getChildren().clear();

        int sellerId = UserSession.getInstance().getCurrentUserId();

        // Hiện loading placeholder
        Label loading = new Label("Đang tải...");
        loading.setStyle("-fx-text-fill: #888; -fx-padding: 20;");
        view.myProductsList().getChildren().add(loading);

        Thread t = new Thread(() -> {
            List<ItemDTO> items;
            try {
                items = ServerConnection.getAuctions().stream()
                        .filter(i -> i.getSellerId() == sellerId)
                        .toList();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Lỗi load sản phẩm của seller #" + sellerId, e);
                items = List.of();
            }

            final List<ItemDTO> finalItems = items;
            Platform.runLater(() -> {
                view.myProductsList().getChildren().clear();
                if (finalItems.isEmpty()) {
                    showMyProductsEmptyState();
                } else {
                    for (ItemDTO dto : finalItems) {
                        view.myProductsList().getChildren().add(
                                buildMyProductCard(AuctionCardMapper.toCardFromDTO(dto)));
                    }
                }
            });
        }, "seller-load-products-thread");
        t.setDaemon(true);
        t.start();
    }

    // ── Submit product (tạm dùng ListingService) ──────────────────────────────

    /**
     * Đăng bán sản phẩm mới.
     * Gửi request CREATE_LISTING lên server qua socket (background thread).
     */
    public void submitProduct() {
        if (!validateListProductForm()) return;

        String title = view.listProductTitleField().getText().trim();
        int endHour   = Integer.parseInt(view.listProductEndHour().getValue());
        int endMinute = Integer.parseInt(view.listProductEndMinute().getValue());
        LocalDateTime endTime = view.listProductEndDate().getValue().atTime(endHour, endMinute, 0);
        String imagePath = selectedProductImage != null
                ? selectedProductImage.getAbsolutePath() : "";

        ListingRequest request = new ListingRequest(
                UserSession.getInstance().getCurrentUserId(),
                title,
                view.listProductCategoryCombo().getValue(),
                view.listProductDescArea().getText().trim(),
                new BigDecimal(view.listProductPriceField().getText().trim()),
                endTime,
                imagePath
        );

        // Gửi lên server trên background thread — không block JavaFX thread
        Thread t = new Thread(() -> {
            try {
                ServerConnection.createListing(request);
                Platform.runLater(() -> {
                    host.refreshCatalog().run();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("UBid");
                    alert.setHeaderText("✓ Đăng bán thành công!");
                    alert.setContentText("Sản phẩm \"" + title + "\" đã được đăng. "
                            + "Người mua có thể thấy và đặt giá ngay!");
                    alert.showAndWait();
                    backToMyProducts();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    view.submitProductError().setText("⚠  Lỗi khi lưu: " + e.getMessage());
                    showError(view.submitProductError());
                });
                LOG.warning("[ListProduct] Lỗi save: " + e.getMessage());
            }
        }, "create-listing-thread");
        t.setDaemon(true);
        t.start();
    }

    // ── Upgrade to Seller ──────────────────────────────────────────────────────

    /**
     * Nâng cấp Buyer lên Seller sau khi đồng ý điều khoản.
     * Gửi request UPGRADE_TO_SELLER lên server qua socket (background thread).
     */
    public void acceptSellerTerms() {
        boolean valid = true;
        if (!view.upgradeTermsMerchandise().isSelected()) {
            showError(view.upgradeTermsMerchandiseError()); valid = false;
        } else hideError(view.upgradeTermsMerchandiseError());

        if (!view.upgradeTermsContent().isSelected()) {
            showError(view.upgradeTermsContentError()); valid = false;
        } else hideError(view.upgradeTermsContentError());

        if (!view.upgradeTermsPrivacy().isSelected()) {
            showError(view.upgradeTermsPrivacyError()); valid = false;
        } else hideError(view.upgradeTermsPrivacyError());

        if (!valid) return;

        int userId = UserSession.getInstance().getCurrentUserId();

        Thread t = new Thread(() -> {
            try {
                ServerConnection.upgradeToSeller(userId);
                Platform.runLater(() -> {
                    // Cập nhật role trong UserSession (không cần logout-login lại)
                    UserDTO dto = UserSession.getInstance().getCurrentUserDTO();
                    if (dto != null) {
                        dto.setRole(UserRole.SELLER);
                    }

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("UBid");
                    alert.setHeaderText("🎉 Chào mừng bạn trở thành Người bán!");
                    alert.setContentText("Tài khoản của bạn đã được nâng cấp thành Người bán. "
                            + "Bạn có thể bắt đầu đăng bán sản phẩm ngay bây giờ.");
                    alert.showAndWait();

                    host.onSessionChanged().run();
                    showMyProductsPanel();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("UBid");
                    err.setHeaderText("Lỗi nâng cấp tài khoản");
                    err.setContentText("Không thể lưu vào cơ sở dữ liệu: " + e.getMessage()
                            + "\nVui lòng thử lại.");
                    err.showAndWait();
                });
                LOG.warning("[UpgradeSeller] Lỗi: " + e.getMessage());
            }
        }, "upgrade-seller-thread");
        t.setDaemon(true);
        t.start();
    }

    // ── Seller terms view ─────────────────────────────────────────────────────

    public void viewMerchandiseTerms() {
        showTermsDialog("Chính sách Hàng hóa",
                "Người bán chỉ được đăng bán những sản phẩm hợp pháp, không vi phạm pháp luật Việt Nam. "
                        + "Hàng giả, hàng nhái, hàng cấm, vũ khí, chất nổ, chất ma túy... đều bị nghiêm cấm. "
                        + "UBid có quyền gỡ bất kỳ sản phẩm nào vi phạm mà không cần báo trước.");
    }

    public void viewContentTerms() {
        showTermsDialog("Chính sách Nội dung",
                "Hình ảnh và mô tả sản phẩm phải trung thực, không gây hiểu nhầm. "
                        + "Không được sử dụng hình ảnh có bản quyền mà chưa được cấp phép. "
                        + "Nội dung không lành mạnh, kích động hoặc phân biệt đối xử sẽ bị xóa.");
    }

    public void viewPrivacyTerms() {
        showTermsDialog("Chính sách Bảo mật",
                "UBid thu thập thông tin cá nhân để vận hành dịch vụ đấu giá. "
                        + "Chúng tôi không bán thông tin của bạn cho bên thứ ba. "
                        + "Thông tin giao dịch được mã hóa và lưu trữ an toàn theo tiêu chuẩn quốc tế.");
    }

    // ── Image upload ──────────────────────────────────────────────────────────

    public void uploadImage() {
        Stage stage = host.ownerStage().get();
        if (stage == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn hình ảnh sản phẩm");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Hình ảnh (JPG, PNG, GIF)", "*.jpg", "*.jpeg", "*.png", "*.gif"));

        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        selectedProductImage = file;
        hideError(view.listProductImageError());
        view.imageUploadBox().getChildren().clear();

        try {
            String uri = file.toURI().toString();
            Image previewImg = new Image(uri, 280, 140, true, true, false);
            if (!previewImg.isError()) {
                ImageView preview = new ImageView(previewImg);
                preview.setFitWidth(280);
                preview.setFitHeight(140);
                preview.setPreserveRatio(true);
                preview.setSmooth(true);
                Label nameLabel = new Label("✓  " + file.getName());
                nameLabel.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 11px;");
                VBox previewBox = new VBox(8, preview, nameLabel);
                previewBox.setAlignment(Pos.CENTER);
                view.imageUploadBox().getChildren().add(previewBox);
            } else {
                showUploadFallbackLabel(file.getName());
            }
        } catch (Exception e) {
            showUploadFallbackLabel(file.getName());
        }
        view.imageUploadBox().setStyle(
                "-fx-border-color: #c9a84c; -fx-background-color: rgba(201,168,76,0.08);");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

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
        view.myProductsList().getChildren().add(emptyState);
    }

    /**
     * Overload nhận AuctionCardModel — dùng khi dữ liệu đến từ ItemDTO (socket).
     * Tái sử dụng buildMyProductCard(AuctionItem) không còn cần thiết.
     */
    private VBox buildMyProductCard(AuctionCardModel card) {
        VBox cardBox = new VBox(10);
        cardBox.getStyleClass().add("my-product-card");

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        String emoji = card.categoryEmoji();
        javafx.scene.Node imgNode = ProductImageHelper.buildNode(card.imageUrl(), emoji, 60, 60);
        if (imgNode instanceof Label lbl) {
            lbl.getStyleClass().add("my-product-card-img");
        } else if (imgNode instanceof ImageView iv) {
            iv.setFitWidth(60);
            iv.setFitHeight(60);
        }

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label titleLabel = new Label(card.title());
        titleLabel.getStyleClass().add("my-product-card-title");
        titleLabel.setWrapText(true);
        Label categoryLabel = new Label(emoji + "  " + card.category());
        categoryLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        Label priceLabel = new Label("Giá khởi: "
                + CurrencyFormatHelper.formatPrice(card.startingPrice()));
        priceLabel.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 13px; -fx-font-weight: bold;");
        info.getChildren().addAll(titleLabel, categoryLabel, priceLabel);

        AuctionStatus status = card.isLive() ? AuctionStatus.ACTIVE : AuctionStatus.CLOSED;
        Label statusBadge = new Label(AuctionCardMapper.statusDisplay(status));
        statusBadge.getStyleClass().add(
                card.isLive() ? "my-product-badge-live" : "my-product-badge-ended");

        row.getChildren().addAll(imgNode, info, statusBadge);
        cardBox.getChildren().add(row);

        cardBox.setOnMouseClicked(e -> {
            host.ensureCoordinators().run();
            host.openSellerItemDetail().accept(card, () -> {
                host.refreshCatalog().run();
                loadMyProducts();
            });
        });
        cardBox.setStyle(cardBox.getStyle() + "; -fx-cursor: hand;");
        return cardBox;
    }

    private VBox buildMyProductCard(AuctionItem item) {
        VBox card = new VBox(10);
        card.getStyleClass().add("my-product-card");

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        String emoji = AuctionCardMapper.categoryEmoji(item.getCategory());
        javafx.scene.Node imgNode = ProductImageHelper.buildNode(
                item.getImageUrl(), emoji, 60, 60);
        if (imgNode instanceof Label lbl) {
            lbl.getStyleClass().add("my-product-card-img");
        } else if (imgNode instanceof ImageView iv) {
            iv.setFitWidth(60);
            iv.setFitHeight(60);
        }

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label titleLabel = new Label(item.getTitle());
        titleLabel.getStyleClass().add("my-product-card-title");
        titleLabel.setWrapText(true);
        Label categoryLabel = new Label(emoji + "  " + item.getCategory());
        categoryLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
        Label priceLabel = new Label("Giá khởi: "
                + CurrencyFormatHelper.formatPrice(item.getStartingPrice().doubleValue()));
        priceLabel.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 13px; -fx-font-weight: bold;");
        info.getChildren().addAll(titleLabel, categoryLabel, priceLabel);

        Label statusBadge = new Label(AuctionCardMapper.statusDisplay(item.getStatus()));
        statusBadge.getStyleClass().add(
                item.getStatus() == AuctionStatus.ACTIVE
                        ? "my-product-badge-live" : "my-product-badge-ended");

        row.getChildren().addAll(imgNode, info, statusBadge);
        card.getChildren().add(row);

        // Map sang AuctionCardModel để truyền cho coordinator
        AuctionCardModel uiItem = AuctionCardMapper.toCardSimple(item);

        card.setOnMouseClicked(e -> {
            host.ensureCoordinators().run();
            host.openSellerItemDetail().accept(uiItem, () -> {
                host.refreshCatalog().run();
                loadMyProducts();
            });
        });
        card.setStyle(card.getStyle() + "; -fx-cursor: hand;");
        return card;
    }

    private boolean validateListProductForm() {
        boolean valid = true;

        String title = view.listProductTitleField().getText().trim();
        if (title.length() < 5) {
            view.listProductTitleError().setText("Tên phải có ít nhất 5 ký tự.");
            showError(view.listProductTitleError()); valid = false;
        } else hideError(view.listProductTitleError());

        if (view.listProductCategoryCombo().getValue() == null) {
            view.listProductCategoryError().setText("Vui lòng chọn danh mục.");
            showError(view.listProductCategoryError()); valid = false;
        } else hideError(view.listProductCategoryError());

        String desc = view.listProductDescArea().getText().trim();
        if (desc.length() < 10) {
            view.listProductDescError().setText("Mô tả cần ít nhất 10 ký tự.");
            showError(view.listProductDescError()); valid = false;
        } else hideError(view.listProductDescError());

        String priceText = view.listProductPriceField().getText().trim();
        try {
            BigDecimal price = new BigDecimal(priceText);
            if (price.compareTo(BigDecimal.ZERO) <= 0)
                throw new NumberFormatException("price ≤ 0");
            hideError(view.listProductPriceError());
        } catch (NumberFormatException e) {
            view.listProductPriceError().setText("Giá phải là số dương hợp lệ.");
            showError(view.listProductPriceError()); valid = false;
        }

        java.time.LocalDate endDate    = view.listProductEndDate().getValue();
        String endHourStr   = view.listProductEndHour().getValue();
        String endMinuteStr = view.listProductEndMinute().getValue();

        if (endDate == null || endHourStr == null || endMinuteStr == null) {
            view.listProductDateError().setText("Vui lòng chọn ngày và giờ kết thúc.");
            showError(view.listProductDateError());
            valid = false;
        } else {
            int h = Integer.parseInt(endHourStr);
            int m = Integer.parseInt(endMinuteStr);
            LocalDateTime endDateTime = endDate.atTime(h, m, 0);
            if (!endDateTime.isAfter(LocalDateTime.now())) {
                view.listProductDateError().setText("Thời gian kết thúc phải sau thời điểm hiện tại.");
                showError(view.listProductDateError());
                valid = false;
            } else {
                hideError(view.listProductDateError());
            }
        }

        return valid;
    }

    private void resetListProductForm() {
        view.listProductTitleField().clear();
        view.listProductCategoryCombo().getSelectionModel().clearSelection();
        view.listProductDescArea().clear();
        view.listProductPriceField().clear();
        view.listProductEndDate().setValue(null);
        view.lblEndTimePreview().setText("");
        view.imageUploadBox().getChildren().clear();
        selectedProductImage = null;

        hideError(view.listProductTitleError());
        hideError(view.listProductCategoryError());
        hideError(view.listProductDescError());
        hideError(view.listProductPriceError());
        hideError(view.listProductDateError());
        hideError(view.listProductImageError());
        hideError(view.submitProductError());
    }

    private void showTermsDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("UBid — " + title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showUploadFallbackLabel(String fileName) {
        Label fallback = new Label("✓  " + fileName);
        fallback.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 13px;");
        view.imageUploadBox().getChildren().add(fallback);
    }

    private static void showError(javafx.scene.Node node) {
        node.setVisible(true); node.setManaged(true);
    }

    private static void hideError(javafx.scene.Node node) {
        node.setVisible(false); node.setManaged(false);
    }
}
