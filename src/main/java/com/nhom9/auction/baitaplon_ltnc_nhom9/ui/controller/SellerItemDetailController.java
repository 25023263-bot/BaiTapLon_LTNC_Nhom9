package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.PhysicalItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.AuctionRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.BidRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.ServiceLocator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model.AuctionCardModel;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller cho màn hình chi tiết sản phẩm của NGƯỜI BÁN.
 *
 * Khác với ItemDetailController (dành cho Buyer, chỉ xem + đặt giá),
 * màn hình này cho phép Seller:
 *   1. Xem thông tin chi tiết phiên đấu giá của sản phẩm mình
 *   2. Chỉnh sửa tiêu đề, mô tả, ảnh (nếu chưa có bid nào)
 *   3. Xóa sản phẩm (nếu chưa có bid nào)
 *
 * Tại sao không cho sửa nếu đã có bid?
 * → Khi người mua đã đặt giá, họ kỳ vọng thông tin sản phẩm không đổi.
 *   Cho phép Seller sửa giá/thông tin lúc đó là không công bằng.
 *   Đây là một business rule quan trọng trong hệ thống đấu giá.
 */
public class SellerItemDetailController {

    // ── FXML fields ──────────────────────────────────────────────

    @FXML private BorderPane rootPane;

    // Header
    @FXML private Label lblTitle;
    @FXML private Label lblStatusBadge;

    // Image area
    @FXML private StackPane imageContainer;
    @FXML private Label lblImageGlyph;

    // Timer
    @FXML private Label lblDays;
    @FXML private Label lblHours;
    @FXML private Label lblMinutes;
    @FXML private Label lblSeconds;

    // Thống kê nhanh
    @FXML private Label lblBidCount;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblStartingPrice;

    // Lịch sử đấu giá
    @FXML private VBox vboxBidHistory;
    @FXML private Label lblBidCountSmall;

    // Edit form (hiện/ẩn theo trạng thái)
    @FXML private VBox editFormPane;
    @FXML private TextField editTitleField;
    @FXML private TextArea  editDescArea;
    @FXML private DatePicker editEndDatePicker;
    @FXML private StackPane  editImageBox;
    @FXML private Label      editImageHint;

    // Các nút hành động
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private Button btnSave;
    @FXML private Button btnCancelEdit;

    // Warning khi đã có bid
    @FXML private Label lblEditWarning;

    // ── State ────────────────────────────────────────────────────

    private Stage thisStage;
    private AuctionCardModel item;                   // dữ liệu UI hiện tại
    private Runnable onDataChanged;             // callback → HomeController reload
    private boolean isEditing = false;
    private File newImageFile = null;           // ảnh mới nếu Seller thay ảnh

    private final AuctionRepository auctionRepo =
            ServiceLocator.getInstance().getAuctionRepo();
    private final BidRepository bidRepo =
            ServiceLocator.getInstance().getBidRepo();

    private ScheduledExecutorService timerScheduler;

    // ── Public API ───────────────────────────────────────────────

    /**
     * Gọi bởi SellerItemDetailCoordinator ngay sau khi load FXML.
     *
     * @param stage         Stage của cửa sổ này (để handleBack() đóng đúng)
     * @param item          Dữ liệu sản phẩm từ HomeController
     * @param onDataChanged Callback khi Seller save/delete → Home reload data
     */
    public void configure(Stage stage, AuctionCardModel item, Runnable onDataChanged) {
        this.thisStage    = stage;
        this.item         = item;
        this.onDataChanged = onDataChanged;

        populateView(item);

        // Load lịch sử bid thật từ DB
        loadBidHistory();

        // Countdown timer
        if (item.isLive() && item.endTime() != null) {
            startCountdownTimer(item.endTime());
        } else {
            lblDays.setText("00"); lblHours.setText("00");
            lblMinutes.setText("00"); lblSeconds.setText("00");
        }

        // Hiển thị nút edit/delete nhưng vô hiệu hóa nếu đã có bid
        refreshActionButtons();
    }

    // ── Populate ─────────────────────────────────────────────────

    private void populateView(AuctionCardModel it) {
        lblTitle.setText(it.title());

        // Badge trạng thái
        String statusText = it.isLive() ? "● Đang đấu giá" : "✓ Đã kết thúc";
        lblStatusBadge.setText(statusText);
        lblStatusBadge.getStyleClass().removeAll("seller-badge-live", "seller-badge-ended");
        lblStatusBadge.getStyleClass().add(it.isLive() ? "seller-badge-live" : "seller-badge-ended");

        // Ảnh hoặc emoji
        refreshImageDisplay(it.imageUrl(), it.imagePlaceholderEmoji());

        // Giá
        lblStartingPrice.setText(formatPrice(it.startingPrice()));
        lblCurrentPrice.setText(formatPrice(it.currentBid()));

        // Số lượt bid (sẽ được cập nhật lại từ loadBidHistory)
        lblBidCount.setText(it.bidCount() + " lượt đấu giá");
    }

    /**
     * Hiển thị ảnh sản phẩm trong imageContainer.
     * Nếu không có ảnh hợp lệ → fallback sang emoji.
     */
    private void refreshImageDisplay(String imageUrl, String emoji) {
        imageContainer.getChildren().clear();
        boolean loaded = false;

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                String uri = new File(imageUrl).toURI().toString();
                Image img = new Image(uri, 320, 220, true, true, false);
                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(320);
                    iv.setFitHeight(220);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    imageContainer.getChildren().add(iv);
                    loaded = true;
                }
            } catch (Exception ignored) {}
        }

        if (!loaded) {
            lblImageGlyph.setText(emoji);
            imageContainer.getChildren().add(lblImageGlyph);
        }
    }

    // ── Bid history ──────────────────────────────────────────────

    private void loadBidHistory() {
        vboxBidHistory.getChildren().clear();
        try {
            int auctionId = Integer.parseInt(item.id());
            List<Bid> bids = bidRepo.findByAuctionId(auctionId);
            int count = bids.size();

            lblBidCount.setText(count + " lượt đấu giá");
            if (lblBidCountSmall != null)
                lblBidCountSmall.setText(count + " lượt");

            if (bids.isEmpty()) {
                Label empty = new Label("Chưa có lượt đấu giá nào.");
                empty.setStyle("-fx-text-fill: #888; -fx-padding: 12;");
                vboxBidHistory.getChildren().add(empty);
            } else {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm  dd/MM");
                for (int i = 0; i < bids.size(); i++) {
                    Bid b = bids.get(i);
                    String name    = b.getBuyerUsername() != null ? b.getBuyerUsername() : "Ẩn danh";
                    String timeStr = b.getBidTime() != null ? b.getBidTime().format(fmt) : "--";
                    vboxBidHistory.getChildren().add(
                            buildBidRow(name, b.getAmount().doubleValue(), timeStr, i == 0)
                    );
                }
            }
        } catch (Exception e) {
            Label err = new Label("Không thể tải lịch sử đấu giá.");
            err.setStyle("-fx-text-fill: #e74c3c; -fx-padding: 12;");
            vboxBidHistory.getChildren().add(err);
        }
    }

    /**
     * Tạo một row trong lịch sử đặt giá.
     * Cấu trúc giống ItemDetailController.buildBidRow() để giao diện đồng nhất.
     */
    private HBox buildBidRow(String name, double amount, String timeStr, boolean isLeading) {
        Label avatarLabel = new Label(String.valueOf(name.charAt(0)).toUpperCase());
        avatarLabel.getStyleClass().add(isLeading ? "history-avatar-lead" : "history-avatar");
        StackPane avatarWrap = new StackPane(avatarLabel);
        avatarWrap.getStyleClass().add("history-avatar-wrap");
        avatarWrap.setMinSize(40, 40);
        avatarWrap.setMaxSize(40, 40);

        Label lblName = new Label(name);
        lblName.getStyleClass().add("history-name");
        Label lblSub  = new Label(isLeading ? "👑  Đang dẫn đầu" : "Đã đặt giá");
        lblSub.getStyleClass().add(isLeading ? "history-sub-lead" : "history-sub");
        VBox nameBox = new VBox(2, lblName, lblSub);
        nameBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblAmount = new Label(formatPrice(amount));
        lblAmount.getStyleClass().add(isLeading ? "history-price-lead" : "history-price");
        Label lblTime = new Label(timeStr);
        lblTime.getStyleClass().add("history-time");
        VBox priceBox = new VBox(2, lblAmount, lblTime);
        priceBox.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(12, avatarWrap, nameBox, spacer, priceBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(isLeading ? "history-row-lead" : "history-row");
        row.setMinHeight(56);
        return row;
    }

    // ── Action buttons ───────────────────────────────────────────

    /**
     * Cập nhật trạng thái nút dựa theo:
     *   - Phiên đã kết thúc → không cho sửa/xóa
     *   - Đã có bid        → không cho sửa/xóa (để bảo vệ người mua)
     *   - Chưa có bid      → cho phép sửa và xóa
     *
     * Đây là business rule: "Seller không được thay đổi điều kiện đấu giá
     * khi đã có người tham gia."
     */
    private void refreshActionButtons() {
        boolean hasAnyBid = item.bidCount() > 0;
        boolean isEnded   = !item.isLive();

        if (isEnded) {
            // Phiên đã kết thúc → ẩn cả 2 nút
            setButtonsVisible(false, false);
            if (lblEditWarning != null) {
                lblEditWarning.setText("⏰  Phiên đấu giá đã kết thúc — không thể chỉnh sửa.");
                lblEditWarning.setVisible(true);
                lblEditWarning.setManaged(true);
            }
        } else if (hasAnyBid) {
            // Đang live nhưng đã có bid → ẩn nút, hiện cảnh báo
            setButtonsVisible(false, false);
            if (lblEditWarning != null) {
                lblEditWarning.setText("⚠  Đã có lượt đặt giá — không thể sửa hoặc xóa sản phẩm.");
                lblEditWarning.setVisible(true);
                lblEditWarning.setManaged(true);
            }
        } else {
            // Đang live, chưa có bid → cho phép edit/delete
            setButtonsVisible(true, true);
            if (lblEditWarning != null) {
                lblEditWarning.setVisible(false);
                lblEditWarning.setManaged(false);
            }
        }
    }

    private void setButtonsVisible(boolean showEdit, boolean showDelete) {
        if (btnEdit   != null) { btnEdit.setVisible(showEdit);   btnEdit.setManaged(showEdit); }
        if (btnDelete != null) { btnDelete.setVisible(showDelete); btnDelete.setManaged(showDelete); }
    }

    // ── Edit mode ────────────────────────────────────────────────

    /**
     * Seller bấm "Chỉnh sửa" → hiện form edit.
     *
     * Tại sao dùng toggle edit mode thay vì mở màn hình mới?
     * → Trải nghiệm liền mạch hơn: Seller thấy ngay kết quả chỉnh sửa
     *   ngay trên cùng màn hình, không cần điều hướng qua lại.
     */
    @FXML
    private void handleEdit() {
        isEditing = true;

        // Điền sẵn dữ liệu hiện tại vào form
        editTitleField.setText(item.title());
        editDescArea.setText(item.description());
        if (item.endTime() != null)
            editEndDatePicker.setValue(item.endTime().toLocalDate());

        newImageFile = null;
        resetEditImageBox();

        // Hiện form, ẩn nút Edit/Delete
        setVisible(editFormPane, true);
        setVisible(btnEdit,   false);
        setVisible(btnDelete, false);
        setVisible(btnSave,   true);
        setVisible(btnCancelEdit, true);
    }

    /** Seller bấm "Hủy" trong khi đang edit → quay về view mode */
    @FXML
    private void handleCancelEdit() {
        isEditing = false;
        setVisible(editFormPane, false);
        setVisible(btnSave,      false);
        setVisible(btnCancelEdit,false);
        refreshActionButtons();
    }

    /**
     * Seller bấm "Lưu" → validate, update DB, cập nhật UI và thông báo Home reload.
     *
     * Flow:
     *   1. Validate (title không trống, ngày hợp lệ)
     *   2. Load lại PhysicalItem từ DB theo id
     *   3. Cập nhật các trường thay đổi
     *   4. Gọi auctionRepo.update()
     *   5. Gọi onDataChanged callback → HomeController reload danh sách
     *   6. Cập nhật lại UI này (không cần đóng màn)
     */
    @FXML
    private void handleSave() {
        // ── Validate ────────────────────────────────────────────
        String newTitle = editTitleField.getText().trim();
        String newDesc  = editDescArea.getText().trim();
        LocalDate newEndDate = editEndDatePicker.getValue();

        if (newTitle.isBlank()) {
            AlertHelper.showError("Lỗi", "Tên sản phẩm không được để trống.");
            return;
        }
        if (newDesc.length() < 10) {
            AlertHelper.showError("Lỗi", "Mô tả phải có ít nhất 10 ký tự.");
            return;
        }
        if (newEndDate == null || !newEndDate.isAfter(LocalDate.now())) {
            AlertHelper.showError("Lỗi", "Ngày kết thúc phải sau hôm nay.");
            return;
        }

        // ── Load và update domain object ────────────────────────
        try {
            int auctionId = Integer.parseInt(item.id());

            // Lấy item hiện tại từ DB để không mất các field không có trên form
            // (condition, weight, shippingCost, v.v.)
            com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem dbItem =
                    auctionRepo.findById(auctionId)
                            .orElseThrow(() -> new IllegalStateException("Không tìm thấy sản phẩm #" + auctionId));

            dbItem.setTitle(newTitle);
            dbItem.setDescription(newDesc);
            dbItem.setEndTime(newEndDate.atTime(23, 59, 59));

            // Nếu Seller đã chọn ảnh mới → cập nhật đường dẫn
            if (newImageFile != null) {
                dbItem.setImageUrl(newImageFile.getAbsolutePath());
            }

            auctionRepo.update(dbItem);

            // ── Cập nhật local AuctionCardModel record ───────────────
            // AuctionCardModel là record (immutable) → tạo record mới với giá trị đã sửa.
            // Đây là pattern đúng khi dùng record: không thể mutate, phải tạo lại.
            this.item = new AuctionCardModel(
                    item.id(),
                    newTitle,
                    item.category(),
                    item.categoryEmoji(),
                    item.currentBid(),
                    item.startingPrice(),
                    newDesc,
                    item.bidCount(),
                    item.isLive(),
                    newEndDate.atTime(23, 59, 59),
                    item.imagePlaceholderEmoji(),
                    newImageFile != null ? newImageFile.getAbsolutePath() : item.imageUrl(),
                    item.sellerId()
            );

            // Cập nhật lại phần hiển thị (title, ảnh) mà không đóng màn
            lblTitle.setText(newTitle);
            refreshImageDisplay(this.item.imageUrl(), this.item.imagePlaceholderEmoji());

            // Thông báo Home reload để dữ liệu đồng bộ
            if (onDataChanged != null) onDataChanged.run();

            // Thoát chế độ edit
            handleCancelEdit();

            AlertHelper.showInfo("Đã lưu!", "Thông tin sản phẩm đã được cập nhật.");

        } catch (Exception e) {
            AlertHelper.showError("Lỗi", "Không thể lưu: " + e.getMessage());
        }
    }

    /** Seller bấm vào ô upload ảnh trong form edit → mở FileChooser */
    @FXML
    private void handleEditImageUpload() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn ảnh mới");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Hình ảnh", "*.jpg", "*.jpeg", "*.png", "*.gif"));
        File file = chooser.showOpenDialog(thisStage);
        if (file != null) {
            newImageFile = file;
            editImageHint.setText("✓  " + file.getName());
            editImageHint.setStyle("-fx-text-fill: #c9a84c;");

            // Preview ảnh nhỏ trong ô upload
            try {
                Image img = new Image(file.toURI().toString(), 200, 100, true, true, false);
                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(200); iv.setFitHeight(100);
                    iv.setPreserveRatio(true);
                    editImageBox.getChildren().clear();
                    VBox box = new VBox(6, iv, editImageHint);
                    box.setAlignment(Pos.CENTER);
                    editImageBox.getChildren().add(box);
                    return;
                }
            } catch (Exception ignored) {}
            // Fallback nếu preview lỗi
            editImageBox.getChildren().clear();
            editImageBox.getChildren().add(editImageHint);
        }
    }

    /** Reset ô upload ảnh về trạng thái mặc định */
    private void resetEditImageBox() {
        editImageBox.getChildren().clear();
        Label icon = new Label("📷");
        icon.setStyle("-fx-font-size: 24px;");
        Label hint = new Label("Bấm để thay ảnh (không bắt buộc)");
        hint.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        editImageHint = hint;
        VBox box = new VBox(6, icon, hint);
        box.setAlignment(Pos.CENTER);
        editImageBox.getChildren().add(box);
    }

    // ── Delete ───────────────────────────────────────────────────

    /**
     * Seller bấm "Xóa" → xác nhận rồi xóa khỏi DB.
     *
     * Tại sao cần xác nhận 2 lần?
     * → Xóa là hành động không thể hoàn tác. Hỏi lại giúp tránh click nhầm.
     *   Đây là UX pattern chuẩn cho destructive actions.
     */
    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("UBid");
        confirm.setHeaderText("Xóa sản phẩm?");
        confirm.setContentText(
                "Bạn chắc chắn muốn xóa \"" + item.title() + "\"?\n" +
                        "Hành động này không thể hoàn tác."
        );
        ButtonType yes = new ButtonType("Xóa", ButtonBar.ButtonData.OK_DONE);
        ButtonType no  = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);

        confirm.showAndWait().ifPresent(result -> {
            if (result == yes) {
                try {
                    auctionRepo.deleteById(Integer.parseInt(item.id()));
                    // Báo Home reload trước khi đóng màn này
                    if (onDataChanged != null) onDataChanged.run();
                    // Đóng màn hình seller detail
                    handleBack();
                } catch (Exception e) {
                    AlertHelper.showError("Lỗi", "Không thể xóa: " + e.getMessage());
                }
            }
        });
    }

    // ── Timer ────────────────────────────────────────────────────

    private void startCountdownTimer(LocalDateTime endTime) {
        timerScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "seller-detail-timer");
            t.setDaemon(true);
            return t;
        });
        timerScheduler.scheduleAtFixedRate(() -> {
            Duration d       = Duration.between(LocalDateTime.now(), endTime);
            long seconds     = Math.max(0, d.getSeconds());
            String days    = String.format("%02d", seconds / 86400);
            String hours   = String.format("%02d", (seconds % 86400) / 3600);
            String minutes = String.format("%02d", (seconds % 3600) / 60);
            String secs    = String.format("%02d", seconds % 60);
            Platform.runLater(() -> {
                lblDays.setText(days);
                lblHours.setText(hours);
                lblMinutes.setText(minutes);
                lblSeconds.setText(secs);
            });

            if (seconds <= 0) {
                // ── Phiên vừa kết thúc ────────────────────────────────────────
                // Bước 1: close DB trên timer thread (trước khi FX thread update UI)
                // Quan trọng: không đặt trong Platform.runLater() vì cần commit xong
                // trước khi onDataChanged gọi loadFromDb() ở HomeController
                try { auctionRepo.closeExpiredAuctions(); } catch (Exception ignored) {}

                // Bước 2: cập nhật item record (immutable record → tạo mới)
                this.item = new AuctionCardModel(
                        item.id(), item.title(), item.category(), item.categoryEmoji(),
                        item.currentBid(), item.startingPrice(), item.description(),
                        item.bidCount(),
                        false,           // isLive = false — phiên đã kết thúc
                        item.endTime(), item.imagePlaceholderEmoji(),
                        item.imageUrl(), item.sellerId()
                );

                // Bước 3: cập nhật UI và thông báo Home reload
                Platform.runLater(() -> {
                    // Đổi badge "● Đang đấu giá" → "✓ Đã kết thúc"
                    lblStatusBadge.setText("✓ Đã kết thúc");
                    lblStatusBadge.getStyleClass().removeAll("seller-badge-live");
                    if (!lblStatusBadge.getStyleClass().contains("seller-badge-ended"))
                        lblStatusBadge.getStyleClass().add("seller-badge-ended");

                    // Ẩn nút Chỉnh sửa/Xóa (không cho sửa phiên đã kết thúc)
                    refreshActionButtons();

                    // Báo HomeController reload để card ngoài trang chủ cũng cập nhật
                    if (onDataChanged != null) onDataChanged.run();
                });

                timerScheduler.shutdown();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    // ── FXML handler ─────────────────────────────────────────────

    @FXML
    private void handleBack() {
        if (timerScheduler != null && !timerScheduler.isShutdown())
            timerScheduler.shutdownNow();
        if (thisStage != null) thisStage.close();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private static void setVisible(javafx.scene.Node node, boolean v) {
        if (node == null) return;
        node.setVisible(v);
        node.setManaged(v);
    }

    private String formatPrice(double price) {
        if (price >= 1_000_000_000)
            return String.format("%.2f tỷ đ", price / 1_000_000_000);
        if (price >= 1_000_000)
            return String.format("%.1f triệu đ", price / 1_000_000);
        return String.format("%,.0f đ", price).replace(',', '.');
    }
}