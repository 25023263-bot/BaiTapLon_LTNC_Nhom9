package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
// Bid — auto-bid logic với canAutoBidUp và calculateAutoBidAmount
/**
 * Đại diện cho một lần đặt giá (bid) trong phiên đấu giá.
 */
public class Bid {

    private int id;
    private int itemId;
    private int bidderId;

    /** Tên người đặt bid (dùng để hiển thị, không cần join thêm) */
    private String bidderUsername;

    /** Số tiền đặt */
    private BigDecimal amount;

    /** Thời điểm đặt bid */
    private LocalDateTime bidTime;

    /** Bid này có phải là bid tự động (auto-bid) không */
    private boolean autoBid;

    /** Giới hạn tối đa nếu là auto-bid */
    private BigDecimal autoBidLimit;

    // ─── Constructor ────────────────────────────────────────────────────────

    public Bid() {}

    public Bid(int itemId, int bidderId, BigDecimal amount) {
        this.itemId   = itemId;
        this.bidderId = bidderId;
        this.amount   = amount;
        this.bidTime  = LocalDateTime.now();
        this.autoBid  = false;
    }

    public Bid(int id, int itemId, int bidderId, String bidderUsername,
               BigDecimal amount, LocalDateTime bidTime, boolean autoBid, BigDecimal autoBidLimit) {
        this.id              = id;
        this.itemId          = itemId;
        this.bidderId        = bidderId;
        this.bidderUsername  = bidderUsername;
        this.amount          = amount;
        this.bidTime         = bidTime;
        this.autoBid         = autoBid;
        this.autoBidLimit    = autoBidLimit;
    }

    // ─── Business Logic ──────────────────────────────────────────────────────

    /**
     * Bid này có thể tự động tăng lên mức mới không.
     * @param newAmount mức giá mới cần vượt qua
     */
    public boolean canAutoBidUp(BigDecimal newAmount) {
        return autoBid && autoBidLimit != null && autoBidLimit.compareTo(newAmount) >= 0;
    }

    /**
     * Tính toán mức auto-bid tiếp theo (không vượt giới hạn).
     * @param increment mức tăng tối thiểu
     * @param rivalBid  bid của đối thủ cần vượt
     * @return số tiền auto-bid mới, hoặc null nếu không thể
     */
    public BigDecimal calculateAutoBidAmount(BigDecimal increment, BigDecimal rivalBid) {
        if (!autoBid || autoBidLimit == null) return null;
        BigDecimal needed = rivalBid.add(increment);
        if (autoBidLimit.compareTo(needed) < 0) return null;
        return needed.min(autoBidLimit);
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public int getItemId()                          { return itemId; }
    public void setItemId(int itemId)               { this.itemId = itemId; }

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

    public BigDecimal getAutoBidLimit()             { return autoBidLimit; }
    public void setAutoBidLimit(BigDecimal limit)   { this.autoBidLimit = limit; }

    // ─── Object ──────────────────────────────────────────────────────────────

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
        return String.format("Bid{id=%d, itemId=%d, bidder='%s', amount=%s, time=%s}",
                id, itemId, bidderUsername, amount, bidTime);
    }
}