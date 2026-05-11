package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO for bid history on the UI.
 */
public class BidDTO {

    private int id;
    private int auctionId;
    /** Title from {@code auctions} (joined for display). */
    private String auctionTitle;
    private int buyerId;
    private String buyerUsername;
    private BigDecimal amount;
    private LocalDateTime bidTime;
    private boolean autoBid;
    private boolean isLeading;

    public BidDTO() {}

    public BidDTO(int id, int auctionId, String auctionTitle, int buyerId,
                  String buyerUsername, BigDecimal amount, LocalDateTime bidTime,
                  boolean autoBid) {
        this.id              = id;
        this.auctionId       = auctionId;
        this.auctionTitle    = auctionTitle;
        this.buyerId         = buyerId;
        this.buyerUsername   = buyerUsername;
        this.amount          = amount;
        this.bidTime         = bidTime;
        this.autoBid         = autoBid;
        this.isLeading       = false;
    }

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public String getFormattedBidTime() {
        return bidTime != null ? bidTime.format(DISPLAY_FORMAT) : "";
    }

    public String getFormattedAmount() {
        return String.format("%,.0f đ", amount);
    }

    public String getStatusLabel() {
        if (isLeading) return "🏆 Đang dẫn đầu";
        return autoBid ? "🤖 Auto-bid" : "Đã vượt";
    }

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public int getAuctionId()                       { return auctionId; }
    public void setAuctionId(int auctionId)       { this.auctionId = auctionId; }

    public String getAuctionTitle()                { return auctionTitle; }
    public void setAuctionTitle(String title)      { this.auctionTitle = title; }

    public int getBuyerId()                         { return buyerId; }
    public void setBuyerId(int buyerId)             { this.buyerId = buyerId; }

    public String getBuyerUsername()              { return buyerUsername; }
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
        return String.format("BidDTO{id=%d, auction='%s', buyer='%s', amount=%s, leading=%s}",
                id, auctionTitle, buyerUsername, amount, isLeading);
    }
}
