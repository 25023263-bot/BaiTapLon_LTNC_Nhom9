package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.AuctionClosedException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.BidTooLowException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.InsufficientBalanceException;

import java.math.BigDecimal;

/**
 * Hợp đồng cho các hành động trong phiên đấu giá.
 */
public interface Auctionable {

    /**
     * Đặt bid thủ công.
     * @return Bid đã được lưu
     */
    Bid placeBid(int itemId, int bidderId, BigDecimal amount)
            throws AuctionClosedException, BidTooLowException, InsufficientBalanceException, Exception;

    /**
     * Đặt auto-bid với giới hạn tối đa.
     */
    Bid placeAutoBid(int itemId, int bidderId, BigDecimal maxLimit)
            throws AuctionClosedException, BidTooLowException, InsufficientBalanceException, Exception;

    /**
     * Mua ngay theo giá Buy-Now.
     */
    void buyNow(int itemId, int buyerId)
            throws AuctionClosedException, InsufficientBalanceException, Exception;

    /**
     * Kết thúc phiên đấu giá (gọi khi hết giờ).
     */
    void closeAuction(int itemId) throws Exception;

    /**
     * Huỷ phiên đấu giá (chỉ khi chưa có bid).
     */
    void cancelAuction(int itemId, int sellerId) throws Exception;

    /**
     * Đăng vật phẩm mới lên đấu giá.
     */
    AuctionItem listItem(AuctionItem item) throws Exception;
}