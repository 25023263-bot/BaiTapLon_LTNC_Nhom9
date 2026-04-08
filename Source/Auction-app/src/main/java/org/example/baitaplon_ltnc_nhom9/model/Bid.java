package org.example.baitaplon_ltnc_nhom9.model;

import java.time.LocalDateTime;

public class Bid {
    private User bidder;
    private AuctionItem item;
    private double amount;
    private LocalDateTime timestamp;

    public Bid(User bidder, AuctionItem item, double amount, LocalDateTime timestamp) {
        this.bidder = bidder;
        this.item = item;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public User getBidder() { return bidder; }
    public AuctionItem getItem() { return item; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("Bid{bidder=%s, item=%s, amount=%.2f, time=%s}",
                bidder.getName(), item.getName(), amount, timestamp);
    }
}