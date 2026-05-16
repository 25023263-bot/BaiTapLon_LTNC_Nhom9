package com.nhom9.auction.baitaplon_ltnc_nhom9.service.listing;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.PhysicalItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction.AuctionHouse;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.mapper.AuctionCardMapper;

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
        item.setCategory(AuctionCardMapper.stripCategoryPrefix(req.category()));
        item.setDescription(req.description());
        item.setImageUrl(req.imagePath() != null ? req.imagePath() : "");
        item.setStartingPrice(req.startingPrice());
        item.setCurrentPrice(req.startingPrice());
        item.setMinBidIncrement(new BigDecimal("1000"));
        item.setBuyNowPrice(null);
        item.setStartTime(LocalDateTime.now());
        item.setEndTime(req.endTime());
        item.setCondition("GOOD");
        item.setWeightGrams(0);
        item.setDimensions("");
        item.setLocation("Việt Nam");
        item.setShippingCost(BigDecimal.ZERO);
        item.setAllowPickup(false);

        return (PhysicalItem) auctionHouse.listItem(item);
    }
}
