package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction;

import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.*;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.DatabaseConnection;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.AuctionHouse;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.AuctionScheduler;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth.AuthService;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.notification.NotificationService;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.payment.CreditCardPayment;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.payment.WalletPayment;

/**
 * Service Locator – khởi tạo toàn bộ dependency một lần duy nhất khi app start.
 * Các Controller lấy service qua ServiceLocator.getInstance().getXxx().
 *
 * (Thay thế gọn cho DI framework như Spring/Guice trong app desktop nhỏ.)
 */
public class ServiceLocator {

    private static ServiceLocator instance;

    // ── Repositories ─────────────────────────────────────────────────────────
    private final UserRepository        userRepo;
    private final ItemRepository        itemRepo;
    private final BidRepository         bidRepo;
    private final WatchlistRepository   watchlistRepo;
    private final TransactionRepository txRepo;

    // ── Services ──────────────────────────────────────────────────────────────
    private final AuthService           authService;
    private final AuctionHouse          auctionHouse;
    private final AuctionScheduler      auctionScheduler;
    private final NotificationService   notificationService;
    private final WalletPayment         walletPayment;
    private final CreditCardPayment     creditCardPayment;

    // ─── Init ─────────────────────────────────────────────────────────────────

    private ServiceLocator() {
        // 1. DB connection (Singleton – tự chạy schema.sql)
        DatabaseConnection.getInstance();

        // 2. Repositories
        userRepo      = new UserRepository();
        itemRepo      = new ItemRepository();
        bidRepo       = new BidRepository();
        watchlistRepo = new WatchlistRepository();
        txRepo        = new TransactionRepository();

        // 3. Services
        authService         = new AuthService(userRepo);
        auctionHouse        = new AuctionHouse(itemRepo, bidRepo, userRepo, txRepo);
        notificationService = new NotificationService(watchlistRepo);
        auctionScheduler    = new AuctionScheduler(itemRepo, auctionHouse);
        walletPayment       = new WalletPayment(userRepo);
        creditCardPayment   = new CreditCardPayment();

        // 4. Kết nối Observer: AuctionHouse → NotificationService
        auctionHouse.addObserver(notificationService);
    }

    public static ServiceLocator getInstance() {
        if (instance == null) instance = new ServiceLocator();
        return instance;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public UserRepository        getUserRepo()           { return userRepo; }
    public ItemRepository        getItemRepo()           { return itemRepo; }
    public BidRepository         getBidRepo()            { return bidRepo; }
    public WatchlistRepository   getWatchlistRepo()      { return watchlistRepo; }
    public TransactionRepository getTxRepo()             { return txRepo; }

    public AuthService           getAuthService()        { return authService; }
    public AuctionHouse          getAuctionHouse()       { return auctionHouse; }
    public AuctionScheduler      getAuctionScheduler()   { return auctionScheduler; }
    public NotificationService   getNotificationService(){ return notificationService; }
    public WalletPayment         getWalletPayment()      { return walletPayment; }
    public CreditCardPayment     getCreditCardPayment()  { return creditCardPayment; }

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    public void shutdown() {
        auctionScheduler.stop();
        DatabaseConnection.getInstance().close();
    }
}