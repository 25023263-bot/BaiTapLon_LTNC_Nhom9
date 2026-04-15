package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object cho AuctionItem (cả Physical và Digital).
 * Kết hợp các trường của cả hai loại – null nếu không áp dụng.
 */
public class ItemDTO {

    private int id;
    private int sellerId;
    private String sellerUsername;    // join từ users
    private String sellerRating;      // hiển thị "4.5 ⭐ (12)"

    private String title;
    private String description;
    private String category;
    private String imageUrl;
    private String itemType;          // PHYSICAL | DIGITAL

    private BigDecimal startingPrice;
    private BigDecimal minBidIncrement;
    private BigDecimal buyNowPrice;
    private BigDecimal currentPrice;
    private int leadingBidderId;
    private String leadingBidderUsername;

    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;

    // Số lần bid
    private int totalBids;

    // ─── Physical-only ───────────────────────────────────────────────────────
    private String condition;
    private double weightGrams;
    private String dimensions;
    private String location;
    private BigDecimal shippingCost;
    private boolean allowPickup;

    // ─── Digital-only ────────────────────────────────────────────────────────
    private String digitalType;
    private String platform;
    private Double fileSizeMB;
    private LocalDateTime expiryDate;
    private boolean replacementGuarantee;
    // deliveryContent KHÔNG bao gồm ở đây vì bảo mật

    // ─── Constructor ────────────────────────────────────────────────────────

    public ItemDTO() {}

    // ─── Utility ─────────────────────────────────────────────────────────────

    public boolean isPhysical()    { return "PHYSICAL".equalsIgnoreCase(itemType); }
    public boolean isDigital()     { return "DIGITAL".equalsIgnoreCase(itemType); }
    public boolean isActive()      { return status == AuctionStatus.ACTIVE; }
    public boolean hasBuyNow()     { return buyNowPrice != null && buyNowPrice.compareTo(BigDecimal.ZERO) > 0; }
    public boolean hasBids()       { return totalBids > 0; }

    /**
     * Giá bid tối thiểu tiếp theo.
     */
    public BigDecimal getNextMinimumBid() {
        if (currentPrice == null || minBidIncrement == null) return startingPrice;
        return currentPrice.add(minBidIncrement);
    }

    /**
     * Số giây còn lại (tính toán client-side để tránh truyền server time).
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

    public String getSellerUsername()                           { return sellerUsername; }
    public void setSellerUsername(String sellerUsername)        { this.sellerUsername = sellerUsername; }

    public String getSellerRating()                             { return sellerRating; }
    public void setSellerRating(String sellerRating)            { this.sellerRating = sellerRating; }

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

    public BigDecimal getBuyNowPrice()                          { return buyNowPrice; }
    public void setBuyNowPrice(BigDecimal buyNowPrice)          { this.buyNowPrice = buyNowPrice; }

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

    public String getCondition()                                { return condition; }
    public void setCondition(String condition)                  { this.condition = condition; }

    public double getWeightGrams()                              { return weightGrams; }
    public void setWeightGrams(double weightGrams)              { this.weightGrams = weightGrams; }

    public String getDimensions()                               { return dimensions; }
    public void setDimensions(String dimensions)                { this.dimensions = dimensions; }

    public String getLocation()                                 { return location; }
    public void setLocation(String location)                    { this.location = location; }

    public BigDecimal getShippingCost()                         { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost)        { this.shippingCost = shippingCost; }

    public boolean isAllowPickup()                              { return allowPickup; }
    public void setAllowPickup(boolean allowPickup)             { this.allowPickup = allowPickup; }

    public String getDigitalType()                              { return digitalType; }
    public void setDigitalType(String digitalType)              { this.digitalType = digitalType; }

    public String getPlatform()                                 { return platform; }
    public void setPlatform(String platform)                    { this.platform = platform; }

    public Double getFileSizeMB()                               { return fileSizeMB; }
    public void setFileSizeMB(Double fileSizeMB)                { this.fileSizeMB = fileSizeMB; }

    public LocalDateTime getExpiryDate()                        { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate)         { this.expiryDate = expiryDate; }

    public boolean isReplacementGuarantee()                     { return replacementGuarantee; }
    public void setReplacementGuarantee(boolean rg)             { this.replacementGuarantee = rg; }

    @Override
    public String toString() {
        return String.format("ItemDTO{id=%d, title='%s', type=%s, price=%s, status=%s}",
                id, title, itemType, currentPrice, status);
    }
}