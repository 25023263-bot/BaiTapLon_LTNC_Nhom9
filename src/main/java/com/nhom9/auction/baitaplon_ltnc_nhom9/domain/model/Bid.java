package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A single bid placed by a buyer on an auction listing.
 */
public class Bid {

    private int id;
    private int auctionId;
    private int buyerId;
    /** Buyer's username (for display; populated from {@code users} when joining). */
    private String buyerUsername;
    private BigDecimal amount;
    private LocalDateTime bidTime;
    private boolean autoBid;
    private BigDecimal autoBidLimit;

    public Bid() {}

    public Bid(int auctionId, int buyerId, BigDecimal amount) {
        this.auctionId = auctionId;
        this.buyerId   = buyerId;
        this.amount    = amount;
        this.bidTime   = LocalDateTime.now();
        this.autoBid   = false;
    }

    public Bid(int id, int auctionId, int buyerId, String buyerUsername,
               BigDecimal amount, LocalDateTime bidTime, boolean autoBid, BigDecimal autoBidLimit) {
        this.id             = id;
        this.auctionId      = auctionId;
        this.buyerId        = buyerId;
        this.buyerUsername  = buyerUsername;
        this.amount         = amount;
        this.bidTime        = bidTime;
        this.autoBid        = autoBid;
        this.autoBidLimit   = autoBidLimit;
    }

    public boolean canAutoBidUp(BigDecimal newAmount) {
        return autoBid && autoBidLimit != null && autoBidLimit.compareTo(newAmount) >= 0;
    }

    public BigDecimal calculateAutoBidAmount(BigDecimal increment, BigDecimal rivalBid) {
        if (!autoBid || autoBidLimit == null) return null;
        BigDecimal needed = rivalBid.add(increment);
        if (autoBidLimit.compareTo(needed) < 0) return null;
        return needed.min(autoBidLimit);
    }

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public int getAuctionId()                       { return auctionId; }
    public void setAuctionId(int auctionId)       { this.auctionId = auctionId; }

    public int getBuyerId()                         { return buyerId; }
    public void setBuyerId(int buyerId)             { this.buyerId = buyerId; }

    public String getBuyerUsername()               { return buyerUsername; }
    public void setBuyerUsername(String name)       { this.buyerUsername = name; }

    public BigDecimal getAmount()                   { return amount; }
    public void setAmount(BigDecimal amount)        { this.amount = amount; }

    public LocalDateTime getBidTime()               { return bidTime; }
    public void setBidTime(LocalDateTime bidTime)   { this.bidTime = bidTime; }

    public boolean isAutoBid()                      { return autoBid; }
    public void setAutoBid(boolean autoBid)          { this.autoBid = autoBid; }

    public BigDecimal getAutoBidLimit()             { return autoBidLimit; }
    public void setAutoBidLimit(BigDecimal limit)   { this.autoBidLimit = limit; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bid)) return false;
        return id == ((Bid) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Bid{id=%d, auctionId=%d, buyer='%s', amount=%s, time=%s}",
                id, auctionId, buyerUsername, amount, bidTime);
    }
}
