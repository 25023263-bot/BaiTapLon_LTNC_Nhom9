package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator;

import com.nhom9.auction.baitaplon_ltnc_nhom9.HelloApplication;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model.AuctionCardModel;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller.SellerItemDetailController;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.AlertHelper;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinator điều phối màn hình quản lý sản phẩm dành cho Người bán.
 *
 * Tại sao cần Coordinator riêng thay vì mở trực tiếp từ HomeController?
 * → HomeController đã đủ phức tạp (quản lý nhiều overlay, timer, nav...).
 *   Tách việc mở cửa sổ mới vào Coordinator giúp HomeController "thin" hơn
 *   và tuân thủ nguyên tắc Single Responsibility Principle (SRP):
 *     - HomeController: quản lý UI trang chủ
 *     - SellerItemDetailCoordinator: điều phối luồng mở/đóng màn seller detail
 *
 * Pattern này giống với ItemDetailCoordinator đã có trong project.
 */
public final class SellerItemDetailCoordinator {

    private static final Logger LOG =
            Logger.getLogger(SellerItemDetailCoordinator.class.getName());

    private final Window owner;

    public SellerItemDetailCoordinator(Window owner) {
        this.owner = Objects.requireNonNull(owner, "owner không được null");
    }

    /**
     * Mở cửa sổ chi tiết/quản lý sản phẩm cho Seller.
     *
     * Dùng showAndWait() để cửa sổ này là modal (chặn tương tác với Home
     * trong khi đang xem/sửa sản phẩm).
     *
     * @param item          Sản phẩm cần xem/quản lý
     * @param onDataChanged Callback được gọi sau khi Seller save hoặc xóa.
     *                      HomeController dùng callback này để reload danh sách.
     */
    public void open(AuctionCardModel item, Runnable onDataChanged) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/fxml/SellerItemDetailView.fxml"));
            Parent root = loader.load();

            SellerItemDetailController ctrl = loader.getController();

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Quản lý sản phẩm — " + item.title());
            stage.setMinWidth(900);
            stage.setMinHeight(580);
            stage.setWidth(980);
            stage.setHeight(700);

            // Truyền stage + item + callback vào controller
            ctrl.configure(stage, item, onDataChanged);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.showAndWait();   // Block đến khi cửa sổ đóng

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Không thể tải SellerItemDetailView.fxml", e);
            AlertHelper.showError("Lỗi hệ thống",
                    "Không thể mở màn hình quản lý sản phẩm.");
        }
    }
}
