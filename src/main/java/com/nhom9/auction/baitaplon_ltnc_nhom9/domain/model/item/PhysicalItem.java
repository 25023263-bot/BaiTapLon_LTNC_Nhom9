package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vật phẩm vật lý trong phiên đấu giá.
 *
 * Các trường physical-only (condition, weightGrams, dimensions, location,
 * shippingCost, allowPickup) đã được xoá vì UI hiện tại không thu thập
 * và không hiển thị những thông tin này.
 */
public class PhysicalItem extends AuctionItem {

    // ─── Constructor ────────────────────────────────────────────────────────

    public PhysicalItem() { super(); }

    public PhysicalItem(int id, int sellerId, String title, String description,
                        String category, BigDecimal startingPrice,
                        BigDecimal minBidIncrement,
                        LocalDateTime startTime, LocalDateTime endTime) {
        super(id, sellerId, title, description, category,
                startingPrice, minBidIncrement, startTime, endTime);
    }

    // ─── Abstract Implementation ─────────────────────────────────────────────

    @Override
    public String getItemType() { return "PHYSICAL"; }

    @Override
    public boolean isValidItem() {
        return title != null && !title.isBlank()
                && startingPrice != null && startingPrice.compareTo(BigDecimal.ZERO) > 0
                && startTime != null && endTime != null
                && endTime.isAfter(startTime);
    }

    @Override
    public String toString() {
        return String.format("PhysicalItem{id=%d, title='%s', price=%s, status=%s}",
                id, title, currentPrice, status);
    }
}
