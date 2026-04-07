package model;

import model.enums.AuctionStatus;
import service.Auctionable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public abstract class AuctionItem implements Auctionable {
    protected int id;
    protected String name;
    protected String description;
    protected double startingPrice;
    protected double currentPrice;
    protected double minBidStep;
    protected LocalDateTime endTime;
    protected AuctionStatus status;
    protected User seller;
    protected List<Bid> bidHistory;

    public AuctionItem(int id, String name, String description, double startingPrice, double minBidStep, User seller) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.minBidStep = minBidStep;
        this.seller = seller;
        this.status = AuctionStatus.PENDING;
        this.bidHistory = new ArrayList<>();
    }

    @Override
    public void placeBid(User bidder, double amount) throws Exception {
        if (status != AuctionStatus.ACTIVE) {
            throw new Exception("Auction is not active");
        }
        if (amount <= currentPrice + minBidStep) {
            throw new Exception("Bid too low: must be at least " + (currentPrice + minBidStep));
        }
        if (bidder.getBalance() < amount) {
            throw new Exception("Insufficient balance");
        }
        // Create bid and update
        Bid bid = new Bid(bidder, this, amount, java.time.LocalDateTime.now());
        bidHistory.add(bid);
        currentPrice = amount;
        // Also add to buyer's bid list if buyer is instance of Buyer
        if (bidder instanceof Buyer) {
            ((Buyer) bidder).addBid(bid);
        }
    }

    @Override
    public double getCurrentPrice() {
        return currentPrice;
    }

    @Override
    public void closeAuction() {
        if (status == AuctionStatus.ACTIVE) {
            status = AuctionStatus.CLOSED;
            // Determine winner (highest bid)
            if (!bidHistory.isEmpty()) {
                Bid winningBid = bidHistory.stream()
                        .max((b1, b2) -> Double.compare(b1.getAmount(), b2.getAmount()))
                        .orElse(null);
                if (winningBid != null) {
                    // Payment would be handled externally, here just notify
                    System.out.println("Auction closed. Winner: " + winningBid.getBidder().getName() +
                            " with amount " + winningBid.getAmount());
                }
            } else {
                System.out.println("Auction closed with no bids.");
            }
        }
    }

    @Override
    public AuctionStatus getStatus() {
        return status;
    }

    public void startAuction(LocalDateTime endTime) {
        if (status == AuctionStatus.PENDING) {
            this.status = AuctionStatus.ACTIVE;
            this.endTime = endTime;
        } else {
            throw new IllegalStateException("Cannot start auction: status is " + status);
        }
    }

    // Getters and setters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getStartingPrice() { return startingPrice; }
    public double getMinBidStep() { return minBidStep; }
    public LocalDateTime getEndTime() { return endTime; }
    public User getSeller() { return seller; }
    public List<Bid> getBidHistory() { return bidHistory; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("AuctionItem{id=%d, name='%s', currentPrice=%.2f, status=%s}",
                id, name, currentPrice, status);
    }
}