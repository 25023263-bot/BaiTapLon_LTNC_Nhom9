package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model.AuctionCardModel;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.network.ServerConnection;

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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller cho màn hình chi tiết sản phẩm của NGƯỜI BÁN.
 *
 * Khác với ItemDetailController (dành cho Buyer, chỉ xem + đặt giá),
 * màn hình này cho phép Seller:
 *   1. Xem thông tin chi tiết phiên đấu giá của sản phẩm mình
 *   2. Chỉnh sửa tiêu đề, mô tả, ảnh (nếu chưa có bid nào)
 *   3. Xóa sản phẩm (nếu chưa có bid nào)
 *
 * <h3>Thay đổi so với phiên bản cũ:</h3>
 * <ul>
 *   <li>Bỏ {@code AuctionRepository} và {@code BidRepository} (không còn import
 *       ServiceLocator).</li>
 *   <li>Mọi thao tác dữ liệu đi qua {@link ServerConnection}.</li>
 *   <li>Lịch sử bid load qua {@code GET_AUCTION_DETAIL}.</li>
 *   <li>Save và Delete gửi request CANCEL_AUCTION / (TODO: UPDATE_AUCTION)
 *       qua socket. Trong giai đoạn chuyển tiếp, update vẫn dùng DB trực tiếp
 *       vì chưa có request type UPDATE_AUCTION — xem TODO bên dưới.</li>
 * </ul>
 */
public class SellerItemDetailController {

    private static final Logger LOG = Logger.getLogger(SellerItemDetailController.class.getName());

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

    // Edit form
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
    private AuctionCardModel item;
    private Runnable onDataChanged;
    private boolean isEditing = false;
    private File newImageFile = null;
    private ScheduledExecutorService timerScheduler;

    // ── Public API ───────────────────────────────────────────────

    /**
     * Gọi bởi SellerItemDetailCoordinator ngay sau khi load FXML.
     * Load lịch sử bid từ server qua socket.
     */
    public void configure(Stage stage, AuctionCardModel item, Runnable onDataChanged) {
        this.thisStage    = stage;
        this.item         = item;
        this.onDataChanged = onDataChanged;

        populateView(item);
        loadBidHistoryFromServer();

        if (item.isLive() && item.endTime() != null) {
            startCountdownTimer(item.endTime());
        } else {
            lblDays.setText("00"); lblHours.setText("00");
            lblMinutes.setText("00"); lblSeconds.setText("00");
        }

        refreshActionButtons();
    }

    // ── Populate ─────────────────────────────────────────────────

    private void populateView(AuctionCardModel it) {
        lblTitle.setText(it.title());

        String statusText = it.isLive() ? "● Đang đấu giá" : "✓ Đã kết thúc";
        lblStatusBadge.setText(statusText);
        lblStatusBadge.getStyleClass().removeAll("seller-badge-live", "seller-badge-ended");
        lblStatusBadge.getStyleClass().add(it.isLive() ? "seller-badge-live" : "seller-badge-ended");

        refreshImageDisplay(it.imageUrl(), it.imagePlaceholderEmoji());

        lblStartingPrice.setText(formatPrice(it.startingPrice()));
        lblCurrentPrice.setText(formatPrice(it.currentBid()));
        lblBidCount.setText(it.bidCount() + " lượt đấu giá");
    }

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

    // ── Bid history (qua socket) ─────────────────────────────────

    /**
     * Load lịch sử bid từ server qua GET_AUCTION_DETAIL (background thread).
     * Sau khi nhận dữ liệu, cập nhật UI trên FX thread.
     */
    private void loadBidHistoryFromServer() {
        vboxBidHistory.getChildren().clear();
        if (!ServerConnection.isConnected()) {
            showBidHistoryError("Server chưa kết nối.");
            return;
        }

        int auctionId = Integer.parseInt(item.id());
        Thread t = new Thread(() -> {
            try {
                var dto = ServerConnection.getAuctionDetail(auctionId);
                Platform.runLater(() -> {
                    int count = dto.getTotalBids();
                    lblBidCount.setText(count + " lượt đấu giá");
                    if (lblBidCountSmall != null)
                        lblBidCountSmall.setText(count + " lượt");

                    // Cập nhật giá hiện tại nếu server có dữ liệu mới hơn
                    if (dto.getCurrentPrice() != null) {
                        lblCurrentPrice.setText(formatPrice(dto.getCurrentPrice().doubleValue()));
                    }

                    // bidCount từ server → cập nhật item (để refreshActionButtons đúng)
                    item = new AuctionCardModel(
                            item.id(), item.title(), item.category(), item.categoryEmoji(),
                            dto.getCurrentPrice() != null ? dto.getCurrentPrice().doubleValue() : item.currentBid(),
                            item.startingPrice(), item.description(),
                            count,
                            item.isLive(), item.endTime(), item.imagePlaceholderEmoji(),
                            item.imageUrl(), item.sellerId()
                    );
                    refreshActionButtons();

                    // Server GET_AUCTION_DETAIL không trả về danh sách bid chi tiết
                    // (chỉ trả totalBids). Hiển thị thông báo phù hợp.
                    if (count == 0) {
                        Label empty = new Label("Chưa có lượt đấu giá nào.");
                        empty.setStyle("-fx-text-fill: #888; -fx-padding: 12;");
                        vboxBidHistory.getChildren().add(empty);
                    } else {
                        Label info = new Label("✓  " + count + " lượt đã được đặt.");
                        info.setStyle("-fx-text-fill: #c9a84c; -fx-padding: 12;");
                        if (dto.getLeadingBidderUsername() != null) {
                            Label leader = new Label("👑  Đang dẫn đầu: "
                                    + dto.getLeadingBidderUsername());
                            leader.setStyle("-fx-text-fill: #2ecc71; -fx-padding: 4 12;");
                            vboxBidHistory.getChildren().addAll(info, leader);
                        } else {
                            vboxBidHistory.getChildren().add(info);
                        }
                    }
                });
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Không thể load bid history qua socket", e);
                Platform.runLater(() -> showBidHistoryError("Không thể tải lịch sử: " + e.getMessage()));
            }
        }, "seller-detail-bid-loader");
        t.setDaemon(true);
        t.start();
    }

    private void showBidHistoryError(String msg) {
        Label err = new Label(msg);
        err.setStyle("-fx-text-fill: #e74c3c; -fx-padding: 12;");
        vboxBidHistory.getChildren().add(err);
    }

    // ── Action buttons ───────────────────────────────────────────

    private void refreshActionButtons() {
        boolean hasAnyBid = item.bidCount() > 0;
        boolean isEnded   = !item.isLive();

        if (isEnded) {
            setButtonsVisible(false, false);
            if (lblEditWarning != null) {
                lblEditWarning.setText("⏰  Phiên đấu giá đã kết thúc — không thể chỉnh sửa.");
                lblEditWarning.setVisible(true);
                lblEditWarning.setManaged(true);
            }
        } else if (hasAnyBid) {
            setButtonsVisible(false, false);
            if (lblEditWarning != null) {
                lblEditWarning.setText("⚠  Đã có lượt đặt giá — không thể sửa hoặc xóa sản phẩm.");
                lblEditWarning.setVisible(true);
                lblEditWarning.setManaged(true);
            }
        } else {
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

    @FXML
    private void handleEdit() {
        isEditing = true;
        editTitleField.setText(item.title());
        editDescArea.setText(item.description());
        if (item.endTime() != null)
            editEndDatePicker.setValue(item.endTime().toLocalDate());
        newImageFile = null;
        resetEditImageBox();
        setVisible(editFormPane, true);
        setVisible(btnEdit,   false);
        setVisible(btnDelete, false);
        setVisible(btnSave,   true);
        setVisible(btnCancelEdit, true);
    }

    @FXML
    private void handleCancelEdit() {
        isEditing = false;
        setVisible(editFormPane, false);
        setVisible(btnSave,      false);
        setVisible(btnCancelEdit,false);
        refreshActionButtons();
    }

    /**
     * Lưu thay đổi.
     *
     * TODO: Thêm request type UPDATE_AUCTION vào protocol để hoàn toàn loại bỏ DB ở client.
     *       Hiện tại sử dụng ServiceLocator tạm thời vì server chưa có endpoint update.
     *       Khi server hỗ trợ UPDATE_AUCTION, thay khối try bên dưới bằng:
     *         ServerConnection.updateAuction(auctionId, newTitle, newDesc, newEndDate, newImagePath)
     */
    @FXML
    private void handleSave() {
        String newTitle  = editTitleField.getText().trim();
        String newDesc   = editDescArea.getText().trim();
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

        // TODO: Thay bằng ServerConnection.updateAuction(...) khi server hỗ trợ.
        // Hiện tại: dùng ServiceLocator tạm thời.
        try {
            int auctionId = Integer.parseInt(item.id());
            var locator = com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.ServiceLocator.getInstance();
            AuctionItem dbItem = locator.getAuctionRepo().findById(auctionId)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy sản phẩm #" + auctionId));

            dbItem.setTitle(newTitle);
            dbItem.setDescription(newDesc);
            dbItem.setEndTime(newEndDate.atTime(23, 59, 59));
            if (newImageFile != null) {
                dbItem.setImageUrl(newImageFile.getAbsolutePath());
            }
            locator.getAuctionRepo().update(dbItem);

            this.item = new AuctionCardModel(
                    item.id(), newTitle, item.category(), item.categoryEmoji(),
                    item.currentBid(), item.startingPrice(), newDesc,
                    item.bidCount(), item.isLive(),
                    newEndDate.atTime(23, 59, 59),
                    item.imagePlaceholderEmoji(),
                    newImageFile != null ? newImageFile.getAbsolutePath() : item.imageUrl(),
                    item.sellerId()
            );

            lblTitle.setText(newTitle);
            refreshImageDisplay(this.item.imageUrl(), this.item.imagePlaceholderEmoji());
            if (onDataChanged != null) onDataChanged.run();
            handleCancelEdit();
            AlertHelper.showInfo("Đã lưu!", "Thông tin sản phẩm đã được cập nhật.");

        } catch (Exception e) {
            AlertHelper.showError("Lỗi", "Không thể lưu: " + e.getMessage());
        }
    }

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
            editImageBox.getChildren().clear();
            editImageBox.getChildren().add(editImageHint);
        }
    }

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

    // ── Delete (qua socket) ───────────────────────────────────────

    /**
     * Hủy phiên đấu giá qua socket (CANCEL_AUCTION).
     *
     * Lưu ý: "Xóa" ở đây thực ra là hủy phiên (cancel), không phải xóa khỏi DB.
     * Điều này phù hợp hơn với business logic — lịch sử đấu giá cần được giữ lại.
     */
    @FXML
    private void handleDelete() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("UBid");
        confirm.setHeaderText("Hủy phiên đấu giá?");
        confirm.setContentText(
                "Bạn chắc chắn muốn hủy \"" + item.title() + "\"?\n" +
                        "Hành động này không thể hoàn tác."
        );
        ButtonType yes = new ButtonType("Hủy phiên", ButtonBar.ButtonData.OK_DONE);
        ButtonType no  = new ButtonType("Giữ lại",   ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);

        confirm.showAndWait().ifPresent(result -> {
            if (result != yes) return;

            if (!ServerConnection.isConnected()) {
                AlertHelper.showError("Lỗi kết nối",
                        "Không thể kết nối đến server. Vui lòng thử lại.");
                return;
            }

            // Chạy trên background thread
            Thread t = new Thread(() -> {
                try {
                    // Seller hủy: truyền cả sellerId để server kiểm tra quyền
                    ServerConnection.cancelAuction(Integer.parseInt(item.id()), item.sellerId());
                    Platform.runLater(() -> {
                        if (onDataChanged != null) onDataChanged.run();
                        handleBack();
                    });
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Không thể hủy phiên #" + item.id(), e);
                    Platform.runLater(() ->
                            AlertHelper.showError("Lỗi", "Không thể hủy phiên: " + e.getMessage()));
                }
            }, "seller-cancel-auction-thread");
            t.setDaemon(true);
            t.start();
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
                // Server tự xử lý đóng phiên qua AuctionScheduler
                // Client chỉ cần cập nhật UI
                this.item = new AuctionCardModel(
                        item.id(), item.title(), item.category(), item.categoryEmoji(),
                        item.currentBid(), item.startingPrice(), item.description(),
                        item.bidCount(),
                        false,       // isLive = false
                        item.endTime(), item.imagePlaceholderEmoji(),
                        item.imageUrl(), item.sellerId()
                );

                Platform.runLater(() -> {
                    lblStatusBadge.setText("✓ Đã kết thúc");
                    lblStatusBadge.getStyleClass().removeAll("seller-badge-live");
                    lblStatusBadge.getStyleClass().add("seller-badge-ended");
                    setButtonsVisible(false, false);
                    if (lblEditWarning != null) {
                        lblEditWarning.setText("⏰  Phiên đấu giá đã kết thúc.");
                        lblEditWarning.setVisible(true);
                        lblEditWarning.setManaged(true);
                    }
                    if (onDataChanged != null) onDataChanged.run();
                });

                timerScheduler.shutdownNow();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    // ── Back ─────────────────────────────────────────────────────

    @FXML
    private void handleBack() {
        if (timerScheduler != null && !timerScheduler.isShutdown()) {
            timerScheduler.shutdownNow();
        }
        if (thisStage != null) thisStage.close();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private String formatPrice(double amount) {
        return String.format("%,.0f đ", amount);
    }

    private static void setVisible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
