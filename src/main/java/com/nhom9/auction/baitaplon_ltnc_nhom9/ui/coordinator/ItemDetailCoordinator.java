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

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinator điều phối 2 màn hình:
 *   1. ItemDetailView  — xem chi tiết sản phẩm đấu giá
 *   2. BidDialogView   — popup đặt giá thầu
 */
public final class ItemDetailCoordinator {

    private static final Logger LOG =
            Logger.getLogger(ItemDetailCoordinator.class.getName());

    private final Window owner;

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
            // Callback: khi user bấm "Đặt giá thầu ngay" → mở BidDialog
            ctrl.configure(stage, item, () -> openBidDialog(item, stage));

            Scene scene = new Scene(root);
            stage.setTitle("Chi tiết đấu giá — " + item.title());
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setMinWidth(900);
            stage.setMinHeight(560);
            // Kích thước cố định — ScrollPane cuộn thay vì Stage tự mở rộng
            stage.setWidth(960);
            stage.setHeight(680);
            stage.setScene(scene);
            stage.showAndWait();

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Không thể tải ItemDetailView.fxml", e);
            AlertHelper.showError("Lỗi hệ thống",
                    "Không thể mở màn hình chi tiết sản phẩm.");
        }
    }

    // ── 2. Mở popup đặt giá ──────────────────────────────────────

    private void openBidDialog(AuctionItem item, Stage detailStage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/BidDialogView.fxml"));
            Parent root = loader.load();

            BidDialogController ctrl = loader.getController();

            Stage dialog = new Stage();
            ctrl.configure(
                    item.currentBid(),
                    dialog,
                    confirmedAmount -> onBidConfirmed(item, confirmedAmount)
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
    private void onBidConfirmed(AuctionItem item, double amount) {
        NumberFormat nf = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));
        String formatted = nf.format(Math.round(amount)) + " đ";

        AlertHelper.showInfo(
                "Đặt giá thành công!",
                "Sản phẩm : " + item.title() + "\n"
                        + "Giá đặt  : " + formatted + "\n\n"
                        + "Hệ thống sẽ thông báo kết quả khi phiên đấu giá kết thúc."
        );
    }
}