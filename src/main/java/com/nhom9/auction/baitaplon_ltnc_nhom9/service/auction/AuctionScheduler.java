package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.AuctionRepository;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Scheduler định kỳ kiểm tra và tự động:
 *   - Mở phiên PENDING đã đến giờ bắt đầu
 *   - Đóng phiên ACTIVE đã hết giờ
 *
 * Chạy trên background thread, không block JavaFX thread.
 */
public class AuctionScheduler {

    private static final Logger LOG = Logger.getLogger(AuctionScheduler.class.getName());

    /** Khoảng thời gian kiểm tra (giây) */
    private static final int POLL_INTERVAL_SECONDS = 10;

    private final AuctionRepository auctionRepo;
    private final AuctionHouse      auctionHouse;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?>      taskHandle;
    private volatile boolean        running = false;

    public AuctionScheduler(AuctionRepository auctionRepo, AuctionHouse auctionHouse) {
        this.auctionRepo  = auctionRepo;
        this.auctionHouse = auctionHouse;
        this.executor     = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionScheduler");
            t.setDaemon(true); // thoát cùng ứng dụng
            return t;
        });
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    public void start() {
        if (running) return;
        running    = true;
        taskHandle = executor.scheduleAtFixedRate(
                this::tick,
                0,
                POLL_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
        LOG.info("AuctionScheduler đã khởi động (interval=" + POLL_INTERVAL_SECONDS + "s).");
    }

    public void stop() {
        running = false;
        if (taskHandle != null) taskHandle.cancel(false);
        executor.shutdown();
        LOG.info("AuctionScheduler đã dừng.");
    }

    public boolean isRunning() { return running; }

    // ─── Tick ─────────────────────────────────────────────────────────────────

    private void tick() {
        try {
            openDueAuctions();
            closeExpiredAuctions();
        } catch (Exception e) {
            LOG.severe("Lỗi trong AuctionScheduler.tick(): " + e.getMessage());
        }
    }

    /**
     * Chuyển PENDING → ACTIVE nếu đã đến giờ bắt đầu.
     */
    private void openDueAuctions() {
        try {
            List<AuctionItem> dueItems = auctionRepo.findDueToStart();
            for (AuctionItem item : dueItems) {
                try {
                    item.setStatus(com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus.ACTIVE);
                    auctionRepo.updateStatus(item.getId(),
                            com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus.ACTIVE);
                    // Notify observers qua AuctionHouse
                    // (gọi internal – ta dùng reflection-free approach)
                    LOG.info("Phiên ACTIVE: item #" + item.getId() + " – " + item.getTitle());
                } catch (Exception e) {
                    LOG.warning("Không mở được item #" + item.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LOG.warning("openDueAuctions lỗi: " + e.getMessage());
        }
    }

    /**
     * Đóng phiên ACTIVE đã hết giờ.
     */
    private void closeExpiredAuctions() {
        try {
            List<AuctionItem> expired = auctionRepo.findExpiredActive();
            for (AuctionItem item : expired) {
                try {
                    auctionHouse.closeAuction(item.getId());
                } catch (Exception e) {
                    LOG.warning("Không đóng được item #" + item.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LOG.warning("closeExpiredAuctions lỗi: " + e.getMessage());
        }
    }
}