package com.nhom9.auction;

import com.nhom9.auction.baitaplon_ltnc_nhom9.config.AppConfig;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.common.FilterCriteria;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.common.Page;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DatabaseConnection;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.ServiceLocator;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers.DateTimeUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * AuctionApp – Dev Sandbox / Backend Test Screen
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * ĐÂY KHÔNG PHẢI UI THẬT CỦA APP.
 *
 * File này là một màn hình riêng dành cho developer, dùng để:
 *  - Kiểm tra backend (DB, services, repositories) còn hoạt động không
 *  - Test nhanh các tính năng (đăng ký, login, đặt bid, ...) mà không cần
 *    bấm qua toàn bộ UI FXML
 *  - Xem log trực tiếp trên giao diện
 *  - Nạp seed data / xoá data trong quá trình phát triển
 *
 * Entry point thật của app là HelloApplication (load FXML HomeView).
 * Entry point của màn hình test này là AuctionApp.main() bên dưới.
 *
 * Để chạy màn hình này thay vì UI thật:
 *  → Trong pom.xml, đổi <mainClass> thành:
 *    com.nhom9.auction.AuctionApp
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class AuctionApp extends Application {

    private static final Logger LOG = Logger.getLogger(AuctionApp.class.getName());

    private ServiceLocator sl;
    private TextArea       logArea;

    // ─── JavaFX Lifecycle ────────────────────────────────────────────────────

    /**
     * init() chạy TRƯỚC start(), trên một background thread (không phải FX thread).
     * Đây là nơi đúng để khởi tạo những thứ nặng như DB connection, services.
     *
     * Nếu đặt code này trong start(), DB sẽ khởi tạo TRÊN FX thread
     * → UI bị đóng băng trong vài giây → trải nghiệm xấu.
     */
    @Override
    public void init() {
        LOG.info("Khởi tạo ServiceLocator...");
        sl = ServiceLocator.getInstance();
        LOG.info("ServiceLocator sẵn sàng.");
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle(AppConfig.APP_TITLE + "  v" + AppConfig.APP_VERSION + "  [Dev Mode]");
        stage.setWidth(900);
        stage.setHeight(680);
        stage.setMinWidth(700);
        stage.setMinHeight(500);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a2e;");

        root.setTop(buildHeader());
        root.setCenter(buildTabs());
        root.setBottom(buildFooter());

        stage.setScene(new Scene(root));
        stage.show();

        startScheduler();
        runSmokeTest();
    }

    /**
     * stop() được JavaFX gọi khi cửa sổ đóng.
     * Đây là nơi đúng để dọn dẹp: đóng DB pool, dừng scheduler.
     *
     * Nếu không override stop(), DB pool sẽ không được đóng sạch
     * → có thể mất data hoặc lock file SQLite.
     */
    @Override
    public void stop() {
        log("App đang tắt – dọn dẹp tài nguyên...");
        sl.shutdown();
        DatabaseConnection.getInstance().close();
        LOG.info("App đã tắt.");
    }

    // ─── UI Building ─────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setStyle("-fx-background-color: #16213e;");
        header.setAlignment(Pos.CENTER_LEFT);

        Text title = new Text("🏛  AUCTION HOUSE");
        title.setFill(Color.web("#e94560"));
        title.setFont(Font.font("System", FontWeight.BOLD, 22));

        Text version = new Text("  v" + AppConfig.APP_VERSION + " – Dev Sandbox");
        version.setFill(Color.web("#a0a0c0"));
        version.setFont(Font.font("System", 13));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Hiển thị đang dùng DB nào
        String dbLabel = AppConfig.USE_MYSQL ? "MySQL" : "SQLite";
        Label statusDot = new Label("● " + dbLabel);
        statusDot.setStyle("-fx-text-fill: #00ff88; -fx-font-size: 12px; -fx-font-weight: bold;");

        header.getChildren().addAll(title, version, spacer, statusDot);
        return header;
    }

    private TabPane buildTabs() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: #1a1a2e;");

        tabs.getTabs().addAll(
                buildSystemTab(),
                buildItemsTab(),
                buildUsersTab(),
                buildBidTab(),
                buildLogTab()
        );
        return tabs;
    }

    // ── Tab 1: Thông tin hệ thống ─────────────────────────────────────────────

    private Tab buildSystemTab() {
        Tab tab = new Tab("  ⚙  Hệ thống  ");

        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #1a1a2e;");

        box.getChildren().add(sectionLabel("TRẠNG THÁI KHỞI ĐỘNG"));

        // Hiển thị thông tin DB đang dùng (SQLite hay MySQL)
        String dbInfo = AppConfig.USE_MYSQL
                ? "✅  MySQL – " + AppConfig.MYSQL_HOST + "/" + AppConfig.MYSQL_DATABASE
                : "✅  SQLite – " + AppConfig.SQLITE_PATH;

        String[][] items = {
                {"Database",            dbInfo},
                {"Schema",              "✅  " + AppConfig.SCHEMA_FILE},
                {"UserRepository",      "✅  Sẵn sàng"},
                {"AuctionRepository",   "✅  Sẵn sàng"},
                {"BidRepository",       "✅  Sẵn sàng"},
                {"AuctionHouse",        "✅  Observer đã kết nối"},
                {"NotificationService", "✅  Inbox in-memory"},
                {"AuctionScheduler",    "✅  Poll interval 10s"},
                {"AuthService",         "✅  BCrypt ROUNDS=12"},
                {"Platform Fee",        "✅  " + (int)(AppConfig.PLATFORM_FEE_RATE * 100) + "%"},
        };

        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(8);
        grid.setPadding(new Insets(12, 0, 0, 0));

        for (int i = 0; i < items.length; i++) {
            Label key = new Label(items[i][0]);
            key.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 13px;");
            Label val = new Label(items[i][1]);
            val.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13px;");
            grid.add(key, 0, i);
            grid.add(val, 1, i);
        }
        box.getChildren().add(grid);

        box.getChildren().add(sectionLabel("ACTIONS"));
        HBox actions = new HBox(10);
        Button btnSeed   = actionButton("📦  Nạp seed data",    "#0f3460", this::seedDatabase);
        Button btnClear  = actionButton("🗑  Xoá tất cả data",  "#4a1020", this::clearDatabase);
        Button btnReload = actionButton("🔄  Reload services",  "#1a3a1a",
                () -> log("Services đã reload (restart app để áp dụng)."));
        actions.getChildren().addAll(btnSeed, btnClear, btnReload);
        box.getChildren().add(actions);

        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #1a1a2e;");
        tab.setContent(sp);
        return tab;
    }

    // ── Tab 2: Danh sách vật phẩm ─────────────────────────────────────────────

    private Tab buildItemsTab() {
        Tab tab = new Tab("  🏷  Vật phẩm  ");

        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #1a1a2e;");

        box.getChildren().add(sectionLabel("VẬT PHẨM ĐANG ĐẤU GIÁ (ACTIVE)"));

        TextArea itemArea = styledTextArea(18);
        box.getChildren().add(itemArea);

        HBox buttons = new HBox(10);
        Button btnLoad   = actionButton("🔍  Load Active Items",  "#0f3460", () -> loadActiveItems(itemArea));
        Button btnAll    = actionButton("📋  Load All Items",      "#1a3a1a", () -> loadAllItems(itemArea));
        Button btnSearch = actionButton("🔎  Search: 'iPhone'",   "#2a2a5a", () -> searchItems(itemArea, "iPhone"));
        buttons.getChildren().addAll(btnLoad, btnAll, btnSearch);
        box.getChildren().add(buttons);

        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #1a1a2e;");
        tab.setContent(sp);
        return tab;
    }

    // ── Tab 3: Users ──────────────────────────────────────────────────────────

    private Tab buildUsersTab() {
        Tab tab = new Tab("  👤  Users  ");

        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #1a1a2e;");

        box.getChildren().add(sectionLabel("QUẢN LÝ NGƯỜI DÙNG"));

        TextArea userArea = styledTextArea(15);
        box.getChildren().add(userArea);

        box.getChildren().add(sectionLabel("ĐĂNG KÝ USER MỚI (TEST)"));
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        TextField tfUser  = styledField("username");
        TextField tfEmail = styledField("email@test.com");
        TextField tfPass  = styledField("Password1");
        TextField tfName  = styledField("Họ và tên");
        ComboBox<String> cbRole = new ComboBox<>();
        cbRole.getItems().addAll("BUYER", "SELLER");
        cbRole.setValue("BUYER");
        cbRole.setStyle("-fx-background-color: #0f3460; -fx-text-fill: white;");

        form.addRow(0, label("Username:"), tfUser,  label("Email:"), tfEmail);
        form.addRow(1, label("Password:"), tfPass,  label("Tên:"),   tfName);
        form.addRow(2, label("Role:"),     cbRole);
        box.getChildren().add(form);

        HBox buttons = new HBox(10);
        Button btnList     = actionButton("📋  Load All Users", "#0f3460",
                () -> loadAllUsers(userArea));
        Button btnRegister = actionButton("➕  Đăng ký",        "#1a3a1a",
                () -> registerUser(tfUser.getText(), tfEmail.getText(),
                        tfPass.getText(), tfName.getText(), cbRole.getValue(), userArea));
        Button btnLogin    = actionButton("🔑  Test Login",     "#2a2a5a",
                () -> testLogin(tfUser.getText(), tfPass.getText(), userArea));
        buttons.getChildren().addAll(btnList, btnRegister, btnLogin);
        box.getChildren().add(buttons);

        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #1a1a2e;");
        tab.setContent(sp);
        return tab;
    }

    // ── Tab 4: Đặt Bid ────────────────────────────────────────────────────────

    private Tab buildBidTab() {
        Tab tab = new Tab("  💰  Bid  ");

        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #1a1a2e;");

        box.getChildren().add(sectionLabel("ĐẶT BID (TEST)"));

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        TextField tfItemId  = styledField("1");
        TextField tfBuyer   = styledField("4");
        TextField tfAmount  = styledField("23000000");
        form.addRow(0, label("Item ID:"),    tfItemId);
        form.addRow(1, label("Buyer ID:"),   tfBuyer);
        form.addRow(2, label("Amount (đ):"), tfAmount);
        box.getChildren().add(form);

        TextArea bidArea = styledTextArea(14);
        box.getChildren().add(bidArea);

        HBox buttons = new HBox(10);
        Button btnBid     = actionButton("💰  Đặt Bid",      "#0f3460",
                () -> placeBid(tfItemId.getText(), tfBuyer.getText(), tfAmount.getText(), bidArea));
        Button btnHistory = actionButton("📜  Lịch sử Bid",  "#1a3a1a",
                () -> loadBidHistory(tfItemId.getText(), bidArea));
        Button btnBuyNow  = actionButton("⚡  Buy Now",       "#3a1a00",
                () -> testBuyNow(tfItemId.getText(), tfBuyer.getText(), bidArea));
        buttons.getChildren().addAll(btnBid, btnHistory, btnBuyNow);
        box.getChildren().add(buttons);

        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #1a1a2e;");
        tab.setContent(sp);
        return tab;
    }

    // ── Tab 5: Log ────────────────────────────────────────────────────────────

    private Tab buildLogTab() {
        Tab tab = new Tab("  📋  Log  ");

        logArea = styledTextArea(999);
        logArea.setStyle(logArea.getStyle() + "-fx-font-family: 'Courier New', monospace;");

        VBox box = new VBox(8);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: #1a1a2e;");

        HBox btns = new HBox(8);
        btns.getChildren().add(actionButton("🗑  Xoá log", "#4a1020", () -> logArea.clear()));
        box.getChildren().addAll(btns, logArea);
        VBox.setVgrow(logArea, Priority.ALWAYS);

        tab.setContent(box);
        return tab;
    }

    private HBox buildFooter() {
        HBox footer = new HBox();
        footer.setPadding(new Insets(8, 16, 8, 16));
        footer.setStyle("-fx-background-color: #0f0f1e;");

        // FIX: Trước đây dùng AppConfig.DB_URL → lỗi sau khi refactor.
        // Bây giờ AppConfig có 2 URL riêng: SQLITE_URL và MYSQL_URL.
        // Dùng biểu thức điều kiện để lấy URL đang active.
        String activeUrl = AppConfig.USE_MYSQL ? AppConfig.MYSQL_URL : AppConfig.SQLITE_URL;

        // Rút gọn URL cho footer cho dễ đọc (cắt bỏ các tham số dài của MySQL)
        String displayUrl = activeUrl.contains("?")
                ? activeUrl.substring(0, activeUrl.indexOf("?"))
                : activeUrl;

        Label lbl = new Label("DB: " + displayUrl
                + "   |   Min duration: " + AppConfig.MIN_AUCTION_DURATION_MINUTES + "min"
                + "   |   Fee: " + (int)(AppConfig.PLATFORM_FEE_RATE * 100) + "%");
        lbl.setStyle("-fx-text-fill: #606080; -fx-font-size: 11px;");
        footer.getChildren().add(lbl);
        return footer;
    }

    // ─── Business Actions ─────────────────────────────────────────────────────

    private void startScheduler() {
        sl.getAuctionScheduler().start();
        log("✅ AuctionScheduler khởi động – poll mỗi 10 giây.");
    }

    private void runSmokeTest() {
        new Thread(() -> {
            try {
                Thread.sleep(500); // Chờ UI render xong rồi mới log
                Platform.runLater(() -> {
                    log("━━━━━━━━━━━━━━ SMOKE TEST ━━━━━━━━━━━━━━");
                    log("DB: " + (AppConfig.USE_MYSQL ? "MySQL" : "SQLite"));
                    log("Platform fee: " + (int)(AppConfig.PLATFORM_FEE_RATE * 100) + "%");
                    try {
                        int userCount = sl.getUserRepo().findAll().size();
                        int itemCount = sl.getAuctionRepo().findByStatus(AuctionStatus.ACTIVE).size();
                        log("Users trong DB: " + userCount);
                        log("Active items:   " + itemCount);
                        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        log("✅ Backend khởi động thành công!");
                    } catch (Exception e) {
                        log("⚠️  DB chưa có data – hãy bấm 'Nạp seed data' ở tab Hệ thống.");
                        log("   Chi tiết lỗi: " + e.getMessage());
                    }
                });
            } catch (InterruptedException ignored) {}
        }, "SmokeTest").start();
    }

    private void loadActiveItems(TextArea area) {
        runAsync(() -> {
            List<AuctionItem> items = sl.getAuctionRepo().findByStatus(AuctionStatus.ACTIVE);
            StringBuilder sb = new StringBuilder();
            if (items.isEmpty()) { sb.append("Không có vật phẩm ACTIVE.\n"); }
            for (AuctionItem item : items) {
                sb.append(String.format("─── #%d  %s\n", item.getId(), item.getTitle()));
                sb.append(String.format("    Type: %-10s  Status: %s\n", item.getItemType(), item.getStatus()));
                sb.append(String.format("    Giá hiện tại: %s\n", DateTimeUtils.formatCurrency(item.getCurrentPrice())));
                sb.append(String.format("    Kết thúc: %s  (còn: %s)\n",
                        DateTimeUtils.formatShort(item.getEndTime()),
                        DateTimeUtils.formatCountdown(item.getEndTime())));
                sb.append(String.format("    Bids: %d  |  Leading bidder: %s\n\n",
                        sl.getBidRepo().countByAuctionId(item.getId()),
                        item.getLeadingBidderId() > 0 ? "#" + item.getLeadingBidderId() : "(chưa có)"));
            }
            area.setText(sb.toString());
            log("Loaded " + items.size() + " active items.");
        });
    }

    private void loadAllItems(TextArea area) {
        runAsync(() -> {
            Page<AuctionItem> page = sl.getAuctionRepo().search(
                    FilterCriteria.builder().build(), 0, 20);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Tổng: %d vật phẩm\n\n", page.getTotalElements()));
            for (AuctionItem item : page.getContent()) {
                sb.append(String.format("#%d  [%s]  %s  –  %s\n",
                        item.getId(), item.getStatus().name(),
                        item.getTitle(), DateTimeUtils.formatCurrency(item.getCurrentPrice())));
            }
            area.setText(sb.toString());
        });
    }

    private void searchItems(TextArea area, String keyword) {
        runAsync(() -> {
            FilterCriteria f = FilterCriteria.builder().keyword(keyword).activeOnly(true).build();
            Page<AuctionItem> page = sl.getAuctionRepo().search(f, 0, 10);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Kết quả tìm '%s': %d items\n\n", keyword, page.getTotalElements()));
            for (AuctionItem item : page.getContent()) {
                sb.append(String.format("  #%d  %s  –  %s\n",
                        item.getId(), item.getTitle(), DateTimeUtils.formatCurrency(item.getCurrentPrice())));
            }
            area.setText(sb.toString());
        });
    }

    private void loadAllUsers(TextArea area) {
        runAsync(() -> {
            List<User> users = sl.getUserRepo().findAll();
            StringBuilder sb = new StringBuilder();
            for (User u : users) {
                sb.append(String.format("#%-3d  %-12s  %-8s  %s\n",
                        u.getId(), u.getUsername(), u.getRole().name(),
                        u.isActive() ? "ACTIVE" : "DISABLED"));
                if (u instanceof Buyer b)
                    sb.append(String.format("       Ví: %s  |  Wins: %d\n",
                            DateTimeUtils.formatCurrency(b.getWalletBalance()), b.getTotalWins()));
            }
            area.setText(sb.toString());
            log("Loaded " + users.size() + " users.");
        });
    }

    private void registerUser(String username, String email, String pass,
                              String name, String role, TextArea area) {
        runAsync(() -> {
            User u = sl.getAuthService().register(username, email, pass, name, "0900000000", role);
            area.setText("✅ Đăng ký thành công!\n\n" + u);
            log("Registered: " + u.getUsername() + " [" + role + "]");
        });
    }

    private void testLogin(String username, String pass, TextArea area) {
        runAsync(() -> {
            User u = sl.getAuthService().login(username, pass);
            area.setText("✅ Đăng nhập thành công!\n\n"
                    + "Username : " + u.getUsername() + "\n"
                    + "Role     : " + u.getRole() + "\n"
                    + "Full name: " + u.getFullName() + "\n"
                    + "Active   : " + u.isActive());
            log("Login OK: " + u.getUsername());
        });
    }

    private void placeBid(String itemIdStr, String buyerIdStr, String amountStr, TextArea area) {
        runAsync(() -> {
            int itemId   = Integer.parseInt(itemIdStr.trim());
            int buyerId  = Integer.parseInt(buyerIdStr.trim());
            BigDecimal a = new BigDecimal(amountStr.trim());
            Bid bid = sl.getAuctionHouse().placeBid(itemId, buyerId, a);
            area.setText("✅ Bid thành công!\n\n" + bid
                    + "\n\nAmount: " + DateTimeUtils.formatCurrency(bid.getAmount()));
            log("Bid đặt: #" + bid.getId() + " – " + DateTimeUtils.formatCurrency(bid.getAmount()));
        });
    }

    private void loadBidHistory(String itemIdStr, TextArea area) {
        runAsync(() -> {
            int itemId = Integer.parseInt(itemIdStr.trim());
            List<Bid> bids = sl.getBidRepo().findByAuctionId(itemId);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Lịch sử bid item #%d: %d bids\n\n", itemId, bids.size()));
            for (Bid b : bids) {
                sb.append(String.format("  #%-5d  %-12s  %s  %s  %s\n",
                        b.getId(), b.getBuyerUsername(),
                        DateTimeUtils.formatCurrency(b.getAmount()),
                        DateTimeUtils.formatRelative(b.getBidTime()),
                        b.isAutoBid() ? "[AUTO]" : ""));
            }
            area.setText(sb.toString());
        });
    }

    private void testBuyNow(String itemIdStr, String buyerIdStr, TextArea area) {
        runAsync(() -> {
            int itemId  = Integer.parseInt(itemIdStr.trim());
            int buyerId = Integer.parseInt(buyerIdStr.trim());
            sl.getAuctionHouse().buyNow(itemId, buyerId);
            area.setText("✅ Buy-Now thành công!\nItem #" + itemId + " đã được mua bởi buyer #" + buyerId);
            log("BuyNow: item #" + itemId + " → buyer #" + buyerId);
        });
    }

    /**
     * Nạp seed data từ seed.sql.
     *
     * FIX: Phiên bản cũ lấy connection bằng getConnection() của singleton cũ
     * (trả về connection không cần đóng). Với HikariCP, getConnection() trả về
     * connection từ pool → BẮT BUỘC phải đóng sau khi dùng.
     *
     * try-with-resources đảm bảo conn.close() được gọi tự động → connection
     * được trả về pool, không bị leak.
     *
     * LƯU Ý: seed.sql dùng "INSERT OR IGNORE" (SQLite). Khi dùng MySQL,
     * phải đổi thành "INSERT IGNORE" – xem file MYSQL_MIGRATION_GUIDE.md.
     */
    private void seedDatabase() {
        runAsync(() -> {
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 Statement stmt = conn.createStatement();
                 InputStream is = getClass().getResourceAsStream(AppConfig.SEED_FILE)) {

                if (is == null) {
                    log("❌ Không tìm thấy file: " + AppConfig.SEED_FILE);
                    return;
                }

                String sql;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    sql = reader.lines().collect(Collectors.joining("\n"));
                }

                // Chạy từng statement, bỏ qua comment và dòng trống
                for (String s : sql.split(";")) {
                    String trimmed = s.strip();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        stmt.execute(trimmed);
                    }
                }
                log("✅ Seed data đã được nạp thành công!");

            } catch (Exception e) {
                log("❌ Lỗi seed: " + e.getMessage());
            }
        });
    }

    private void clearDatabase() {
        runAsync(() -> {
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 Statement stmt = conn.createStatement()) {

                // Xoá theo thứ tự từ bảng con → bảng cha để không vi phạm FK
                String[] tables = {"transactions", "watchlist", "bids", "digital_items",
                        "physical_items", "auctions", "admins", "sellers", "buyers", "users"};
                for (String t : tables) stmt.execute("DELETE FROM " + t);

                log("🗑  Đã xoá toàn bộ data.");

            } catch (Exception e) {
                log("❌ Lỗi clear: " + e.getMessage());
            }
        });
    }

    // ─── Utility Helpers ──────────────────────────────────────────────────────

    /**
     * Chạy một task trên background thread.
     *
     * Tại sao không chạy thẳng trên FX thread?
     * → FX thread chịu trách nhiệm render UI. Nếu chạy DB query trực tiếp,
     *   UI sẽ bị đóng băng trong khi query đang chạy.
     * → Giải pháp: chạy query trên background thread, rồi dùng
     *   Platform.runLater() để cập nhật UI từ FX thread.
     *
     * runAsync() xử lý exception và hiển thị lỗi vào log tự động.
     */
    private void runAsync(ThrowingRunnable task) {
        new Thread(() -> {
            try {
                task.run();
            } catch (Exception e) {
                Platform.runLater(() -> log("❌ " + e.getClass().getSimpleName() + ": " + e.getMessage()));
            }
        }).start();
    }

    private void log(String msg) {
        String line = "[" + DateTimeUtils.formatTime(java.time.LocalDateTime.now()) + "]  " + msg;
        if (logArea != null) {
            Platform.runLater(() -> {
                logArea.appendText(line + "\n");
                logArea.setScrollTop(Double.MAX_VALUE);
            });
        }
        LOG.info(msg);
    }

    @FunctionalInterface
    interface ThrowingRunnable { void run() throws Exception; }

    // ─── UI Factories ─────────────────────────────────────────────────────────

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #e94560; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8 0 0 0;");
        return l;
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 13px;");
        return l;
    }

    private Button actionButton(String text, String bgColor, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: #e0e0e0; "
                + "-fx-font-size: 13px; -fx-padding: 7 14; -fx-cursor: hand; -fx-background-radius: 5;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.8));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private TextArea styledTextArea(int rows) {
        TextArea ta = new TextArea();
        ta.setPrefRowCount(rows);
        ta.setEditable(false);
        ta.setWrapText(false);
        ta.setStyle("-fx-background-color: #0d0d1a; -fx-text-fill: #c8c8e0; "
                + "-fx-font-size: 12.5px; -fx-border-color: #303060; "
                + "-fx-border-radius: 4; -fx-background-radius: 4;");
        return ta;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #0f3460; -fx-text-fill: #e0e0e0; "
                + "-fx-prompt-text-fill: #606090; -fx-font-size: 13px; "
                + "-fx-border-color: #303060; -fx-border-radius: 4; -fx-background-radius: 4;");
        tf.setPrefWidth(180);
        return tf;
    }

    // ─── Main ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        launch(args);
    }
}