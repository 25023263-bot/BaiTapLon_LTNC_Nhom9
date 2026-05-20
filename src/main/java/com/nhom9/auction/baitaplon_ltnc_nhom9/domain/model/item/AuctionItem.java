package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.io.Serializable;

/**
 * Aggregate root for a seller listing ({@code auctions} row + physical detail).
 * Concrete subtype: {@link PhysicalItem}.
 */
public abstract class AuctionItem implements Serializable {

    private static final long serialVersionUID = 1L;

    protected int id;
    protected int sellerId;
    protected String title;
    protected String description;
    protected String category;
    protected String imageUrl;

    /** Giá khởi điểm */
    protected BigDecimal startingPrice;

    /** Giá bid tối thiểu mỗi lần tăng */
    protected BigDecimal minBidIncrement;

    /** Giá bid cao nhất hiện tại */
    protected BigDecimal currentPrice;

    /** ID người đang dẫn đầu bid (0 = chưa có bid) */
    protected int leadingBidderId;

    protected AuctionStatus status;
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;
    protected LocalDateTime createdAt;

    // ─── Constructor ────────────────────────────────────────────────────────

    protected AuctionItem() {}

    protected AuctionItem(int id, int sellerId, String title, String description,
                          String category, BigDecimal startingPrice,
                          BigDecimal minBidIncrement,
                          LocalDateTime startTime, LocalDateTime endTime) {
        this.id               = id;
        this.sellerId         = sellerId;
        this.title            = title;
        this.description      = description;
        this.category         = category;
        this.startingPrice    = startingPrice;
        this.minBidIncrement  = minBidIncrement != null ? minBidIncrement : BigDecimal.ONE;
        this.currentPrice     = startingPrice;
        this.leadingBidderId  = 0;
        this.status           = AuctionStatus.PENDING;
        this.startTime        = startTime;
        this.endTime          = endTime;
        this.createdAt        = LocalDateTime.now();
    }

    // ─── Abstract ────────────────────────────────────────────────────────────

    /** Trả về loại vật phẩm (PHYSICAL / DIGITAL) */
    public abstract String getItemType();

    /** Xác thực dữ liệu đặc thù của loại vật phẩm */
    public abstract boolean isValidItem();

    // ─── Business Logic ──────────────────────────────────────────────────────

    /**
     * Kiểm tra bid mới có hợp lệ không.
     * @param bidAmount số tiền đặt
     * @return true nếu hợp lệ
     */
    public boolean isValidBid(BigDecimal bidAmount) {
        if (bidAmount == null) return false;
        BigDecimal minimum = currentPrice.add(minBidIncrement);
        return bidAmount.compareTo(minimum) >= 0;
    }

    /**
     * Cập nhật giá hiện tại khi có bid mới thắng.
     */
    public void updateCurrentBid(BigDecimal newBid, int bidderId) {
        this.currentPrice    = newBid;
        this.leadingBidderId = bidderId;
    }

    /**
     * Tính giá bid tối thiểu tiếp theo.
     */
    public BigDecimal getNextMinimumBid() {
        return currentPrice.add(minBidIncrement);
    }

    /**
     * Số giây còn lại của phiên đấu giá.
     */
    public long getRemainingSeconds() {
        if (endTime == null) return 0;
        long diff = ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);
        return Math.max(0, diff);
    }

    /**
     * Phiên đấu giá đã có bid chưa.
     */
    public boolean hasBids() {
        return leadingBidderId > 0 && currentPrice.compareTo(startingPrice) > 0;
    }

    /**
     * Phiên đấu giá đang active không.
     */
    public boolean isActive() {
        return status == AuctionStatus.ACTIVE;
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public int getSellerId()                        { return sellerId; }
    public void setSellerId(int sellerId)           { this.sellerId = sellerId; }

    public String getTitle()                        { return title; }
    public void setTitle(String title)              { this.title = title; }

    public String getDescription()                  { return description; }
    public void setDescription(String desc)         { this.description = desc; }

    public String getCategory()                     { return category; }
    public void setCategory(String category)        { this.category = category; }

    public String getImageUrl()                     { return imageUrl; }
    public void setImageUrl(String imageUrl)        { this.imageUrl = imageUrl; }

    public BigDecimal getStartingPrice()            { return startingPrice; }
    public void setStartingPrice(BigDecimal p)      { this.startingPrice = p; }

    public BigDecimal getMinBidIncrement()          { return minBidIncrement; }
    public void setMinBidIncrement(BigDecimal inc)  { this.minBidIncrement = inc; }

    public BigDecimal getCurrentPrice()             { return currentPrice; }
    public void setCurrentPrice(BigDecimal p)       { this.currentPrice = p; }

    public int getLeadingBidderId()                 { return leadingBidderId; }
    public void setLeadingBidderId(int id)          { this.leadingBidderId = id; }

    public AuctionStatus getStatus()                { return status; }
    public void setStatus(AuctionStatus status)     { this.status = status; }

    public LocalDateTime getStartTime()             { return startTime; }
    public void setStartTime(LocalDateTime t)       { this.startTime = t; }

    public LocalDateTime getEndTime()               { return endTime; }
    public void setEndTime(LocalDateTime t)         { this.endTime = t; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime t)       { this.createdAt = t; }

    // ─── Object ──────────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuctionItem)) return false;
        return id == ((AuctionItem) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("AuctionItem{id=%d, title='%s', status=%s, currentPrice=%s}",
                id, title, status, currentPrice);
    }
}