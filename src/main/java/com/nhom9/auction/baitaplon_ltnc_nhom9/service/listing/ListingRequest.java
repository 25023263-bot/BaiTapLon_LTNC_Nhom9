package com.nhom9.auction.baitaplon_ltnc_nhom9.service.listing;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Dữ liệu form đăng bán — không phụ thuộc JavaFX. */
public record ListingRequest(
        int sellerId,
        String title,
        String category,
        String description,
        BigDecimal startingPrice,
        LocalDateTime endTime,
        String imagePath
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
