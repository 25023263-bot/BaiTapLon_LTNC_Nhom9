package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model;

import java.time.LocalDateTime;

/**
 * View model cho thẻ sản phẩm / màn chi tiết — tách khỏi domain {@code AuctionItem}.
 */
public record AuctionCardModel(
        String id,
        String title,
        String category,
        String categoryEmoji,
        double currentBid,
        double startingPrice,
        String description,
        int bidCount,
        boolean isLive,
        LocalDateTime endTime,
        String imagePlaceholderEmoji,
        String imageUrl,
        int sellerId
) {}
