package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.Item;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.observer.BidObserver;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Auction {
    private String auctionId;
    private Item item;
    private double currentHighestBid;
    private Buyer highestBuyer;
    private List<Bid> bidHistory;
    private AuctionStatus status;
    private Date startTime;
    private Date endTime;

    private List<BidObserver> observers;
    private final ReentrantLock lock = new ReentrantLock();

    public Auction(Item item, Date startTime, Date endTime) {
        this.auctionId = UUID.randomUUID().toString();
        this.item = item;
        this.currentHighestBid = item.getStartingPrice();
        this.bidHistory = new ArrayList<>();
        this.observers = new ArrayList<>();
        this.status = AuctionStatus.OPEN;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Observer quản lý (thread‑safe)
    public void addObserver(BidObserver observer) {
        lock.lock();
        try { observers.add(observer); } finally { lock.unlock(); }
    }

    public void removeObserver(BidObserver observer) {
        lock.lock();
        try { observers.remove(observer); } finally { lock.unlock(); }
    }

    private void notifyObservers(Bid bid) {
        for (BidObserver obs : observers) {
            obs.onBidPlaced(bid, this);
        }
    }

    /**
     * Đặt giá – sử dụng checked exceptions để controller xử lý rõ ràng.
     */
    public void placeBid(Bid bid) throws InvalidBidException, AuctionClosedException {
        lock.lock();
        try {
            if (status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Auction is not running (current status: " + status + ")");
            }
            if (bid.getAmount() <= currentHighestBid) {
                throw new InvalidBidException(
                        String.format("Bid %.2f is not higher than current %.2f", bid.getAmount(), currentHighestBid)
                );
            }
            bidHistory.add(bid);
            currentHighestBid = bid.getAmount();
            highestBuyer = bid.getBuyer();
            notifyObservers(bid);
        } finally {
            lock.unlock();
        }
    }

    // Chuyển trạng thái (có lock)
    public void start() {
        lock.lock();
        try {
            if (status == AuctionStatus.OPEN) {
                status = AuctionStatus.RUNNING;
                System.out.println("Auction " + auctionId + " started.");
            }
        } finally { lock.unlock(); }
    }

    public void finish() {
        lock.lock();
        try {
            if (status == AuctionStatus.RUNNING) {
                status = AuctionStatus.FINISHED;
                System.out.println("Auction " + auctionId + " finished. Winner: " +
                        (highestBuyer != null ? highestBuyer.getUsername() : "none"));
            }
        } finally { lock.unlock(); }
    }

    public void markPaid() {
        lock.lock();
        try {
            if (status == AuctionStatus.FINISHED) {
                status = AuctionStatus.PAID;
            }
        } finally { lock.unlock(); }
    }

    public void cancel() {
        lock.lock();
        try {
            if (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING) {
                status = AuctionStatus.CANCELED;
            }
        } finally { lock.unlock(); }
    }

    // Getters
    public String getAuctionId() { return auctionId; }
    public Item getItem() { return item; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public Buyer getHighestBuyer() { return highestBuyer; }
    public AuctionStatus getStatus() { return status; }
    public Date getStartTime() { return startTime; }
    public Date getEndTime() { return endTime; }
}
