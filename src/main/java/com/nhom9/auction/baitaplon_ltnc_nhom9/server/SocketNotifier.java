package com.nhom9.auction.baitaplon_ltnc_nhom9.server;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.server.protocol.Response;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.AuctionObserver;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

/**
 * Cầu nối giữa Observer pattern (AuctionHouse) và Socket (ClientHandler).
 *
 * Khi AuctionHouse gọi onNewBid() → SocketNotifier broadcast
 * đến TẤT CẢ client đang kết nối → mọi người thấy giá mới ngay lập tức.
 *
 * Đây là phần "realtime update qua Observer/Socket" theo yêu cầu bài.
 */
public class SocketNotifier implements AuctionObserver {

    private static final Logger LOG = Logger.getLogger(SocketNotifier.class.getName());
    private final List<ClientHandler> clients;

    public SocketNotifier(List<ClientHandler> clients) {
        this.clients = clients;
    }

    @Override
    public void onNewBid(AuctionItem item, Bid bid) {
        LOG.info("Broadcast bid mới: item #" + item.getId() + ", amount=" + bid.getAmount());
        Response notification = Response.notification(bid);
        clients.forEach(c -> c.sendNotification(notification));
    }

    @Override
    public void onAuctionClosed(AuctionItem item, Integer winnerId) {
        String msg = winnerId != null
                ? "Phiên #" + item.getId() + " kết thúc. Người thắng: #" + winnerId
                : "Phiên #" + item.getId() + " hết hạn (không có bid).";
        Response notification = Response.notification(msg);
        clients.forEach(c -> c.sendNotification(notification));
    }

    @Override
    public void onAuctionStarted(AuctionItem item) {
        Response notification = Response.notification(
                "Phiên #" + item.getId() + " - " + item.getTitle() + " vừa bắt đầu!"
        );
        clients.forEach(c -> c.sendNotification(notification));
    }

    @Override
    public void onAuctionCancelled(AuctionItem item) {
        Response notification = Response.notification(
                "Phiên #" + item.getId() + " đã bị huỷ."
        );
        clients.forEach(c -> c.sendNotification(notification));
    }

    @Override
    public void onAuctionExtended(AuctionItem item, LocalDateTime newEndTime) {
        Response notification = Response.notification(
                "Phiên #" + item.getId() + " được gia hạn đến " + newEndTime
        );
        clients.forEach(c -> c.sendNotification(notification));
    }
}