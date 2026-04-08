package org.example.baitaplon_ltnc_nhom9.service;

import org.example.baitaplon_ltnc_nhom9.model.AuctionItem;
import org.example.baitaplon_ltnc_nhom9.model.enums.AuctionStatus;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionScheduler {
    private final AuctionHouse auctionHouse;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public AuctionScheduler(AuctionHouse auctionHouse) {
        this.auctionHouse = auctionHouse;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::checkAndCloseExpiredAuctions, 0, 1, TimeUnit.MINUTES);
    }

    private void checkAndCloseExpiredAuctions() {
        for (AuctionItem item : auctionHouse.getActiveItems()) {
            if (item.getEndTime() != null && item.getEndTime().isBefore(LocalDateTime.now())) {
                item.closeAuction();
                // In a full implementation, we would also trigger payment
            }
        }
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
