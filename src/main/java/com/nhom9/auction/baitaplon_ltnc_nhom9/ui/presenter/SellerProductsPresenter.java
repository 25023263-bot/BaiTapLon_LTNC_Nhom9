package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.AuctionRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.BidRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.UserRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.listing.ListingRequest;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.listing.ListingService;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.CurrencyFormatHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.ProductImageHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.mapper.AuctionCardMapper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model.AuctionCardModel;

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
import java.time.LocalTime;
import java.util.List;

/**
 * Tab Sản phẩm của tôi: danh sách SP, nâng cấp Seller, form đăng bán.
 */
public final class SellerProductsPresenter {

    private final AuctionRepository auctionRepo;
    private final BidRepository bidRepo;
    private final AuctionCardMapper cardMapper;
    private final ListingService listingService;
    private final UserRepository userRepo;

    private SellerProductsView view;
    private SellerProductsHost host;
    private File selectedProductImage;

    public SellerProductsPresenter(
            AuctionRepository auctionRepo,
            BidRepository bidRepo,
            AuctionCardMapper cardMapper,
            ListingService listingService,
            UserRepository userRepo) {
        this.auctionRepo = auctionRepo;
        this.bidRepo = bidRepo;
        this.cardMapper = cardMapper;
        this.listingService = listingService;
        this.userRepo = userRepo;
    }

    public void bind(SellerProductsView view, SellerProductsHost host) {
        this.view = view;
        this.host = host;
    }

    public void initForm() {
        if (view.listProductCategoryCombo() != null) {
            view.listProductCategoryCombo().getItems().addAll(
                    "⌚ Đồng hồ", "💎 Trang sức", "🎨 Nghệ thuật", "🏺 Đồ cổ",
                    "🚗 Xe hơi", "🏡 Nội thất", "🏆 Sưu tầm", "🏢 Bất động sản",
                    "📱 Điện tử", "👗 Thời trang", "📦 Khác"
            );
        }
        if (view.listProductEndHour() != null) {
            for (int h = 0; h < 24; h++) {
                view.listProductEndHour().getItems().add(String.format("%02d", h));
            }
            int defaultHour = Math.min(LocalTime.now().getHour() + 1, 23);
            view.listProductEndHour().setValue(String.format("%02d", defaultHour));
        }
        if (view.listProductEndMinute() != null) {
            for (int m = 0; m < 60; m += 5) {
                view.listProductEndMinute().getItems().add(String.format("%02d", m));
            }
            view.listProductEndMinute().setValue("00");
        }

        Runnable updatePreview = () -> {
            if (view.lblEndTimePreview() == null) return;
            var date = view.listProductEndDate() != null ? view.listProductEndDate().getValue() : null;
            var hour = view.listProductEndHour() != null ? view.listProductEndHour().getValue() : null;
            var min = view.listProductEndMinute() != null ? view.listProductEndMinute().getValue() : null;
            if (date != null && hour != null && min != null) {
                view.lblEndTimePreview().setText(String.format(
                        "→ Kết thúc: %02d/%02d lúc %s:%s",
                        date.getDayOfMonth(), date.getMonthValue(), hour, min));
                view.lblEndTimePreview().setVisible(true);
                view.lblEndTimePreview().setManaged(true);
            } else {
                view.lblEndTimePreview().setVisible(false);
                view.lblEndTimePreview().setManaged(false);
            }
        };
        if (view.listProductEndDate() != null) {
            view.listProductEndDate().valueProperty().addListener((obs, o, n) -> updatePreview.run());
        }
        if (view.listProductEndHour() != null) {
            view.listProductEndHour().valueProperty().addListener((obs, o, n) -> updatePreview.run());
        }
        if (view.listProductEndMinute() != null) {
            view.listProductEndMinute().valueProperty().addListener((obs, o, n) -> updatePreview.run());
        }
    }

    public void onMyProductsTabSelected() {
        UserSession session = UserSession.getInstance();

        // Bước 1: Luôn hiện overlay "Sản phẩm của tôi" trước
        host.showMyProductsOverlay().run();

        // Bước 2: Hiện nội dung phù hợp với trạng thái đăng nhập
        if (!session.isLoggedIn()) {
            showGuestState();   // Màn hình "Bạn cần đăng nhập"
            return;
        }

        if (session.isSeller() || session.isAdmin()) {
            loadMyProducts();   // Màn hình danh sách sản phẩm
        } else {
            showSellerTermsPanel(); // Màn hình điều khoản nâng cấp Seller
        }
    }

    /**
     * Màn hình guest: hiển thị trong overlay khi chưa đăng nhập.
     * Có nút "Đăng nhập" để mở cửa sổ login trực tiếp.
     */
    private void showGuestState() {
        view.myProductsList().getChildren().clear();

        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setStyle("-fx-padding: 80 40 60 40;");

        Label icon = new Label("🔒");
        icon.setStyle("-fx-font-size: 56px;");

        Label title = new Label("Bạn cần đăng nhập để tiếp tục");
        title.setStyle("-fx-text-fill: #f4f4f4; -fx-font-size: 20px; -fx-font-weight: bold;");
        title.setWrapText(true);
        title.setAlignment(Pos.CENTER);

        Label hint = new Label("Đăng nhập để xem và quản lý sản phẩm đấu giá của bạn.");
        hint.setStyle("-fx-text-fill: #888888; -fx-font-size: 13px;");
        hint.setWrapText(true);
        hint.setAlignment(Pos.CENTER);

        Button btnLogin = new Button("  Đăng nhập");
        btnLogin.setStyle(
                "-fx-background-color: #c9a84c;" +
                        "-fx-text-fill: #0a0a0a;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 12 32 12 32;" +
                        "-fx-cursor: hand;"
        );
        // Bấm nút → mở cửa sổ đăng nhập trực tiếp (không qua dialog xác nhận)
        btnLogin.setOnAction(e -> host.requireLogin().run());

        container.getChildren().addAll(icon, title, hint, btnLogin);
        view.myProductsList().getChildren().add(container);
    }

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

    public void acceptSellerTerms() {
        boolean valid = true;
        if (!view.upgradeTermsMerchandise().isSelected()) {
            showError(view.upgradeTermsMerchandiseError());
            valid = false;
        } else hideError(view.upgradeTermsMerchandiseError());

        if (!view.upgradeTermsContent().isSelected()) {
            showError(view.upgradeTermsContentError());
            valid = false;
        } else hideError(view.upgradeTermsContentError());

        if (!view.upgradeTermsPrivacy().isSelected()) {
            showError(view.upgradeTermsPrivacyError());
            valid = false;
        } else hideError(view.upgradeTermsPrivacyError());

        if (!valid) return;

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

        Seller newSeller = new Seller();
        newSeller.setId(currentUser.getId());
        newSeller.setUsername(currentUser.getUsername());
        newSeller.setEmail(currentUser.getEmail());
        newSeller.setFullName(currentUser.getFullName());
        newSeller.setPhone(currentUser.getPhone());
        newSeller.setRole(UserRole.SELLER);
        newSeller.setActive(currentUser.isActive());
        UserSession.getInstance().login(newSeller);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("UBid");
        alert.setHeaderText("🎉 Chào mừng bạn trở thành Người bán!");
        alert.setContentText("Tài khoản của bạn đã được nâng cấp thành Người bán. "
                + "Bạn có thể bắt đầu đăng bán sản phẩm ngay bây giờ.");
        alert.showAndWait();

        host.onSessionChanged().run();
        showMyProductsPanel();
    }

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

    public void submitProduct() {
        if (!validateListProductForm()) return;

        String title = view.listProductTitleField().getText().trim();
        int endHour = Integer.parseInt(view.listProductEndHour().getValue());
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

        try {
            listingService.createListing(request);
            host.refreshCatalog().run();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("UBid");
            alert.setHeaderText("✓ Đăng bán thành công!");
            alert.setContentText("Sản phẩm \"" + title + "\" đã được đăng. "
                    + "Người mua có thể thấy và đặt giá ngay!");
            alert.showAndWait();
            backToMyProducts();
        } catch (Exception e) {
            view.submitProductError().setText("⚠  Lỗi khi lưu: " + e.getMessage());
            showError(view.submitProductError());
            System.err.println("[ListProduct] Lỗi save: " + e.getMessage());
        }
    }

    private void loadMyProducts() {
        view.myProductsList().getChildren().clear();
        int sellerId = UserSession.getInstance().getCurrentUserId();
        List<AuctionItem> items;
        try {
            items = auctionRepo.findBySellerId(sellerId);
        } catch (Exception e) {
            System.err.println("[MyProducts] Lỗi load từ DB: " + e.getMessage());
            items = List.of();
        }
        if (items.isEmpty()) {
            showMyProductsEmptyState();
        } else {
            for (AuctionItem dbItem : items) {
                view.myProductsList().getChildren().add(buildMyProductCard(dbItem));
            }
        }
    }

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

        int bidCount = 0;
        try {
            bidCount = bidRepo.countByAuctionId(item.getId());
        } catch (Exception ignored) {
        }
        AuctionCardModel uiItem = cardMapper.toCard(item, bidCount);

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

    private void showSellerTermsPanel() {
        view.upgradeTermsMerchandise().setSelected(false);
        view.upgradeTermsContent().setSelected(false);
        view.upgradeTermsPrivacy().setSelected(false);
        hideError(view.upgradeTermsMerchandiseError());
        hideError(view.upgradeTermsContentError());
        hideError(view.upgradeTermsPrivacyError());
        host.showSellerTermsOverlay().run();
    }

    private void resetListProductForm() {
        view.listProductTitleField().clear();
        view.listProductCategoryCombo().setValue(null);
        view.listProductDescArea().clear();
        view.listProductPriceField().clear();
        view.listProductEndDate().setValue(null);
        if (view.listProductEndHour() != null) {
            int defaultHour = Math.min(LocalTime.now().getHour() + 1, 23);
            view.listProductEndHour().setValue(String.format("%02d", defaultHour));
        }
        if (view.listProductEndMinute() != null) {
            view.listProductEndMinute().setValue("00");
        }
        if (view.lblEndTimePreview() != null) {
            view.lblEndTimePreview().setVisible(false);
            view.lblEndTimePreview().setManaged(false);
        }
        selectedProductImage = null;
        view.imageUploadBox().getChildren().clear();
        VBox defaultContent = new VBox(10);
        defaultContent.setAlignment(Pos.CENTER);
        Label defaultIcon = new Label("📷");
        defaultIcon.getStyleClass().add("image-upload-icon");
        Label defaultHint = new Label("Bấm để chọn hình ảnh");
        defaultHint.getStyleClass().add("image-upload-hint");
        Label defaultSub = new Label("JPG, PNG, GIF · Tối đa 10MB");
        defaultSub.getStyleClass().add("image-upload-sub");
        defaultContent.getChildren().addAll(defaultIcon, defaultHint, defaultSub);
        view.imageUploadBox().getChildren().add(defaultContent);
        view.imageUploadBox().setStyle("");

        hideError(view.listProductTitleError());
        hideError(view.listProductCategoryError());
        hideError(view.listProductDescError());
        hideError(view.listProductPriceError());
        hideError(view.listProductDateError());
        hideError(view.listProductImageError());
        hideError(view.submitProductError());
    }

    private boolean validateListProductForm() {
        boolean valid = true;
        if (view.listProductTitleField().getText().isBlank()) {
            showError(view.listProductTitleError());
            valid = false;
        } else hideError(view.listProductTitleError());

        if (view.listProductCategoryCombo().getValue() == null) {
            showError(view.listProductCategoryError());
            valid = false;
        } else hideError(view.listProductCategoryError());

        if (view.listProductDescArea().getText().trim().length() < 20) {
            showError(view.listProductDescError());
            valid = false;
        } else hideError(view.listProductDescError());

        try {
            double price = Double.parseDouble(view.listProductPriceField().getText().trim());
            if (price <= 0) throw new NumberFormatException();
            hideError(view.listProductPriceError());
        } catch (NumberFormatException e) {
            view.listProductPriceError().setText("⚠  Giá phải là số dương (ví dụ: 500000)");
            showError(view.listProductPriceError());
            valid = false;
        }

        if (view.listProductEndDate().getValue() == null) {
            view.listProductDateError().setText("⚠  Vui lòng chọn ngày kết thúc");
            showError(view.listProductDateError());
            valid = false;
        } else if (view.listProductEndHour().getValue() == null
                || view.listProductEndMinute().getValue() == null) {
            view.listProductDateError().setText("⚠  Vui lòng chọn giờ kết thúc");
            showError(view.listProductDateError());
            valid = false;
        } else {
            int h = Integer.parseInt(view.listProductEndHour().getValue());
            int m = Integer.parseInt(view.listProductEndMinute().getValue());
            LocalDateTime chosenEnd = view.listProductEndDate().getValue().atTime(h, m, 0);
            if (!chosenEnd.isAfter(LocalDateTime.now().plusMinutes(5))) {
                view.listProductDateError().setText(
                        "⚠  Thời gian kết thúc phải sau thời điểm hiện tại ít nhất 5 phút");
                showError(view.listProductDateError());
                valid = false;
            } else {
                hideError(view.listProductDateError());
            }
        }

        if (selectedProductImage == null) {
            showError(view.listProductImageError());
            valid = false;
        } else hideError(view.listProductImageError());

        return valid;
    }

    private void showUploadFallbackLabel(String fileName) {
        view.imageUploadBox().getChildren().clear();
        VBox fallback = new VBox(8);
        fallback.setAlignment(Pos.CENTER);
        Label icon = new Label("🖼");
        icon.setStyle("-fx-font-size: 32px;");
        Label name = new Label("✓  " + fileName);
        name.setStyle("-fx-text-fill: #c9a84c; -fx-font-size: 12px;");
        fallback.getChildren().addAll(icon, name);
        view.imageUploadBox().getChildren().add(fallback);
    }

    private void showTermsDialog(String title, String content) {
        var owner = host.dialogOwner().get();
        if (owner == null) return;

        Stage dialog = new Stage();
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("UBid — " + title);
        dialog.setResizable(false);

        Label lblIcon = new Label("📋");
        lblIcon.setStyle("-fx-font-size: 28px;");
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f0e6c8;");
        lblTitle.setWrapText(true);
        VBox header = new VBox(10, lblIcon, lblTitle);
        header.setAlignment(Pos.CENTER);
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

        Button btnClose = new Button("  Đã hiểu");
        btnClose.setMaxWidth(Double.MAX_VALUE);
        btnClose.setStyle(
                "-fx-background-color: linear-gradient(to right,#c9a84c,#d9b65c);"
                        + "-fx-background-radius:10;-fx-text-fill:#0e0e18;"
                        + "-fx-font-size:14px;-fx-font-weight:bold;-fx-padding:13 0 13 0;-fx-cursor:hand;");
        btnClose.setOnAction(e -> dialog.close());
        VBox footer = new VBox(btnClose);
        footer.setStyle("-fx-padding: 20 32 28 32;");

        VBox root = new VBox(header, sep, scroll, footer);
        root.setStyle(
                "-fx-background-color:#0e0e18;"
                        + "-fx-background-radius:16;-fx-border-radius:16;"
                        + "-fx-border-color:#252538;-fx-border-width:1.5;");
        root.setPrefWidth(500);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static void showError(Label errorLabel) {
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private static void hideError(Label errorLabel) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}