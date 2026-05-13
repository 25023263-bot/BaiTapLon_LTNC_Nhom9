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
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.ServiceLocator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
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
     * TODO: thay bằng BidService.placeBid() khi có backend.
     */
    private void onBidConfirmed(AuctionItem item, double amount, ItemDetailController detailCtrl) {
        NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
        String formatted = nf.format(Math.round(amount)) + " đ";

        int buyerId = UserSession.getInstance().getCurrentUserId();
        int auctionId = Integer.parseInt(item.id());

        // ── Lớp bảo vệ thứ 2: kiểm tra lại trước khi ghi vào DB ────────────
        //
        // Tại sao cần kiểm tra ở đây dù HomeController đã kiểm tra rồi?
        // → Coordinator là "cửa ngõ" cuối trước khi dữ liệu vào DB.
        //   Nếu sau này có thêm đường khác mở BidDialog (ví dụ từ màn search,
        //   notification, deep-link...), kiểm tra ở đây đảm bảo rule LUÔN được
        //   áp dụng, bất kể user đến từ đâu. Đây là nguyên tắc "Fail-Safe":
        //   nếu mọi lớp trên đều bỏ sót, lớp này vẫn chặn được.
        if (buyerId == item.sellerId()) {
            AlertHelper.showError(
                    "Không thể đặt giá",
                    "Người bán không được đặt giá cho sản phẩm của chính mình."
            );
            return;
        }

        try {
            // ── Kiểm tra lại giá cao nhất tại thời điểm submit ──────────────
            // Trường hợp: 2 user cùng mở dialog, user A đặt xong trước.
            // User B đã validate trên UI nhưng giá freshCurrentBid có thể
            // đã lỗi thời khi submit → cần kiểm tra lại lần nữa ở đây.
            List<Bid> latestBids = bidRepo.findByAuctionId(auctionId);
            if (!latestBids.isEmpty()) {
                double actualHighest = latestBids.get(0).getAmount().doubleValue();
                if (amount <= actualHighest) {
                    AlertHelper.showError(
                            "Giá đặt quá thấp",
                            "Giá cao nhất hiện tại là " + nf.format(Math.round(actualHighest)) + " đ.\n"
                                    + "Vui lòng đặt giá cao hơn mức này."
                    );
                    return;
                }
            }

            // Lưu lượt bid vào bảng bids
            Bid bid = new Bid();
            bid.setAuctionId(auctionId);
            bid.setBuyerId(buyerId);
            bid.setAmount(BigDecimal.valueOf(amount));
            bid.setBidTime(LocalDateTime.now());
            bid.setAutoBid(false);
            bidRepo.save(bid);

            // Cập nhật giá hiện tại và người dẫn đầu trong bảng auctions
            auctionRepo.updateCurrentBid(auctionId, BigDecimal.valueOf(amount), buyerId);

            // Refresh giá + lịch sử trên màn chi tiết
            detailCtrl.refreshAfterBid(amount, bidRepo.countByAuctionId(auctionId),
                    bidRepo.findByAuctionId(auctionId));

            // ── Anti-snipe: đọc lại end_time từ DB sau khi AuctionHouse có thể đã gia hạn ──
            //
            // Tại sao đọc lại thay vì tự tính?
            //   AuctionHouse.extendIfLastMinute() chạy trong service layer và ghi
            //   end_time mới vào DB. Coordinator không biết liệu extension có xảy ra
            //   hay không — cách duy nhất chính xác là đọc lại từ DB.
            //   Nếu không có extension, end_time giống cũ → refreshEndTime() chỉ reset
            //   timer về đúng giá trị (vô hại). Nếu có extension → timer cập nhật đúng.
            try {
                auctionRepo.findById(auctionId).ifPresent(freshItem -> {
                    LocalDateTime freshEndTime = freshItem.getEndTime();
                    Platform.runLater(() -> detailCtrl.refreshEndTime(freshEndTime));
                });
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Không thể đọc endTime mới từ DB sau bid", ex);
                // Không cần báo lỗi cho user — timer cũ vẫn chạy bình thường
            }

            AlertHelper.showInfo(
                    "Đặt giá thành công!",
                    "Sản phẩm : " + item.title() + "\n"
                            + "Giá đặt  : " + formatted + "\n\n"
                            + "Hệ thống sẽ thông báo kết quả khi phiên đấu giá kết thúc."
            );
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Lỗi khi lưu bid", e);
            AlertHelper.showError("Lỗi đặt giá", "Không thể lưu lượt đặt giá: " + e.getMessage());
        }
    }
}