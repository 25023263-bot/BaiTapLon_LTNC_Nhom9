package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int auctionId;
    private int buyerId;
    private String buyerUsername;
    private BigDecimal amount;
    private LocalDateTime bidTime;
    private boolean autoBid;
    private boolean isLeading;

    public BidDTO() {}

    public BidDTO(int id, int auctionId, int buyerId,
                  String buyerUsername, BigDecimal amount, LocalDateTime bidTime,
                  boolean autoBid) {
        this.id              = id;
        this.auctionId       = auctionId;
        this.buyerId         = buyerId;
        this.buyerUsername   = buyerUsername;
        this.amount          = amount;
        this.bidTime         = bidTime;
        this.autoBid         = autoBid;
        this.isLeading       = false;
    }

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public int getAuctionId()                       { return auctionId; }
    public void setAuctionId(int auctionId)         { this.auctionId = auctionId; }

    public int getBuyerId()                         { return buyerId; }
    public void setBuyerId(int buyerId)             { this.buyerId = buyerId; }

    public String getBuyerUsername()                { return buyerUsername; }
    public void setBuyerUsername(String name)       { this.buyerUsername = name; }

    public BigDecimal getAmount()                   { return amount; }
    public void setAmount(BigDecimal amount)        { this.amount = amount; }

    public LocalDateTime getBidTime()               { return bidTime; }
    public void setBidTime(LocalDateTime bidTime)   { this.bidTime = bidTime; }

    public boolean isAutoBid()                      { return autoBid; }
    public void setAutoBid(boolean autoBid)         { this.autoBid = autoBid; }

    public boolean isLeading()                      { return isLeading; }
    public void setLeading(boolean leading)         { isLeading = leading; }

    @Override
    public String toString() {
        return String.format("BidDTO{id=%d, auctionId=%d, buyer='%s', amount=%s, leading=%s}",
                id, auctionId, buyerUsername, amount, isLeading);
    }
}
