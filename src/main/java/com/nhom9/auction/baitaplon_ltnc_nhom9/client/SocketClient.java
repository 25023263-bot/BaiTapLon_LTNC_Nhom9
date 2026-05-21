package com.nhom9.auction.baitaplon_ltnc_nhom9.client;

import com.nhom9.auction.baitaplon_ltnc_nhom9.server.protocol.Request;
import com.nhom9.auction.baitaplon_ltnc_nhom9.server.protocol.Response;

import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Client-side socket wrapper.
 *
 * <h3>Vấn đề của thiết kế cũ:</h3>
 * Có 2 thread cùng đọc ObjectInputStream:
 * <ul>
 *   <li>NotificationListener thread: đọc NOTIFICATION</li>
 *   <li>sendRequest(): đọc OK/ERROR response</li>
 * </ul>
 * Kết quả: listener "cướp" mất response của request → sendRequest() không nhận được
 * → Exception → "Mất kết nối đến server".
 *
 * <h3>Thiết kế mới — Single Reader Pattern:</h3>
 * <pre>
 *   CHỈ MỘT thread đọc từ socket (readerThread).
 *   Nó phân loại response:
 *
 *     NOTIFICATION ──▶ notificationHandler (cập nhật UI realtime)
 *     OK / ERROR   ──▶ responseQueue ──▶ sendRequest() lấy ra
 * </pre>
 *
 * Cách này đảm bảo không có race condition vì chỉ 1 thread đọc stream.
 * {@link BlockingQueue} đóng vai trò "hộp thư" thread-safe giữa reader và caller.
 *
 * <h3>Cấu hình HOST/PORT:</h3>
 * Đọc từ file {@code server.properties} ở thư mục chạy chương trình.
 * Nếu không tìm thấy file → dùng mặc định localhost:9999.
 * <pre>
 *   server.host=0.tcp.ap.ngrok.io
 *   server.port=19447
 * </pre>
 */
public class SocketClient {

    private static final Logger LOG = Logger.getLogger(SocketClient.class.getName());

    // ── Đọc cấu hình từ server.properties ────────────────────────────────────
    private static final String HOST;
    private static final int    PORT;

    static {
        String host = "localhost";
        int    port = 9999;

        File configFile = new File("server.properties");
        if (configFile.exists()) {
            try (InputStream in = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(in);
                host = props.getProperty("server.host", "localhost");
                port = Integer.parseInt(props.getProperty("server.port", "9999"));
                LOG.info("Đọc cấu hình từ server.properties → " + host + ":" + port);
            } catch (Exception e) {
                LOG.warning("Không đọc được server.properties, dùng mặc định. Lỗi: " + e.getMessage());
            }
        } else {
            LOG.info("Không tìm thấy server.properties → dùng mặc định localhost:9999");
        }

        HOST = host;
        PORT = port;
    }
    // ─────────────────────────────────────────────────────────────────────────

    private static final int TIMEOUT_SECONDS = 30;

    // Singleton
    private static SocketClient instance;

    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;

    /**
     * "Hộp thư" chứa response từ server.
     * readerThread bỏ response vào đây.
     * sendRequest() lấy ra và trả về caller.
     *
     * BlockingQueue là thread-safe: put() và take() có thể gọi từ 2 thread khác nhau
     * mà không cần synchronized thêm.
     */
    private final BlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();

    private Thread readerThread;

    // UI callback cho realtime notification
    private Consumer<Response> notificationHandler;

    private SocketClient() {}

    public static synchronized SocketClient getInstance() {
        if (instance == null) instance = new SocketClient();
        return instance;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Kết nối đến server. Gọi một lần khi app khởi động (trong HelloApplication.init()).
     */
    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);

        // QUAN TRỌNG: tạo OutputStream TRƯỚC InputStream
        // Nếu đảo ngược → cả client và server block nhau mãi mãi (deadlock)
        out = new ObjectOutputStream(socket.getOutputStream());
        in  = new ObjectInputStream(socket.getInputStream());

        startReaderThread();
        LOG.info("Đã kết nối đến server " + HOST + ":" + PORT);
    }

    /**
     * Gửi request và chờ response (blocking, tối đa TIMEOUT_SECONDS giây).
     *
     * <b>Không gọi method này trên JavaFX Application Thread!</b>
     * Dùng background thread (như trong LoginController).
     *
     * Tại sao synchronized? Vì nhiều Controller có thể gọi sendRequest() đồng thời.
     * synchronized đảm bảo chỉ một request được gửi tại một thời điểm,
     * tránh các request bị trộn lẫn trên stream.
     *
     * @throws IOException nếu mất kết nối khi gửi
     * @throws InterruptedException nếu thread bị interrupt khi chờ
     */
    public synchronized Response sendRequest(Request request)
            throws IOException, InterruptedException {

        // Gửi request lên server
        out.writeObject(request);
        out.flush();
        out.reset(); // tránh ObjectOutputStream cache object cũ

        // Chờ readerThread bỏ response vào queue
        // poll() trả về null nếu chờ quá TIMEOUT_SECONDS giây
        Response response = responseQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (response == null) {
            throw new IOException("Server không phản hồi sau " + TIMEOUT_SECONDS + " giây.");
        }

        return response;
    }

    /**
     * Đăng ký callback nhận NOTIFICATION realtime từ server.
     * Gọi trong HomeController sau khi login thành công.
     */
    public void setNotificationHandler(Consumer<Response> handler) {
        this.notificationHandler = handler;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void disconnect() {
        try {
            if (readerThread != null) readerThread.interrupt();
            if (socket != null)       socket.close();
            LOG.info("Socket đã ngắt kết nối.");
        } catch (IOException e) {
            LOG.warning("Lỗi khi disconnect: " + e.getMessage());
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    /**
     * Khởi động thread đọc socket — đây là thread DUY NHẤT được đọc từ "in".
     *
     * Vòng lặp của thread:
     *   1. Đọc một Response từ stream (blocking — chờ đến khi có data)
     *   2. Nếu là NOTIFICATION → gọi handler trên JavaFX thread
     *   3. Nếu là OK/ERROR     → bỏ vào responseQueue để sendRequest() lấy
     */
    private void startReaderThread() {
        readerThread = new Thread(() -> {
            try {
                while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    Response response = (Response) in.readObject();

                    if (response.isNotification()) {
                        // Notification: dispatch về JavaFX thread để cập nhật UI
                        if (notificationHandler != null) {
                            Platform.runLater(() -> notificationHandler.accept(response));
                        }
                    } else {
                        // OK/ERROR: bỏ vào queue để sendRequest() lấy ra
                        responseQueue.put(response);
                    }
                }
            } catch (EOFException | java.net.SocketException e) {
                LOG.info("Server ngắt kết nối.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // restore flag
                LOG.info("Reader thread bị interrupt — đang shutdown.");
            } catch (Exception e) {
                if (!socket.isClosed()) {
                    LOG.warning("Reader thread lỗi: " + e.getMessage());
                }
            }
        });

        readerThread.setDaemon(true); // tự tắt khi app đóng
        readerThread.setName("socket-reader-thread");
        readerThread.start();
    }
}