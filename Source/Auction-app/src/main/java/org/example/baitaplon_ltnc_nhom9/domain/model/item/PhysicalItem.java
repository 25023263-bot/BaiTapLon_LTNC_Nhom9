package org.example.baitaplon_ltnc_nhom9.domain.model.item;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vật phẩm vật lý – cần giao hàng, có địa chỉ và phí ship.
 */
public class PhysicalItem extends AuctionItem {

    /** Tình trạng: NEW, LIKE_NEW, GOOD, FAIR, POOR */
    private String condition;

    /** Trọng lượng tính bằng gram */
    private double weightGrams;

    /** Kích thước (VD: "30x20x10 cm") */
    private String dimensions;

    /** Địa điểm đang lưu giữ vật phẩm */
    private String location;

    /** Phí vận chuyển cơ bản (0 = miễn phí) */
    private BigDecimal shippingCost;

    /** Có cho phép mua ngay và nhận hàng trực tiếp không */
    private boolean allowPickup;

    // ─── Constructor ────────────────────────────────────────────────────────

    public PhysicalItem() { super(); }

    public PhysicalItem(int id, int sellerId, String title, String description,
                        String category, BigDecimal startingPrice,
                        BigDecimal minBidIncrement, BigDecimal buyNowPrice,
                        LocalDateTime startTime, LocalDateTime endTime,
                        String condition, double weightGrams, String dimensions,
                        String location, BigDecimal shippingCost, boolean allowPickup) {
        super(id, sellerId, title, description, category,
                startingPrice, minBidIncrement, buyNowPrice, startTime, endTime);
        this.condition    = condition;
        this.weightGrams  = weightGrams;
        this.dimensions   = dimensions;
        this.location     = location;
        this.shippingCost = shippingCost != null ? shippingCost : BigDecimal.ZERO;
        this.allowPickup  = allowPickup;
    }

    // ─── Abstract Implementation ─────────────────────────────────────────────

    @Override
    public String getItemType() { return "PHYSICAL"; }

    @Override
    public boolean isValidItem() {
        return title != null && !title.isBlank()
                && startingPrice != null && startingPrice.compareTo(BigDecimal.ZERO) > 0
                && condition != null && !condition.isBlank()
                && startTime != null && endTime != null
                && endTime.isAfter(startTime);
    }

    // ─── Business Logic ──────────────────────────────────────────────────────

    /**
     * Tính tổng chi phí người mua phải trả (giá + ship).
     */
    public BigDecimal getTotalCostForBuyer() {
        return currentPrice.add(shippingCost != null ? shippingCost : BigDecimal.ZERO);
    }

    /**
     * Có miễn phí vận chuyển không.
     */
    public boolean isFreeShipping() {
        return shippingCost == null || shippingCost.compareTo(BigDecimal.ZERO) == 0;
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public String getCondition()                    { return condition; }
    public void setCondition(String condition)      { this.condition = condition; }

    public double getWeightGrams()                  { return weightGrams; }
    public void setWeightGrams(double weightGrams)  { this.weightGrams = weightGrams; }

    public String getDimensions()                   { return dimensions; }
    public void setDimensions(String dimensions)    { this.dimensions = dimensions; }

    public String getLocation()                     { return location; }
    public void setLocation(String location)        { this.location = location; }

    public BigDecimal getShippingCost()             { return shippingCost; }
    public void setShippingCost(BigDecimal cost)    { this.shippingCost = cost; }

    public boolean isAllowPickup()                  { return allowPickup; }
    public void setAllowPickup(boolean allowPickup) { this.allowPickup = allowPickup; }

    @Override
    public String toString() {
        return String.format("PhysicalItem{id=%d, title='%s', condition='%s', price=%s, status=%s}",
                id, title, condition, currentPrice, status);
    }
}