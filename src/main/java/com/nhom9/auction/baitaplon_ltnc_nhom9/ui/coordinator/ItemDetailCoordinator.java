package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator;

import com.nhom9.auction.baitaplon_ltnc_nhom9.HelloApplication;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller.BidDialogController;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller.HomeController.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller.ItemDetailController;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.AuctionRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.BidRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.AuctionHouse;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.ServiceLocator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

/**
 * Coordinator điều phối 2 màn hình:
 *   1. ItemDetailView  — xem chi tiết sản phẩm đấu giá
 *   2. BidDialogView   — popup đặt giá thầu
 *
 * Cơ chế "realtime" cập nhật chart:
 *   Vì chưa có WebSocket backend, ta dùng polling — gọi DB mỗi 5 giây
 *   để kiểm tra bid mới từ người khác. Khi có bid mới → gọi
 *   detailCtrl.refreshAfterBid() để cập nhật chart + danh sách + giá.
 *
 *   Sau này khi có Spring Boot + WebSocket:
 *   - Xóa ScheduledExecutorService này
 *   - Subscribe vào topic "/topic/auction/{id}/bids"
 *   - Nhận event → gọi refreshAfterBid() tương tự
 *   → Interface của controller KHÔNG thay đổi, chỉ thay cách trigger.
 */
public final class ItemDetailCoordinator {

    private static final Logger LOG =
            Logger.getLogger(ItemDetailCoordinator.class.getName());

    private final Window owner;
    private final AuctionRepository auctionRepo = ServiceLocator.getInstance().getAuctionRepo();
    private final BidRepository     bidRepo     = ServiceLocator.getInstance().getBidRepo();
    private final AuctionHouse      auctionHouse = ServiceLocator.getInstance().getAuctionHouse();

    public ItemDetailCoordinator(Window owner) {
        this.owner = Objects.requireNonNull(owner, "owner không được null");
    }

    // ── 1. Mở màn hình chi tiết ─────────────────────────────────

    public void openForAuction(AuctionItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/ItemDetailView.fxml"));
            Parent root = loader.load();

            ItemDetailController ctrl = loader.getController();

            Stage stage = new Stage();
            ctrl.configure(stage, item, () -> openBidDialog(item, stage, ctrl));

            // Load lịch sử bid thật ngay khi mở màn chi tiết
            int auctionId = Integer.parseInt(item.id());
            try {
                ctrl.refreshAfterBid(
                        item.currentBid(),
                        bidRepo.countByAuctionId(auctionId),
                        bidRepo.findByAuctionId(auctionId)
                );
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Không thể load lịch sử bid", e);
            }

            // ── Polling: tự động kiểm tra bid mới mỗi 5 giây ────────────────
            //
            // Cách hoạt động:
            //   1. Lưu bid count hiện tại vào lastKnownCount (atomic để thread-safe)
            //   2. Mỗi 5 giây, gọi DB kiểm tra count mới
            //   3. Nếu count tăng → có bid mới → load danh sách đầy đủ và refresh UI
            //   4. Khi cửa sổ đóng → shutdownNow() dừng thread polling
            //
            // Tại sao dùng AtomicInteger thay vì int thông thường?
            // → Thread polling chạy trong background thread, không phải JavaFX thread.
            //   AtomicInteger đảm bảo read/write không bị race condition giữa
            //   background thread và JavaFX thread.
            //
            // Tại sao 5 giây chứ không phải 1 giây?
            // → 1 giây gọi DB liên tục 60 lần/phút — quá tải cho DB local.
            //   5 giây là compromise: đủ "realtime" cho UX, không overload DB.
            //   Khi có WebSocket thật, polling này sẽ được xóa hoàn toàn.
            AtomicInteger lastKnownCount = new AtomicInteger(
                    bidRepo.countByAuctionId(auctionId));
            ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ubid-bid-poller");
                t.setDaemon(true); // daemon: tự tắt khi app đóng, không block JVM
                return t;
            });
            poller.scheduleAtFixedRate(() -> {
                try {
                    int currentCount = bidRepo.countByAuctionId(auctionId);
                    if (currentCount != lastKnownCount.get()) {
                        // Có bid mới → cập nhật count và reload UI trên JavaFX thread
                        lastKnownCount.set(currentCount);
                        List<com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid> freshBids
                                = bidRepo.findByAuctionId(auctionId);
                        double newPrice = freshBids.isEmpty() ? item.currentBid()
                                : freshBids.get(0).getAmount().doubleValue();
                        // Platform.runLater: bắt buộc khi cập nhật UI từ background thread
                        // JavaFX là single-threaded — chỉ được sửa UI từ JavaFX Application Thread
                        Platform.runLater(() ->
                                ctrl.refreshAfterBid(newPrice, currentCount, freshBids));
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Polling lỗi: " + e.getMessage());
                }
            }, 5, 5, TimeUnit.SECONDS);

            Scene scene = new Scene(root);
            stage.setTitle("Chi tiết đấu giá — " + item.title());
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setMinWidth(900);
            stage.setMinHeight(560);
            stage.setWidth(960);
            stage.setHeight(700); // tăng thêm 20px cho chart section
            stage.setScene(scene);

            // Khi cửa sổ đóng → dừng polling ngay để giải phóng thread
            stage.setOnHidden(e -> poller.shutdownNow());

            stage.showAndWait();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Không thể tải ItemDetailView.fxml", e);
            AlertHelper.showError("Lỗi hệ thống",
                    "Không thể mở màn hình chi tiết sản phẩm.");
        }
    }

    // ── 2. Mở popup đặt giá ──────────────────────────────────────

    private void openBidDialog(AuctionItem item, Stage detailStage, ItemDetailController detailCtrl) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/BidDialogView.fxml"));
            Parent root = loader.load();

            BidDialogController ctrl = loader.getController();

            // ── Đọc giá cao nhất thật từ DB ngay trước khi mở dialog ────────
            // Tại sao cần đọc lại? item là record bất biến được tạo từ lúc mở
            // màn chi tiết. Nếu có bid mới (từ polling hoặc user khác) sau đó,
            // item.currentBid() vẫn là giá cũ → user có thể đặt giá thấp hơn
            // giá cao nhất thật → dữ liệu sai + biểu đồ đi xuống.
            int auctionId = Integer.parseInt(item.id());
            double freshCurrentBid = item.currentBid(); // fallback nếu DB lỗi
            try {
                List<Bid> latestBids = bidRepo.findByAuctionId(auctionId);
                if (!latestBids.isEmpty()) {
                    // findByAuctionId trả về theo amount DESC → phần tử đầu là cao nhất
                    freshCurrentBid = latestBids.get(0).getAmount().doubleValue();
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Không thể đọc giá hiện tại từ DB, dùng giá cache", e);
            }
            final double currentBidForDialog = freshCurrentBid;

            Stage dialog = new Stage();
            ctrl.configure(
                    currentBidForDialog,
                    dialog,
                    confirmedAmount -> onBidConfirmed(item, confirmedAmount, detailCtrl)
            );

            // UNDECORATED: không có thanh tiêu đề hệ thống
            // bo góc VBox trong FXML hiện ra đẹp hơn
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.initOwner(detailStage);
            dialog.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(root);
            // Nền trong suốt để border-radius của VBox hiện ra
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            dialog.setScene(scene);

            // Căn giữa so với cửa sổ ItemDetail sau khi dialog hiện ra
            dialog.setOnShown(e -> {
                dialog.setX(detailStage.getX()
                        + (detailStage.getWidth()  - dialog.getWidth())  / 2);
                dialog.setY(detailStage.getY()
                        + (detailStage.getHeight() - dialog.getHeight()) / 2);
            });

            dialog.showAndWait();

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Không thể tải BidDialogView.fxml", e);
            AlertHelper.showError("Lỗi hệ thống",
                    "Không thể mở popup đặt giá.");
        }
    }

    // ── 3. Xử lý sau khi xác nhận ───────────────────────────────

    /**
     * Được gọi khi user bấm "Tiếp tục" trong BidDialog và giá hợp lệ.
     *
     * Trước đây: ghi bid thẳng vào DB qua bidRepo + auctionRepo → bỏ qua AuctionHouse
     * → NotificationService.onNewBid() không bao giờ được gọi → không có thông báo.
     *
     * Fix: gọi auctionHouse.placeBid() — nó đã xử lý đầy đủ:
     *   1. Validate giá (race condition check)
     *   2. Lưu bid vào DB
     *   3. updateCurrentBid trong bảng auctions
     *   4. extendIfLastMinute (anti-snipe)
     *   5. notifyNewBid → NotificationService.onNewBid() → seller + previous bidders
     */
    private void onBidConfirmed(AuctionItem item, double amount, ItemDetailController detailCtrl) {
        NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
        String formatted = nf.format(Math.round(amount)) + " đ";

        int buyerId   = UserSession.getInstance().getCurrentUserId();
        int auctionId = Integer.parseInt(item.id());

        // Guard: seller không tự bid sản phẩm mình
        if (buyerId == item.sellerId()) {
            AlertHelper.showError("Không thể đặt giá",
                    "Người bán không được đặt giá cho sản phẩm của chính mình.");
            return;
        }

        try {
            // Một lần gọi duy nhất — AuctionHouse lo toàn bộ:
            // validate → save bid → update price → anti-snipe → notify
            Bid saved = auctionHouse.placeBid(auctionId, buyerId, BigDecimal.valueOf(amount));

            // Refresh UI sau khi bid thành công
            detailCtrl.refreshAfterBid(
                    saved.getAmount().doubleValue(),
                    bidRepo.countByAuctionId(auctionId),
                    bidRepo.findByAuctionId(auctionId));

            // Anti-snipe: đọc lại endTime mới từ DB (nếu đã được gia hạn)
            try {
                auctionRepo.findById(auctionId).ifPresent(freshItem ->
                        Platform.runLater(() ->
                                detailCtrl.refreshEndTime(freshItem.getEndTime())));
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Không thể đọc endTime mới sau bid", ex);
            }

            AlertHelper.showInfo("Đặt giá thành công!",
                    "Sản phẩm : " + item.title() + "\n"
                            + "Giá đặt  : " + formatted + "\n\n"
                            + "Hệ thống sẽ thông báo kết quả khi phiên đấu giá kết thúc.");

        } catch (IllegalArgumentException e) {
            // AuctionHouse ném khi giá không hợp lệ hoặc phiên không ACTIVE
            AlertHelper.showError("Không thể đặt giá", e.getMessage());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Lỗi khi đặt giá", e);
            AlertHelper.showError("Lỗi đặt giá", "Không thể lưu lượt đặt giá: " + e.getMessage());
        }
    }
}