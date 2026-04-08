package service;

import model.AuctionItem;
import model.Buyer;
import model.User;

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