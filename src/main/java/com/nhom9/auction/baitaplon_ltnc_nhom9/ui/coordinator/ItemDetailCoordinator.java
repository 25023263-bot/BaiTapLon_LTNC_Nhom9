package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator;

import com.nhom9.auction.baitaplon_ltnc_nhom9.HelloApplication;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.BidDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.ItemDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller.BidDialogController;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller.BidRequest;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller.ItemDetailController;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model.AuctionCardModel;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.network.ServerConnection;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

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

/**
 * Coordinator điều phối 2 màn hình:
 *   1. ItemDetailView  — xem chi tiết sản phẩm đấu giá
 *   2. BidDialogView   — popup đặt giá thầu
 *
 * <h3>Thay đổi so với phiên bản cũ:</h3>
 * <ul>
 *   <li>Bỏ dependency vào {@code ServiceLocator}, {@code AuctionRepository},
 *       {@code BidRepository}, và {@code AuctionHouse}.</li>
 *   <li>Mọi thao tác dữ liệu (detail, bid history, place bid) đi qua
 *       {@link ServerConnection}.</li>
 *   <li>Polling vẫn giữ nhưng dùng {@code GET_AUCTION_DETAIL} qua socket
 *       thay vì query DB trực tiếp.</li>
 *   <li>Kiểm tra phiên hết hạn trước khi mở dialog cũng qua socket.</li>
 * </ul>
 */
public final class ItemDetailCoordinator {

    private static final Logger LOG =
            Logger.getLogger(ItemDetailCoordinator.class.getName());

    private final Window owner;

    public ItemDetailCoordinator(Window owner) {
        this.owner = Objects.requireNonNull(owner, "owner không được null");
    }

    // ── 1. Mở màn hình chi tiết ─────────────────────────────────

    public void openForAuction(AuctionCardModel item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/ItemDetailView.fxml"));
            Parent root = loader.load();

            ItemDetailController ctrl = loader.getController();

            Stage stage = new Stage();
            ctrl.configure(stage, item, () -> openBidDialog(item, stage, ctrl));

            // Load chi tiết đầy đủ qua socket ngay khi mở màn hình
            loadDetailAndRefresh(item, ctrl);

            // ── Polling: kiểm tra bid mới mỗi 5 giây qua socket ────────────
            //
            // Dùng socket thay vì truy vấn DB trực tiếp.
            // Cơ chế vẫn giống cũ: so sánh totalBids, nếu tăng thì reload.
            int auctionId = Integer.parseInt(item.id());
            AtomicInteger lastKnownCount = new AtomicInteger(item.bidCount());

            ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ubid-bid-poller");
                t.setDaemon(true);
                return t;
            });
            poller.scheduleAtFixedRate(() -> {
                if (!ServerConnection.isConnected()) return;
                try {
                    ItemDTO fresh = ServerConnection.getAuctionDetail(auctionId);
                    int currentCount = fresh.getTotalBids();
                    if (currentCount != lastKnownCount.get()) {
                        lastKnownCount.set(currentCount);
                        double newPrice = fresh.getCurrentPrice() != null
                                ? fresh.getCurrentPrice().doubleValue()
                                : item.currentBid();
                        Platform.runLater(() ->
                                ctrl.refreshAfterBid(newPrice, currentCount, toBids(fresh.getBids())));
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
            stage.setHeight(700);
            stage.setScene(scene);
            stage.setOnHidden(e -> poller.shutdownNow());
            stage.showAndWait();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Không thể tải ItemDetailView.fxml", e);
            AlertHelper.showError("Lỗi hệ thống",
                    "Không thể mở màn hình chi tiết sản phẩm.");
        }
    }

    /**
     * Load chi tiết phiên đấu giá từ server (background thread)
     * và refresh UI khi xong.
     */
    private void loadDetailAndRefresh(AuctionCardModel item, ItemDetailController ctrl) {
        if (!ServerConnection.isConnected()) return;
        int auctionId = Integer.parseInt(item.id());
        Thread t = new Thread(() -> {
            try {
                ItemDTO dto = ServerConnection.getAuctionDetail(auctionId);
                double price = dto.getCurrentPrice() != null
                        ? dto.getCurrentPrice().doubleValue() : item.currentBid();
                Platform.runLater(() -> {
                    ctrl.refreshAfterBid(price, dto.getTotalBids(), toBids(dto.getBids()));
                    if (dto.getEndTime() != null) {
                        ctrl.refreshEndTime(dto.getEndTime());
                    }
                });
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Không thể load chi tiết phiên đấu giá", e);
            }
        }, "ubid-detail-loader");
        t.setDaemon(true);
        t.start();
    }

    // ── 2. Mở popup đặt giá ──────────────────────────────────────

    private void openBidDialog(AuctionCardModel item, Stage detailStage,
                               ItemDetailController detailCtrl) {

        // Kiểm tra phiên còn hạn qua socket trước khi mở dialog
        if (ServerConnection.isConnected()) {
            try {
                ItemDTO fresh = ServerConnection.getAuctionDetail(Integer.parseInt(item.id()));
                boolean sessionExpired = fresh.getStatus() != AuctionStatus.ACTIVE
                        || (fresh.getEndTime() != null
                        && fresh.getEndTime().isBefore(java.time.LocalDateTime.now()));
                if (sessionExpired) {
                    AlertHelper.showError("Phiên đã kết thúc",
                            "Phiên đấu giá này đã kết thúc.\nKhông thể đặt giá.");
                    return;
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING,
                        "Không thể kiểm tra trạng thái phiên trước khi mở dialog", e);
                // Nếu không kiểm tra được → vẫn cho mở, server sẽ reject nếu sai
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/BidDialogview.fxml"));
            Parent root = loader.load();

            BidDialogController ctrl = loader.getController();

            // Lấy giá hiện tại từ server (tránh dùng giá cache có thể đã cũ)
            double freshCurrentBid = item.currentBid();
            if (ServerConnection.isConnected()) {
                try {
                    ItemDTO fresh = ServerConnection.getAuctionDetail(
                            Integer.parseInt(item.id()));
                    if (fresh.getCurrentPrice() != null) {
                        freshCurrentBid = fresh.getCurrentPrice().doubleValue();
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING,
                            "Không thể đọc giá hiện tại từ server, dùng giá cache", e);
                }
            }
            final double currentBidForDialog = freshCurrentBid;

            Stage dialog = new Stage();
            ctrl.configure(
                    currentBidForDialog,
                    dialog,
                    bidRequest -> onBidConfirmed(item, bidRequest, detailCtrl)
            );

            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.initOwner(detailStage);
            dialog.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            dialog.setScene(scene);

            dialog.setOnShown(e -> {
                dialog.setX(detailStage.getX()
                        + (detailStage.getWidth()  - dialog.getWidth())  / 2);
                dialog.setY(detailStage.getY()
                        + (detailStage.getHeight() - dialog.getHeight()) / 2);
            });

            dialog.showAndWait();

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Không thể tải BidDialogView.fxml", e);
            AlertHelper.showError("Lỗi hệ thống", "Không thể mở popup đặt giá.");
        }
    }

    // ── 3. Xử lý sau khi xác nhận ───────────────────────────────

    /**
     * Được gọi khi user bấm "Tiếp tục" trong BidDialog và giá hợp lệ.
     * Gửi request PLACE_BID hoặc PLACE_AUTO_BID qua socket (background thread).
     */
    private void onBidConfirmed(AuctionCardModel item, BidRequest request,
                                ItemDetailController detailCtrl) {
        NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
        int buyerId   = UserSession.getInstance().getCurrentUserId();
        int auctionId = Integer.parseInt(item.id());

        // Guard: seller không tự bid sản phẩm mình
        if (buyerId == item.sellerId()) {
            AlertHelper.showError("Không thể đặt giá",
                    "Người bán không được đặt giá cho sản phẩm của chính mình.");
            return;
        }

        // Chạy trên background thread — socket call không được block FX thread
        Thread t = new Thread(() -> {
            try {
                final Bid saved;
                if (request.isAuto()) {
                    BigDecimal maxLimit = BigDecimal.valueOf(request.amount());
                    saved = ServerConnection.placeAutoBid(auctionId, buyerId, maxLimit);

                    String firstBidFormatted = nf.format(saved.getAmount().longValue()) + " đ";
                    String maxLimitFormatted = nf.format(maxLimit.longValue()) + " đ";

                    Platform.runLater(() ->
                            AlertHelper.showInfo("Đặt giá tự động thành công!",
                                    "Sản phẩm      : " + item.title() + "\n"
                                            + "Giá đặt ngay  : " + firstBidFormatted + "\n"
                                            + "Giới hạn tối đa: " + maxLimitFormatted + "\n\n"
                                            + "Hệ thống sẽ tự động tăng giá khi có người vượt qua,\n"
                                            + "cho đến khi đạt giới hạn tối đa của bạn."));
                } else {
                    BigDecimal amount = BigDecimal.valueOf(request.amount());
                    saved = ServerConnection.placeBid(auctionId, buyerId, amount);

                    String formatted = nf.format(saved.getAmount().longValue()) + " đ";
                    Platform.runLater(() ->
                            AlertHelper.showInfo("Đặt giá thành công!",
                                    "Sản phẩm : " + item.title() + "\n"
                                            + "Giá đặt  : " + formatted + "\n\n"
                                            + "Hệ thống sẽ thông báo kết quả khi phiên đấu giá kết thúc."));
                }

                // Refresh detail bằng cách load lại từ server
                ItemDTO fresh = ServerConnection.getAuctionDetail(auctionId);
                Platform.runLater(() -> {
                    double newPrice = fresh.getCurrentPrice() != null
                            ? fresh.getCurrentPrice().doubleValue()
                            : saved.getAmount().doubleValue();
                    detailCtrl.refreshAfterBid(newPrice, fresh.getTotalBids(), toBids(fresh.getBids()));
                    if (fresh.getEndTime() != null) {
                        detailCtrl.refreshEndTime(fresh.getEndTime());
                    }
                });

            } catch (IllegalArgumentException e) {
                Platform.runLater(() ->
                        AlertHelper.showError("Không thể đặt giá", e.getMessage()));
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Lỗi khi đặt giá", e);
                Platform.runLater(() ->
                        AlertHelper.showError("Lỗi đặt giá",
                                "Không thể lưu lượt đặt giá: " + e.getMessage()));
            }
        }, "ubid-place-bid-thread");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Chuyển List&lt;BidDTO&gt; từ server sang List&lt;Bid&gt; mà ItemDetailController cần.
     * Trả về list rỗng nếu bids null (server cũ chưa gửi field này).
     */
    private List<Bid> toBids(List<BidDTO> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(d -> {
            Bid b = new Bid(d.getAuctionId(), d.getBuyerId(), d.getAmount());
            b.setId(d.getId());
            b.setBuyerUsername(d.getBuyerUsername());
            b.setBidTime(d.getBidTime());
            b.setAutoBid(d.isAutoBid());
            return b;
        }).toList();
    }
}
