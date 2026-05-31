package com.nhom9.auction.baitaplon_ltnc_nhom9.service.listing;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.PhysicalItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.AuctionHouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tạo phiên đấu giá mới từ form người bán — gọi {@link AuctionHouse#listItem}.
 */
public final class ListingService {

    private final AuctionHouse auctionHouse;

    public ListingService(AuctionHouse auctionHouse) {
        this.auctionHouse = auctionHouse;
    }

    public PhysicalItem createListing(ListingRequest req) throws Exception {
        PhysicalItem item = new PhysicalItem();
        item.setSellerId(req.sellerId());
        item.setTitle(req.title());
        item.setCategory(stripCategoryPrefix(req.category()));
        item.setDescription(req.description());
        item.setImageUrl(req.imagePath() != null ? req.imagePath() : "");
        item.setStartingPrice(req.startingPrice());
        item.setCurrentPrice(req.startingPrice());
        item.setMinBidIncrement(new BigDecimal("1000"));
        item.setStartTime(LocalDateTime.now());
        item.setEndTime(req.endTime());

        return (PhysicalItem) auctionHouse.listItem(item);
    }

    /**
     * Bỏ emoji đầu chuỗi danh mục do UI gửi lên (vd. "⌚ Đồng hồ" → "Đồng hồ").
     * Logic này thuộc service vì server cần lưu tên category sạch vào DB,
     * không phụ thuộc vào UI layer.
     */
    private static String stripCategoryPrefix(String category) {
        if (category == null || category.isBlank()) return "";
        int space = category.indexOf(' ');
        if (space > 0 && space < category.length() - 1) {
            return category.substring(space + 1).trim();
        }
        return category.trim();
    }
}