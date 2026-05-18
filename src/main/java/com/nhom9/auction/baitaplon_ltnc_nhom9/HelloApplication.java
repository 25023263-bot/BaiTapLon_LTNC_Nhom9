package com.nhom9.auction.baitaplon_ltnc_nhom9;

import com.nhom9.auction.baitaplon_ltnc_nhom9.client.SocketClient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Entry point của JavaFX client.
 *
 * <h3>Thứ tự khởi động:</h3>
 * <ol>
 *   <li>Kết nối socket đến AuctionServer (port 9999)</li>
 *   <li>Nếu server chưa chạy → hỏi user có muốn thử lại / thoát</li>
 *   <li>Hiển thị màn hình chính (HomeView)</li>
 * </ol>
 *
 * <h3>Bước 8 — Xoá ServiceLocator khỏi client:</h3>
 * Client không còn kết nối DB trực tiếp.
 * Toàn bộ dữ liệu đi qua socket → AuctionServer.
 * ServiceLocator, AuctionScheduler chỉ tồn tại trên server.
 */
public class HelloApplication extends Application {

    private static final Logger LOG = Logger.getLogger(HelloApplication.class.getName());

    /**
     * JavaFX gọi init() trước start() — chạy trên JavaFX Launcher thread,
     * KHÔNG phải JavaFX Application Thread.
     * Kết nối socket ở đây để không block UI.
     */
    @Override
    public void init() {
        LOG.info("=== UBid khởi động ===");
        connectToServer();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("/fxml/HomeView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1120.0, 740.0);
        stage.setTitle("UBid");
        stage.setScene(scene);
        stage.show();
        LOG.info("Giao diện chính đã hiển thị.");
    }

    /**
     * JavaFX tự gọi stop() khi user đóng cửa sổ.
     * Chỉ cần ngắt kết nối socket — không còn DB để đóng.
     */
    @Override
    public void stop() {
        LOG.info("=== UBid đang tắt ===");
        SocketClient.getInstance().disconnect();
        LOG.info("Socket đã ngắt kết nối.");
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Thử kết nối đến AuctionServer.
     *
     * Nếu thất bại → dialog hỏi user:
     *   - "Thử lại"  : thử kết nối thêm 1 lần
     *   - "Thoát"    : đóng app
     *
     * Không còn tùy chọn "Tiếp tục offline" vì client đã bỏ hoàn toàn DB.
     */
    private void connectToServer() {
        try {
            SocketClient.getInstance().connect();
            LOG.info("✅ Đã kết nối đến server thành công.");
        } catch (IOException e) {
            LOG.warning("⚠️ Không thể kết nối server: " + e.getMessage());

            boolean retry = showConnectionErrorDialog();
            if (retry) {
                try {
                    SocketClient.getInstance().connect();
                    LOG.info("✅ Kết nối thành công sau lần thử lại.");
                } catch (IOException ex) {
                    LOG.warning("⚠️ Thử lại cũng thất bại. App sẽ không có dữ liệu.");
                }
            }
        }
    }

    /**
     * Hiện dialog cảnh báo kết nối thất bại.
     * Dùng Platform.runLater + wait-latch vì init() chạy trước khi Stage hiển thị.
     *
     * @return true nếu user chọn "Thử lại"
     */
    private boolean showConnectionErrorDialog() {
        final boolean[] retry = {false};
        final Object lock = new Object();

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("UBid — Không thể kết nối");
            alert.setHeaderText("Không thể kết nối đến Auction Server");
            alert.setContentText(
                    "Server chưa khởi động hoặc không thể kết nối tại localhost:9999.\n\n" +
                            "Hãy đảm bảo AuctionServer đang chạy rồi thử lại."
            );

            ButtonType btnRetry = new ButtonType("Thử lại");
            ButtonType btnExit  = new ButtonType("Thoát", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnRetry, btnExit);

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == btnRetry) {
                retry[0] = true;
            } else {
                Platform.exit();
            }

            synchronized (lock) { lock.notifyAll(); }
        });

        synchronized (lock) {
            try {
                lock.wait(30_000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        return retry[0];
    }

    public static void main(String[] args) {
        launch();
    }
}
