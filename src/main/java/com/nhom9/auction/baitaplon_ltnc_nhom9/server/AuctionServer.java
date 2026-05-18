package com.nhom9.auction.baitaplon_ltnc_nhom9.server;

import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.ServiceLocator;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Entry point của Server — lắng nghe kết nối từ client.
 *
 * Chạy riêng biệt với JavaFX client:
 *   java -cp <jar> ...server.AuctionServer
 *
 * Mỗi client kết nối → tạo 1 ClientHandler chạy trên thread riêng
 * → server xử lý nhiều client đồng thời.
 */
public class AuctionServer {

    public static final int PORT = 9999;
    private static final Logger LOG = Logger.getLogger(AuctionServer.class.getName());

    // Danh sách client đang kết nối — dùng để broadcast notification
    // CopyOnWriteArrayList: an toàn khi nhiều thread đọc/ghi đồng thời
    static final CopyOnWriteArrayList<ClientHandler> connectedClients
            = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws Exception {

        // 1. Khởi tạo toàn bộ service và DB
        ServiceLocator locator = ServiceLocator.getInstance();
        LOG.info("ServiceLocator khởi tạo xong.");

        // 2. Đăng ký SocketNotifier vào AuctionHouse
        //    → khi có bid mới, SocketNotifier broadcast đến tất cả client
        SocketNotifier notifier = new SocketNotifier(connectedClients);
        locator.getAuctionHouse().addObserver(notifier);
        LOG.info("SocketNotifier đã đăng ký vào AuctionHouse.");

        // 3. Mở ServerSocket và chờ client kết nối
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOG.info("=== Auction Server đang chạy tại cổng " + PORT + " ===");

            while (true) {
                // accept() chặn tại đây cho đến khi có client kết nối
                Socket clientSocket = serverSocket.accept();
                LOG.info("Client mới: " + clientSocket.getInetAddress());

                // Tạo handler và chạy trên thread riêng
                ClientHandler handler = new ClientHandler(clientSocket, locator);
                connectedClients.add(handler);

                Thread t = new Thread(handler);
                t.setDaemon(true); // thread tự tắt khi main thread tắt
                t.start();
            }
        }
    }
}