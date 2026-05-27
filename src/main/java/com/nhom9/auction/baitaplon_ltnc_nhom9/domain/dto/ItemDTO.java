package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.List;

/**
 * Data Transfer Object cho AuctionItem — dùng để truyền dữ liệu qua socket.
 *
 * Các trường physical-only (condition, weightGrams, dimensions, location,
 * shippingCost, allowPickup) đã được xoá vì UI không hiển thị những
 * thông tin này và server không map chúng vào DTO.
 */
public class ItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int sellerId;

    private String title;
    private String description;
    private String category;
    private String imageUrl;
    private String itemType;

    private BigDecimal startingPrice;
    private BigDecimal minBidIncrement;
    private BigDecimal currentPrice;
    private int leadingBidderId;
    private String leadingBidderUsername;

    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;

    private int totalBids;

    /** Chỉ populate trong GET_AUCTION_DETAIL, null trong GET_AUCTIONS */
    private List<BidDTO> bids;

    // ─── Constructor ────────────────────────────────────────────────────────

    public ItemDTO() {}

    // ─── Utility ─────────────────────────────────────────────────────────────

    public boolean isActive()  { return status == AuctionStatus.ACTIVE; }
    public boolean hasBids()   { return totalBids > 0; }

    /**
     * Giá bid tối thiểu tiếp theo.
     */
    public BigDecimal getNextMinimumBid() {
        if (currentPrice == null || minBidIncrement == null) return startingPrice;
        return currentPrice.add(minBidIncrement);
    }

    /**
     * Số giây còn lại (tính client-side).
     */
    public long getRemainingSeconds() {
        if (endTime == null) return 0;
        long diff = java.time.temporal.ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);
        return Math.max(0, diff);
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public int getId()                                          { return id; }
    public void setId(int id)                                   { this.id = id; }

    public int getSellerId()                                    { return sellerId; }
    public void setSellerId(int sellerId)                       { this.sellerId = sellerId; }

    public String getTitle()                                    { return title; }
    public void setTitle(String title)                          { this.title = title; }

    public String getDescription()                              { return description; }
    public void setDescription(String description)              { this.description = description; }

    public String getCategory()                                 { return category; }
    public void setCategory(String category)                    { this.category = category; }

    public String getImageUrl()                                 { return imageUrl; }
    public void setImageUrl(String imageUrl)                    { this.imageUrl = imageUrl; }

    public String getItemType()                                 { return itemType; }
    public void setItemType(String itemType)                    { this.itemType = itemType; }

    public BigDecimal getStartingPrice()                        { return startingPrice; }
    public void setStartingPrice(BigDecimal startingPrice)      { this.startingPrice = startingPrice; }

    public BigDecimal getMinBidIncrement()                      { return minBidIncrement; }
    public void setMinBidIncrement(BigDecimal minBidIncrement)  { this.minBidIncrement = minBidIncrement; }

    public BigDecimal getCurrentPrice()                         { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice)        { this.currentPrice = currentPrice; }

    public int getLeadingBidderId()                             { return leadingBidderId; }
    public void setLeadingBidderId(int id)                      { this.leadingBidderId = id; }

    public String getLeadingBidderUsername()                    { return leadingBidderUsername; }
    public void setLeadingBidderUsername(String name)           { this.leadingBidderUsername = name; }

    public AuctionStatus getStatus()                            { return status; }
    public void setStatus(AuctionStatus status)                 { this.status = status; }

    public LocalDateTime getStartTime()                         { return startTime; }
    public void setStartTime(LocalDateTime startTime)           { this.startTime = startTime; }

    public LocalDateTime getEndTime()                           { return endTime; }
    public void setEndTime(LocalDateTime endTime)               { this.endTime = endTime; }

    public LocalDateTime getCreatedAt()                         { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)           { this.createdAt = createdAt; }

    public int getTotalBids()                                   { return totalBids; }
    public void setTotalBids(int totalBids)                     { this.totalBids = totalBids; }

    public List<BidDTO> getBids()                               { return bids; }
    public void setBids(List<BidDTO> bids)                      { this.bids = bids; }

    @Override
    public String toString() {
        return String.format("ItemDTO{id=%d, title='%s', price=%s, status=%s}",
                id, title, currentPrice, status);
    }
}
