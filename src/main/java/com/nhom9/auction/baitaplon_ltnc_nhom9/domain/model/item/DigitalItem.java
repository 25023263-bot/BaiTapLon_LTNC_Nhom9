package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vật phẩm kỹ thuật số – giao hàng bằng key/link/file tải về.
 * Không có phí ship, giao tức thì sau thanh toán.
 */
public class DigitalItem extends AuctionItem {

    /** Loại nội dung: SOFTWARE_KEY, GIFT_CARD, EBOOK, GAME_CODE, v.v. */
    private String digitalType;

    /** Nền tảng hỗ trợ: Windows, Mac, Cross-platform, v.v. */
    private String platform;

    /** File size (MB), null nếu chỉ là key/code */
    private Double fileSizeMB;

    /** Ngày hết hạn của key/code, null = không hết hạn */
    private LocalDateTime expiryDate;

    /**
     * Nội dung bí mật (key, link tải) – chỉ được tiết lộ sau thanh toán.
     * Lưu mã hoá trong DB, chỉ hiển thị cho người thắng đấu giá.
     */
    private String deliveryContent;

    /** Có bảo hành thay thế nếu key lỗi không */
    private boolean replacementGuarantee;

    // ─── Constructor ────────────────────────────────────────────────────────

    public DigitalItem() { super(); }

    public DigitalItem(int id, int sellerId, String title, String description,
                       String category, BigDecimal startingPrice,
                       BigDecimal minBidIncrement, BigDecimal buyNowPrice,
                       LocalDateTime startTime, LocalDateTime endTime,
                       String digitalType, String platform, Double fileSizeMB,
                       LocalDateTime expiryDate, String deliveryContent,
                       boolean replacementGuarantee) {
        super(id, sellerId, title, description, category,
                startingPrice, minBidIncrement, buyNowPrice, startTime, endTime);
        this.digitalType          = digitalType;
        this.platform             = platform;
        this.fileSizeMB           = fileSizeMB;
        this.expiryDate           = expiryDate;
        this.deliveryContent      = deliveryContent;
        this.replacementGuarantee = replacementGuarantee;
    }

    // ─── Abstract Implementation ─────────────────────────────────────────────

    @Override
    public String getItemType() { return "DIGITAL"; }

    @Override
    public boolean isValidItem() {
        return title != null && !title.isBlank()
                && startingPrice != null && startingPrice.compareTo(BigDecimal.ZERO) > 0
                && digitalType != null && !digitalType.isBlank()
                && deliveryContent != null && !deliveryContent.isBlank()
                && startTime != null && endTime != null
                && endTime.isAfter(startTime);
    }

    // ─── Business Logic ──────────────────────────────────────────────────────

    /**
     * Key/code còn hiệu lực không.
     */
    public boolean isExpired() {
        return expiryDate != null && LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * Vật phẩm số luôn miễn phí ship.
     */
    public BigDecimal getShippingCost() {
        return BigDecimal.ZERO;
    }

    /**
     * Tổng chi phí người mua = chỉ giá thắng (không có phí ship).
     */
    public BigDecimal getTotalCostForBuyer() {
        return currentPrice;
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public String getDigitalType()                              { return digitalType; }
    public void setDigitalType(String digitalType)              { this.digitalType = digitalType; }

    public String getPlatform()                                 { return platform; }
    public void setPlatform(String platform)                    { this.platform = platform; }

    public Double getFileSizeMB()                               { return fileSizeMB; }
    public void setFileSizeMB(Double fileSizeMB)                { this.fileSizeMB = fileSizeMB; }

    public LocalDateTime getExpiryDate()                        { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate)         { this.expiryDate = expiryDate; }

    public String getDeliveryContent()                          { return deliveryContent; }
    public void setDeliveryContent(String deliveryContent)      { this.deliveryContent = deliveryContent; }

    public boolean isReplacementGuarantee()                     { return replacementGuarantee; }
    public void setReplacementGuarantee(boolean rg)             { this.replacementGuarantee = rg; }

    @Override
    public String toString() {
        return String.format("DigitalItem{id=%d, title='%s', type='%s', price=%s, status=%s}",
                id, title, digitalType, currentPrice, status);
    }
}