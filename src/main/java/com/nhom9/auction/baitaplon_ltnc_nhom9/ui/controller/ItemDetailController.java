package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model.AuctionCardModel;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.stage.Stage;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;

public class ItemDetailController {

    // ── FXML fields ──────────────────────────────────────────────
    @FXML private BorderPane rootPane;

    @FXML private Label lblBreadcrumb;
    @FXML private Label lblTitle;
    @FXML private Label lblBidCount;
    @FXML private Label lblBidCountSmall;

    /**
     * imageContainer: StackPane chứa ảnh thật (ImageView) hoặc fallback emoji (Label).
     * Được populate bằng loadImageInto() trong configure(), giống cơ chế HomeController.
     * FXML không còn Label lblMainImageGlyph — controller tự inject node vào container.
     */
    @FXML private StackPane imageContainer;

    @FXML private Label lblDays;
    @FXML private Label lblHours;
    @FXML private Label lblMinutes;
    @FXML private Label lblSeconds;

    @FXML private Label lblStartingPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblDescription;

    @FXML private VBox   vboxBidHistory;
    @FXML private Button btnPlaceBid;

    @FXML private StackPane chartContainer;
    @FXML private Label     lblChartEmpty;

    // ── State ────────────────────────────────────────────────────
    private Stage    thisStage;
    private Runnable onPlaceBid;
    private AuctionCardModel auctionItem;
    private ScheduledExecutorService timerScheduler;
    private List<Bid> currentBids = new ArrayList<>();
    private Canvas chartCanvas;
    /**
     * Giá khởi điểm của phiên — dùng làm floor (sàn) cho trục Y biểu đồ.
     * Trục Y không bao giờ xuống dưới giá này vì giá đấu giá chỉ tăng.
     */
    private double chartStartingPrice = 0;

    // ── Public API ───────────────────────────────────────────────

    public void configure(Stage owner, AuctionCardModel item, Runnable onPlaceBid) {
        this.auctionItem = item;
        this.onPlaceBid  = onPlaceBid;

        if (owner instanceof Stage s) this.thisStage = s;

        lblTitle.setText(item.title());
        lblBreadcrumb.setText("Trang chủ  >  Đấu giá  >  " + item.title());
        lblBidCount.setText(item.bidCount() + " lượt đấu giá");

        // Load ảnh thật vào imageContainer (giống HomeController.buildImageNode)
        loadImageInto(imageContainer,
                item.imageUrl(),
                item.imagePlaceholderEmoji(),
                260, 220);

        lblStartingPrice.setText(formatPrice(item.startingPrice()));
        lblCurrentPrice.setText(formatPrice(item.currentBid()));

        if (item.isLive() && item.endTime() != null) {
            startCountdownTimer(item.endTime());
        } else {
            lblDays.setText("00"); lblHours.setText("00");
            lblMinutes.setText("00"); lblSeconds.setText("00");
        }

        lblDescription.setText(item.description().isBlank() ? "Không có mô tả." : item.description());
        chartStartingPrice = item.startingPrice(); // khởi tạo floor trục Y ngay từ đầu
        populateBidHistory(List.of(), item.startingPrice());
        setupChart();
    }

    // ── Image loading ─────────────────────────────────────────────

    /**
     * Load ảnh thật từ imageUrl vào container (StackPane).
     * Logic giống HomeController.buildImageNode():
     *   1. Thử load file từ đường dẫn tuyệt đối → chuyển sang URI
     *   2. Kiểm tra img.isError() — JavaFX không throw ngay khi URI sai
     *   3. Nếu thành công → thêm ImageView vào container, căn giữa
     *   4. Nếu thất bại → thêm Label emoji làm fallback
     *
     * Tại sao dùng StackPane thay vì Label?\
     * → StackPane cho phép chứa cả ImageView (ảnh thật) lẫn Label (fallback)
     *   mà không cần thay đổi cấu trúc FXML. Controller quyết định node nào
     *   được thêm vào tùy theo dữ liệu thực tế.
     *
     * @param container  StackPane trong FXML sẽ chứa ảnh
     * @param imageUrl   đường dẫn file tuyệt đối lưu trong DB (có thể null/rỗng)
     * @param emoji      emoji fallback nếu ảnh không load được
     * @param width      chiều rộng mong muốn của ảnh (px)
     * @param height     chiều cao mong muốn của ảnh (px)
     */
    private void loadImageInto(StackPane container, String imageUrl,
                               String emoji, double width, double height) {
        if (container == null) return;
        container.getChildren().clear();

        if (imageUrl != null && !imageUrl.isBlank()) {
            try {
                // Chuyển đường dẫn file hệ thống → URI mà JavaFX Image hiểu
                // VD: "C:\photos\mac.jpg" → "file:///C:/photos/mac.jpg"
                String uri = new File(imageUrl).toURI().toString();
                Image img = new Image(uri, width, height, true, true, false);

                if (!img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(width);
                    iv.setFitHeight(height);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    // clip bo góc cho đẹp (giống style card ở home)
                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(width, height);
                    clip.setArcWidth(14);
                    clip.setArcHeight(14);
                    iv.setClip(clip);
                    container.getChildren().add(iv);
                    container.setAlignment(Pos.CENTER);
                    return;
                }
            } catch (Exception ignored) {
                // File không tồn tại hoặc không phải ảnh → fallback emoji
            }
        }

        // Fallback: Label emoji
        Label fallback = new Label(emoji);
        fallback.setStyle("-fx-font-size: 36px; -fx-text-fill: #f5f5f5;");
        container.getChildren().add(fallback);
        container.setAlignment(Pos.CENTER);
    }

    // ── Timer ────────────────────────────────────────────────────

    private void startCountdownTimer(LocalDateTime endTime) {
        timerScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "detail-timer");
            t.setDaemon(true);
            return t;
        });
        timerScheduler.scheduleAtFixedRate(() -> {
            Duration d   = Duration.between(LocalDateTime.now(), endTime);
            long seconds = Math.max(0, d.getSeconds());
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
            if (seconds <= 0) timerScheduler.shutdown();
        }, 0, 1, TimeUnit.SECONDS);
    }

    // ── Refresh sau khi có bid mới ────────────────────────────────

    public void refreshAfterBid(double newCurrentBid, int newBidCount, List<Bid> bids) {
        lblCurrentPrice.setText(formatPrice(newCurrentBid));
        lblBidCount.setText(newBidCount + " lượt đấu giá");
        populateBidHistory(bids, auctionItem.startingPrice());
        updateChart(bids, auctionItem.startingPrice());
    }

    /**
     * Anti-snipe: được gọi khi phiên vừa được gia hạn.
     * Dừng timer cũ và khởi động lại với {@code newEndTime} mới.
     *
     * <p>Gọi từ {@code ItemDetailCoordinator.onBidConfirmed()} sau khi
     * đọc lại {@code end_time} từ DB — đảm bảo timer đồng bộ với server.</p>
     *
     * @param newEndTime thời điểm kết thúc mới (sau gia hạn)
     */
    public void refreshEndTime(LocalDateTime newEndTime) {
        // Dừng timer cũ trước (tránh 2 timer cùng chạy)
        if (timerScheduler != null && !timerScheduler.isShutdown())
            timerScheduler.shutdownNow();

        // Khởi động timer mới với endTime được gia hạn
        startCountdownTimer(newEndTime);

        // Thông báo nhẹ trên UI (chạy trên JavaFX thread)
        Platform.runLater(() -> {
            // Flash nhẹ label giây để người dùng biết timer đã reset
            lblSeconds.setStyle("-fx-text-fill: #f2d67e; -fx-font-weight: bold;");
            // Sau 2 giây trả về style gốc
            ScheduledExecutorService flashReset = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "timer-flash-reset");
                t.setDaemon(true);
                return t;
            });
            flashReset.schedule(() ->
                            Platform.runLater(() -> lblSeconds.setStyle("")),
                    2, TimeUnit.SECONDS);
            flashReset.shutdown();
        });
    }

    // ── Bid History List ──────────────────────────────────────────

    private void populateBidHistory(List<Bid> bids, double startingPrice) {
        vboxBidHistory.getChildren().clear();

        if (lblBidCountSmall != null)
            lblBidCountSmall.setText(bids.size() + " lượt");

        if (bids.isEmpty()) {
            Label empty = new Label("Chưa có lượt đấu giá nào.");
            empty.setStyle("-fx-text-fill: #888; -fx-padding: 12;");
            vboxBidHistory.getChildren().add(empty);
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm  dd/MM");
        for (int i = 0; i < bids.size(); i++) {
            Bid b = bids.get(i);
            if (b.getAmount().doubleValue() < startingPrice) continue;
            String name    = b.getBuyerUsername() != null ? b.getBuyerUsername() : "Ẩn danh";
            String timeStr = b.getBidTime() != null ? b.getBidTime().format(fmt) : "--";
            vboxBidHistory.getChildren().add(
                    buildBidRow(name, b.getAmount().doubleValue(), timeStr, i == 0)
            );
        }
    }

    private HBox buildBidRow(String name, double amount, String timeAgo, boolean isLeading) {
        Label avatarLabel = new Label(String.valueOf(name.charAt(0)).toUpperCase());
        avatarLabel.getStyleClass().add(isLeading ? "history-avatar-lead" : "history-avatar");
        StackPane avatarWrap = new StackPane(avatarLabel);
        avatarWrap.getStyleClass().add("history-avatar-wrap");
        avatarWrap.setMinSize(36, 36);
        avatarWrap.setMaxSize(36, 36);

        Label lblName = new Label(name);
        lblName.getStyleClass().add("history-name");
        Label lblSub = new Label(isLeading ? "👑  Đang dẫn đầu" : "Đã đặt giá");
        lblSub.getStyleClass().add(isLeading ? "history-sub-lead" : "history-sub");
        VBox nameBox = new VBox(2, lblName, lblSub);
        nameBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblAmount = new Label(formatPrice(amount));
        lblAmount.getStyleClass().add(isLeading ? "history-price-lead" : "history-price");
        Label lblTime = new Label(timeAgo);
        lblTime.getStyleClass().add("history-time");
        VBox priceBox = new VBox(2, lblAmount, lblTime);
        priceBox.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(12, avatarWrap, nameBox, spacer, priceBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(isLeading ? "history-row-lead" : "history-row");
        row.setMinHeight(52);
        return row;
    }

    // ── Price Curve Chart ─────────────────────────────────────────

    private void setupChart() {
        if (chartContainer == null) return;

        chartCanvas = new Canvas(400, 160);
        chartCanvas.widthProperty().bind(chartContainer.widthProperty());
        chartCanvas.heightProperty().bind(chartContainer.heightProperty());
        chartCanvas.widthProperty().addListener((obs, o, n) -> redrawChart());
        chartCanvas.heightProperty().addListener((obs, o, n) -> redrawChart());

        chartContainer.getChildren().add(chartCanvas);
        drawEmptyChart();
    }

    private void updateChart(List<Bid> bids, double startingPrice) {
        if (chartCanvas == null) return;
        currentBids = bids.stream()
                .filter(b -> b.getAmount() != null
                        && b.getAmount().doubleValue() >= startingPrice
                        && b.getBidTime() != null)
                .sorted((a, b2) -> a.getBidTime().compareTo(b2.getBidTime()))
                .collect(java.util.stream.Collectors.toList());
        // Lưu lại startingPrice để dùng trong redrawChart() tính floor trục Y
        this.chartStartingPrice = startingPrice;
        redrawChart();
    }

    private void redrawChart() {
        if (chartCanvas == null) return;

        Platform.runLater(() -> {
            double w = chartCanvas.getWidth();
            double h = chartCanvas.getHeight();
            if (w <= 0 || h <= 0) return;

            GraphicsContext gc = chartCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, w, h);

            if (currentBids.size() < 2) {
                drawEmptyChart();
                return;
            }

            double padLeft = 60, padRight = 20, padTop = 20, padBottom = 40;
            double plotW = w - padLeft - padRight;
            double plotH = h - padTop - padBottom;

            double minPrice = currentBids.stream().mapToDouble(b -> b.getAmount().doubleValue()).min().orElse(0);
            double maxPrice = currentBids.stream().mapToDouble(b -> b.getAmount().doubleValue()).max().orElse(minPrice + 1);
            double priceRange = maxPrice - minPrice;

            // ── FIX: Trục Y không được xuống dưới startingPrice ──────────────
            //
            // Trước đây: priceMin = minPrice - priceRange * 0.1
            // → Khi auto-bid tạo ra nhiều bid gần nhau, priceRange lớn
            //   → priceMin có thể âm hoặc rất nhỏ → đường giá vẽ đi xuống
            //
            // Giải pháp:
            //   - Sàn (floor) của trục Y = chartStartingPrice (giá khởi điểm)
            //     vì trong đấu giá, giá không bao giờ thấp hơn giá khởi điểm
            //   - Chỉ thêm padding lên TRÊN (priceMax), không trừ xuống dưới
            //   - Nếu tất cả bid cùng giá (priceRange = 0): mở rộng đều 2 phía
            double priceMin = Math.min(minPrice, chartStartingPrice); // floor = startingPrice
            double priceMax = maxPrice + priceRange * 0.15;           // padding 15% lên trên
            if (priceMax == priceMin) { priceMin = Math.max(0, priceMin - 1000); priceMax += 1000; }

            LocalDateTime timeMin = currentBids.get(0).getBidTime();
            LocalDateTime timeMax = currentBids.get(currentBids.size() - 1).getBidTime();
            long totalSeconds = Duration.between(timeMin, timeMax).getSeconds();
            if (totalSeconds == 0) totalSeconds = 1;

            final long fTotal = totalSeconds;
            final double fPMin = priceMin, fPMax = priceMax;

            gc.setFill(Color.web("#0a0a14"));
            gc.fillRoundRect(0, 0, w, h, 12, 12);

            gc.setStroke(Color.web("#1e1e30"));
            gc.setLineWidth(1);
            int gridCount = 4;
            for (int i = 0; i <= gridCount; i++) {
                double y = padTop + plotH * i / gridCount;
                gc.strokeLine(padLeft, y, padLeft + plotW, y);
                double price = fPMax - (fPMax - fPMin) * i / gridCount;
                gc.setFill(Color.web("#505068"));
                gc.setFont(javafx.scene.text.Font.font("Arial", 10));
                gc.fillText(formatPriceShort(price), 2, y + 4);
            }

            double[] xs = new double[currentBids.size()];
            double[] ys = new double[currentBids.size()];
            for (int i = 0; i < currentBids.size(); i++) {
                Bid b = currentBids.get(i);
                long elapsed = Duration.between(timeMin, b.getBidTime()).getSeconds();
                xs[i] = padLeft + plotW * elapsed / fTotal;
                double ratio = (b.getAmount().doubleValue() - fPMin) / (fPMax - fPMin);
                ys[i] = padTop + plotH * (1.0 - ratio);
            }

            gc.setFill(new LinearGradient(0, padTop, 0, padTop + plotH, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#d9b65c", 0.35)),
                    new Stop(1, Color.web("#d9b65c", 0.0))));
            gc.beginPath();
            gc.moveTo(xs[0], padTop + plotH);
            gc.lineTo(xs[0], ys[0]);
            for (int i = 1; i < xs.length; i++) gc.lineTo(xs[i], ys[i]);
            gc.lineTo(xs[xs.length - 1], padTop + plotH);
            gc.closePath();
            gc.fill();

            gc.setStroke(Color.web("#d9b65c"));
            gc.setLineWidth(2.5);
            gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            gc.beginPath();
            gc.moveTo(xs[0], ys[0]);
            for (int i = 1; i < xs.length; i++) gc.lineTo(xs[i], ys[i]);
            gc.stroke();

            for (int i = 0; i < xs.length; i++) {
                boolean isLast = (i == xs.length - 1);
                if (isLast) {
                    gc.setFill(Color.web("#d9b65c", 0.2));
                    gc.fillOval(xs[i] - 10, ys[i] - 10, 20, 20);
                    gc.setFill(Color.web("#f2d67e"));
                    gc.fillOval(xs[i] - 6, ys[i] - 6, 12, 12);
                    gc.setFill(Color.web("#ffffff", 0.9));
                    gc.fillOval(xs[i] - 2.5, ys[i] - 2.5, 5, 5);
                } else {
                    gc.setFill(Color.web("#c9a84c"));
                    gc.fillOval(xs[i] - 3.5, ys[i] - 3.5, 7, 7);
                }
            }

            DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
            gc.setFill(Color.web("#505068"));
            gc.setFont(javafx.scene.text.Font.font("Arial", 10));
            gc.fillText(timeMin.format(timeFmt), padLeft, h - 6);
            if (currentBids.size() > 1)
                gc.fillText(timeMax.format(timeFmt), padLeft + plotW - 30, h - 6);

            double lastX = xs[xs.length - 1], lastY = ys[xs.length - 1];
            String topPrice = formatPriceShort(currentBids.get(currentBids.size() - 1).getAmount().doubleValue());
            gc.setFill(Color.web("#1e1a0a"));
            double labelW = 80, labelH2 = 22;
            double labelX = Math.min(lastX - labelW / 2, w - labelW - 4);
            double labelY = lastY - labelH2 - 8;
            gc.fillRoundRect(labelX, labelY, labelW, labelH2, 6, 6);
            gc.setFill(Color.web("#f2d67e"));
            gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 10));
            gc.fillText("▲ " + topPrice, labelX + 6, labelY + 15);

            if (lblChartEmpty != null) {
                lblChartEmpty.setVisible(false);
                lblChartEmpty.setManaged(false);
            }
        });
    }

    private void drawEmptyChart() {
        if (chartCanvas == null) return;
        Platform.runLater(() -> {
            double w = chartCanvas.getWidth();
            double h = chartCanvas.getHeight();
            if (w <= 0 || h <= 0) return;
            GraphicsContext gc = chartCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, w, h);
            gc.setFill(Color.web("#0a0a14"));
            gc.fillRoundRect(0, 0, w, h, 12, 12);
            gc.setStroke(Color.web("#2a2a40"));
            gc.setLineWidth(1.5);
            gc.setLineDashes(8, 6);
            gc.strokeLine(40, h / 2, w - 20, h / 2);
            gc.setLineDashes(0);
            if (lblChartEmpty != null) {
                lblChartEmpty.setVisible(true);
                lblChartEmpty.setManaged(true);
            }
        });
    }

    // ── Formatting ───────────────────────────────────────────────

    private String formatPrice(double price) {
        if (price >= 1_000_000_000) return String.format("%.2f tỷ đ", price / 1_000_000_000);
        if (price >= 1_000_000)     return String.format("%.1f triệu đ", price / 1_000_000);
        return String.format("%,.0f đ", price).replace(',', '.');
    }

    private String formatPriceShort(double price) {
        if (price >= 1_000_000_000) return String.format("%.1fB", price / 1_000_000_000);
        if (price >= 1_000_000)     return String.format("%.1fM", price / 1_000_000);
        if (price >= 1_000)         return String.format("%.0fK", price / 1_000);
        return String.format("%.0f", price);
    }

    // ── FXML handlers ────────────────────────────────────────────

    @FXML
    private void handleBack() {
        if (timerScheduler != null && !timerScheduler.isShutdown())
            timerScheduler.shutdownNow();
        if (thisStage != null) thisStage.close();
    }

    @FXML
    private void handlePlaceBid() {
        if (onPlaceBid != null) onPlaceBid.run();
    }
}