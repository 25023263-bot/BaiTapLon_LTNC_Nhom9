package org.example.baitaplon_ltnc_nhom9.service.notification;

import org.example.baitaplon_ltnc_nhom9.model.AuctionItem;
import org.example.baitaplon_ltnc_nhom9.model.Buyer;
import org.example.baitaplon_ltnc_nhom9.model.User;
import org.example.baitaplon_ltnc_nhom9.service.auction.AuctionObserver;

public class NotificationService implements AuctionObserver {
    @Override
    public void update(AuctionItem item) {
        // In real app, send emails or push notifications
        System.out.println("NOTIFICATION: Item '" + item.getName() + "' updated. Current price: " + item.getCurrentPrice());
        // Notify watchers: in a full implementation, we would iterate over buyers who have this item in watchlist
        // Here we just simulate
        if (item.getBidHistory() != null && !item.getBidHistory().isEmpty()) {
            User lastBidder = item.getBidHistory().get(item.getBidHistory().size()-1).getBidder();
            System.out.println("NOTIFICATION: " + lastBidder.getName() + " placed a new bid.");
        }
    }
}