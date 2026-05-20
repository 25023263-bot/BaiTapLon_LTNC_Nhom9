package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.controller;

import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator.ItemDetailCoordinator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.coordinator.SellerItemDetailCoordinator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.UserSession;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model.AuctionCardModel;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.presenter.HomeCatalogPresenter;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/**
 * HomeBidHandler — xử lý logic khi user click vào một auction card.
 *
 * <p>Được tách ra từ HomeController để tập trung toàn bộ routing logic vào
 * một chỗ. Class này trả lời câu hỏi: "Khi user bấm vào một card, thì
 * cần mở màn hình nào?"</p>
 *
 * <h3>Ba trường hợp cần xử lý:</h3>
 * <ol>
 *   <li>User chưa đăng nhập → hiện dialog mời đăng nhập</li>
 *   <li>User là chủ sản phẩm → mở SellerItemDetail (xem/quản lý)</li>
 *   <li>User là buyer → mở ItemDetail (xem chi tiết và đặt giá)</li>
 * </ol>
 *
 * <p>Trường hợp 2 là "Defense in Depth" — UI đã ẩn nút "Đặt giá" với chủ
 * sản phẩm, nhưng tầng controller vẫn phải kiểm tra lại để tránh race
 * condition hoặc bug render.</p>
 */
public class HomeBidHandler {

    private final HomeCatalogPresenter          catalogPresenter;
    private final Runnable                      onLoginRequest;    // callback mở login window

    // Coordinators có thể null khi chưa có Stage (lazy init)
    private ItemDetailCoordinator       itemDetailCoordinator;
    private SellerItemDetailCoordinator sellerItemDetailCoordinator;

    // ─── Constructor ─────────────────────────────────────────────────────────

    /**
     * @param catalogPresenter  dùng để tìm item theo ID và refresh sau khi bid
     * @param onLoginRequest    callback được gọi khi user cần đăng nhập trước
     */
    public HomeBidHandler(HomeCatalogPresenter catalogPresenter,
                          Runnable onLoginRequest) {
        this.catalogPresenter = catalogPresenter;
        this.onLoginRequest   = onLoginRequest;
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Cập nhật coordinators sau khi Stage đã sẵn sàng.
     * HomeController gọi method này sau khi scene được attach.
     */
    public void setCoordinators(ItemDetailCoordinator itemDetailCoordinator,
                                SellerItemDetailCoordinator sellerItemDetailCoordinator) {
        this.itemDetailCoordinator       = itemDetailCoordinator;
        this.sellerItemDetailCoordinator = sellerItemDetailCoordinator;
    }

    /**
     * Điểm vào duy nhất khi user click vào một auction card.
     *
     * @param auctionId ID của phiên đấu giá (dạng String để khớp với AuctionCardModel)
     */
    public void handle(String auctionId) {
        // Bước 1: kiểm tra đăng nhập
        if (!UserSession.getInstance().isLoggedIn()) {
            showLoginRequiredDialog();
            return;
        }

        // Bước 2: tìm item trong catalog
        AuctionCardModel item = catalogPresenter.findById(auctionId);
        if (item == null) {
            showItemNotFoundAlert(auctionId);
            return;
        }

        // Bước 3: routing theo quyền sở hữu
        //
        // Tại sao kiểm tra lại ở đây dù UI đã ẩn nút?
        // → Defense in Depth: UI là lớp cuối cùng user thấy, nhưng không phải
        //   lớp cuối cùng code chạy. Nếu catalog refresh muộn hơn một chút
        //   (race condition), có thể sellerId chưa cập nhật đúng → user bấm nhầm.
        //   Kiểm tra ở controller đảm bảo routing luôn đúng bất kể UI.
        int currentUserId = UserSession.getInstance().getCurrentUserId();
        if (currentUserId == item.sellerId()) {
            openSellerView(item);
        } else {
            openBuyerView(item);
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void openSellerView(AuctionCardModel item) {
        if (sellerItemDetailCoordinator == null) return;
        // Sau khi seller thay đổi item → refresh catalog để giá/status đúng
        sellerItemDetailCoordinator.open(item, catalogPresenter::refreshAll);
    }

    private void openBuyerView(AuctionCardModel item) {
        if (itemDetailCoordinator == null) return;
        itemDetailCoordinator.openForAuction(item);
        // Refresh ngay sau khi mở để cập nhật giá mới nhất
        catalogPresenter.refreshAll();
    }

    /**
     * Dialog mời đăng nhập với 2 lựa chọn: Đăng nhập hoặc Đóng.
     * Nếu user chọn "Đăng nhập" → gọi callback onLoginRequest.
     */
    private void showLoginRequiredDialog() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("UBid");
        a.setHeaderText(null);
        a.setContentText("Vui lòng đăng nhập để tiếp tục");

        ButtonType loginBtn = new ButtonType("Đăng nhập", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeBtn = new ButtonType("Đóng",      ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(loginBtn, closeBtn);

        a.showAndWait()
                .filter(response -> response == loginBtn)
                .ifPresent(r -> {
                    if (onLoginRequest != null) onLoginRequest.run();
                });
    }

    private void showItemNotFoundAlert(String auctionId) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("UBid");
        a.setHeaderText("Không tìm thấy phiên đấu giá");
        a.setContentText("ID: " + auctionId + " — phiên có thể đã kết thúc hoặc bị xóa.");
        a.show();
    }
}
