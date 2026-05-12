package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller.HomeController.AuctionItem;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller cho màn hình chi tiết sản phẩm đấu giá.
 *
 * Điểm khác biệt chính so với bản cũ:
 *   1. lvBidHistory (ListView<String>) → vboxBidHistory (VBox):
 *      mỗi lượt đấu giá là một HBox được build trong Java, tự do style.
 *   2. thisStage lưu trực tiếp từ configure() → handleBack() hoạt động đúng.
 */
public class ItemDetailController {

    // ── FXML fields ──────────────────────────────────────────────
    @FXML private BorderPane rootPane;

    @FXML private Label lblBreadcrumb;
    @FXML private Label lblTitle;
    @FXML private Label lblViewCount;
    @FXML private Label lblBidCount;
    @FXML private Label lblBidCountSmall;  // badge "X lượt" trên header lịch sử
    @FXML private Label lblMainImageGlyph;

    @FXML private Label lblDays;
    @FXML private Label lblHours;
    @FXML private Label lblMinutes;
    @FXML private Label lblSeconds;

    @FXML private Label lblStartingPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblDescription;

    @FXML private VBox   vboxBidHistory;  // thay cho ListView cũ
    @FXML private Button btnPlaceBid;

    // ── State ────────────────────────────────────────────────────
    private Stage    thisStage;
    private Runnable onPlaceBid;
    private AuctionItem auctionItem;

    // ── Public API ───────────────────────────────────────────────

    public void configure(Window owner, AuctionItem item, Runnable onPlaceBid) {
        this.auctionItem = item;
        this.onPlaceBid  = onPlaceBid;

        // Lưu stage để handleBack() đóng trực tiếp, không cần rootPane
        if (owner instanceof Stage s) this.thisStage = s;

        lblTitle.setText(item.title());
        lblBreadcrumb.setText("Trang chủ  >  Đấu giá  >  " + item.title());
        lblViewCount.setText("~ " + item.bidCount() * 6 + " lượt xem");
        lblBidCount.setText(item.bidCount() + " lượt đấu giá");
        lblMainImageGlyph.setText(item.imagePlaceholderEmoji());

        lblStartingPrice.setText(formatPrice(item.currentBid() * 0.6));
        lblCurrentPrice.setText(formatPrice(item.currentBid()));

        if (item.isLive() && item.endTime() != null) {
            updateTimer(item.endTime());
        } else {
            lblDays.setText("00"); lblHours.setText("00");
            lblMinutes.setText("00"); lblSeconds.setText("00");
        }

        lblDescription.setText(
                "Sản phẩm đấu giá cao cấp thuộc danh mục " + item.category() + ". "
                        + "Được kiểm định chất lượng bởi các chuyên gia hàng đầu và lưu trữ "
                        + "an toàn trong kho bảo mật đạt chuẩn quốc tế. Tình trạng: như mới, "
                        + "đầy đủ hộp và giấy tờ kèm theo. Giao hàng toàn quốc, bảo hiểm "
                        + "vận chuyển 100%.");

        populateBidHistory(item);
    }

    // ── Timer ────────────────────────────────────────────────────

    private void updateTimer(LocalDateTime endTime) {
        Duration d   = Duration.between(LocalDateTime.now(), endTime);
        long seconds = Math.max(0, d.getSeconds());
        lblDays.setText(String.format("%02d", seconds / 86400));
        lblHours.setText(String.format("%02d", (seconds % 86400) / 3600));
        lblMinutes.setText(String.format("%02d", (seconds % 3600) / 60));
        lblSeconds.setText(String.format("%02d", seconds % 60));
    }

    // ── Bid history ──────────────────────────────────────────────

    /**
     * Build danh sách lịch sử đấu giá dưới dạng custom rows.
     *
     * TẠI SAO dùng VBox + HBox thay vì ListView?
     *   ListView chỉ render một String trên nền trắng, không thể style
     *   từng phần tử riêng lẻ (tên, giá, thời gian). Với VBox ta tự tạo
     *   từng Node và gắn CSS class khác nhau → kiểm soát hoàn toàn UI.
     */
    private void populateBidHistory(AuctionItem item) {
        vboxBidHistory.getChildren().clear();

        // Record tạm — sau này thay bằng BidDTO từ service/repository
        record BidEntry(String name, double amount, String timeAgo) {}

        List<BidEntry> entries = List.of(
                new BidEntry("Nguyễn Văn Anh",  item.currentBid(),        "5 phút trước"),
                new BidEntry("Lê Minh Khôi",    item.currentBid() * 0.92, "12 phút trước"),
                new BidEntry("Trần Quang Hải",  item.currentBid() * 0.85, "28 phút trước"),
                new BidEntry("Phạm Thị Lan",    item.currentBid() * 0.78, "45 phút trước"),
                new BidEntry("Hoàng Đức Nam",   item.currentBid() * 0.71, "1 giờ trước")
        );

        if (lblBidCountSmall != null) {
            lblBidCountSmall.setText(entries.size() + " lượt");
        }

        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            vboxBidHistory.getChildren().add(
                    buildBidRow(e.name(), e.amount(), e.timeAgo(), i == 0)
            );
        }
    }

    /**
     * Tạo một HBox row cho một lượt đặt giá.
     *
     * Layout:
     *   [Avatar]  [VBox: tên + phụ đề]  [Spacer]  [VBox: giá + thời gian]
     *
     * @param isLeading  true = người đang dẫn đầu → dùng style vàng + badge 👑
     */
    private HBox buildBidRow(String name, double amount, String timeAgo,
                             boolean isLeading) {

        // ── Avatar circle ─────────────────────────────────────────
        Label avatarLabel = new Label(String.valueOf(name.charAt(0)).toUpperCase());
        avatarLabel.getStyleClass().add(isLeading ? "history-avatar-lead" : "history-avatar");

        StackPane avatarWrap = new StackPane(avatarLabel);
        avatarWrap.getStyleClass().add("history-avatar-wrap");
        avatarWrap.setMinSize(40, 40);
        avatarWrap.setMaxSize(40, 40);

        // ── Tên + phụ đề ─────────────────────────────────────────
        Label lblName = new Label(name);
        lblName.getStyleClass().add("history-name");

        Label lblSub = new Label(isLeading ? "👑  Đang dẫn đầu" : "Đã đặt giá");
        lblSub.getStyleClass().add(isLeading ? "history-sub-lead" : "history-sub");

        VBox nameBox = new VBox(2, lblName, lblSub);
        nameBox.setAlignment(Pos.CENTER_LEFT);

        // ── Spacer ────────────────────────────────────────────────
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ── Giá + thời gian ───────────────────────────────────────
        Label lblAmount = new Label(formatPrice(amount));
        lblAmount.getStyleClass().add(isLeading ? "history-price-lead" : "history-price");

        Label lblTime = new Label(timeAgo);
        lblTime.getStyleClass().add("history-time");

        VBox priceBox = new VBox(2, lblAmount, lblTime);
        priceBox.setAlignment(Pos.CENTER_RIGHT);

        // ── Row tổng hợp ──────────────────────────────────────────
        HBox row = new HBox(12, avatarWrap, nameBox, spacer, priceBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(isLeading ? "history-row-lead" : "history-row");
        row.setMinHeight(56);

        return row;
    }

    // ── Formatting ───────────────────────────────────────────────

    private String formatPrice(double price) {
        if (price >= 1_000_000_000) {
            return String.format("%.2f tỷ đ", price / 1_000_000_000);
        }
        if (price >= 1_000_000) {
            return String.format("%.1f triệu đ", price / 1_000_000);
        }
        return String.format("%,.0f đ", price).replace(',', '.');
    }

    // ── FXML handlers ────────────────────────────────────────────

    @FXML
    private void handleBack() {
        if (thisStage != null) thisStage.close();
    }

    @FXML
    private void handlePlaceBid() {
        if (onPlaceBid != null) onPlaceBid.run();
    }
}