package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction;

import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.*;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DatabaseConnection;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth.AuthService;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.notification.NotificationService;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.listing.ListingService;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.payment.WalletPayment;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.wallet.WalletDepositService;

/**
 * Service Locator — khởi tạo toàn bộ dependency một lần duy nhất khi app start.
 * Các Controller lấy service qua ServiceLocator.getInstance().getXxx().
 *
 * <p>(Thay thế gọn cho DI framework như Spring/Guice trong app desktop nhỏ.)
 *
 * <h3>Tại sao dùng Eager Initialization thay vì Lazy?</h3>
 * <pre>
 *   // Lazy (cũ — có vấn đề):
 *   if (instance == null) instance = new ServiceLocator(); // ← KHÔNG an toàn
 *
 *   // Eager (mới — an toàn):
 *   private static final ServiceLocator INSTANCE = new ServiceLocator();
 * </pre>
 *
 * <p>Lazy initialization bị "race condition" nếu 2 thread gọi {@code getInstance()}
 * cùng lúc — cả hai đều thấy {@code instance == null} và tạo 2 instance khác nhau.
 * Với desktop app một thread thì hiếm xảy ra, nhưng khi bạn thêm background thread
 * (scheduler, poller) thì sẽ thành bug thật. Eager init an toàn hơn và đơn giản hơn.
 *
 * <p>Nếu muốn lazy AN TOÀN hơn mà không cần sửa nhiều, có thể dùng
 * "Initialization-on-Demand Holder" pattern (tìm hiểu thêm khi học Java nâng cao).
 */
public class ServiceLocator {

    // ── Eager initialization — JVM đảm bảo thread-safe, không cần synchronized ──
    private static final ServiceLocator INSTANCE = new ServiceLocator();

    // ── Repositories ─────────────────────────────────────────────────────────
    private final UserRepository        userRepo;
    private final AuctionRepository     auctionRepo;
    private final BidRepository         bidRepo;
    private final WatchlistRepository   watchlistRepo;
    private final TransactionRepository txRepo;

    // ── Services ──────────────────────────────────────────────────────────────
    private final AuthService           authService;
    private final AuctionHouse          auctionHouse;
    private final AuctionScheduler      auctionScheduler;
    private final NotificationService   notificationService;
    private final WalletPayment         walletPayment;
    private final ListingService        listingService;
    private final WalletDepositService  walletDepositService;

    // ─── Init ─────────────────────────────────────────────────────────────────

    private ServiceLocator() {
        // 1. DB connection (Singleton — tự chạy schema.sql)
        DatabaseConnection.getInstance();

        // 2. Repositories
        userRepo      = new UserRepository();
        auctionRepo   = new AuctionRepository();
        bidRepo       = new BidRepository();
        watchlistRepo = new WatchlistRepository();
        txRepo        = new TransactionRepository();

        // 3. Services
        authService  = new AuthService(userRepo);
        auctionHouse = new AuctionHouse(auctionRepo, bidRepo, userRepo, txRepo);

        NotificationRepository notifRepo = new NotificationRepository();
        try { notifRepo.deleteOlderThan(30); }
        catch (Exception e) { /* không critical, bỏ qua */ }

        notificationService = new NotificationService(watchlistRepo, bidRepo, notifRepo);
        auctionScheduler    = new AuctionScheduler(auctionRepo, auctionHouse);
        walletPayment       = new WalletPayment(userRepo);
        listingService      = new ListingService(auctionHouse);
        walletDepositService = new WalletDepositService(userRepo);

        // 4. Kết nối Observer: AuctionHouse → NotificationService
        auctionHouse.addObserver(notificationService);
    }

    public static ServiceLocator getInstance() {
        return INSTANCE;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public UserRepository        getUserRepo()            { return userRepo; }
    public AuctionRepository     getAuctionRepo()         { return auctionRepo; }
    public BidRepository         getBidRepo()             { return bidRepo; }
    public WatchlistRepository   getWatchlistRepo()       { return watchlistRepo; }
    public TransactionRepository getTxRepo()              { return txRepo; }

    public AuthService           getAuthService()         { return authService; }
    public AuctionHouse          getAuctionHouse()        { return auctionHouse; }
    public AuctionScheduler      getAuctionScheduler()    { return auctionScheduler; }
    public NotificationService   getNotificationService() { return notificationService; }
    public WalletPayment         getWalletPayment()       { return walletPayment; }
    public ListingService        getListingService()      { return listingService; }
    public WalletDepositService  getWalletDepositService(){ return walletDepositService; }

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    public void shutdown() {
        auctionScheduler.stop();
        DatabaseConnection.getInstance().close();
    }
}