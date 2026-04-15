package org.example.baitaplon_ltnc_nhom9.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Data Transfer Object cho Bid.
 * Dùng để hiển thị lịch sử đấu giá trên UI.
 */
public class BidDTO {

    private int id;
    private int itemId;
    private String itemTitle;        // join từ auction_items
    private int bidderId;
    private String bidderUsername;
    private BigDecimal amount;
    private LocalDateTime bidTime;
    private boolean autoBid;
    private boolean isLeading;       // Có phải bid đang dẫn đầu không

    // ─── Constructor ────────────────────────────────────────────────────────

    public BidDTO() {}

    public BidDTO(int id, int itemId, String itemTitle, int bidderId,
                  String bidderUsername, BigDecimal amount, LocalDateTime bidTime,
                  boolean autoBid) {
        this.id              = id;
        this.itemId          = itemId;
        this.itemTitle       = itemTitle;
        this.bidderId        = bidderId;
        this.bidderUsername  = bidderUsername;
        this.amount          = amount;
        this.bidTime         = bidTime;
        this.autoBid         = autoBid;
        this.isLeading       = false;
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

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

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public int getItemId()                          { return itemId; }
    public void setItemId(int itemId)               { this.itemId = itemId; }

    public String getItemTitle()                    { return itemTitle; }
    public void setItemTitle(String itemTitle)      { this.itemTitle = itemTitle; }

    public int getBidderId()                        { return bidderId; }
    public void setBidderId(int bidderId)           { this.bidderId = bidderId; }

    public String getBidderUsername()               { return bidderUsername; }
    public void setBidderUsername(String name)      { this.bidderUsername = name; }

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
        return String.format("BidDTO{id=%d, item='%s', bidder='%s', amount=%s, leading=%s}",
                id, itemTitle, bidderUsername, amount, isLeading);
    }
}