package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auction;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Dữ liệu được gửi kèm khi một phiên đấu giá kết thúc.
 *
 * Tại sao cần class này?
 * ─────────────────────
 * Khi phiên kết thúc, server cần push 2 loại thông tin khác nhau về client:
 *
 *   1. Thông tin chung (ai cũng cần thấy):
 *      - Phiên #X đã kết thúc, người thắng là ai, giá cuối bao nhiêu
 *
 *   2. Thông tin cá nhân (chỉ buyer/seller liên quan):
 *      - Buyer: số dư ví mới sau khi bị trừ tiền
 *      - Seller: số earnings mới sau khi được cộng tiền
 *
 * Bằng cách gói tất cả vào AuctionClosedEvent (Serializable), server có thể
 * gửi một object duy nhất qua socket. Client nhận rồi tự kiểm tra:
 *   - winnerId == myId  → cập nhật walletBalance
 *   - sellerId == myId  → cập nhật earningsBalance
 *
 * Nhờ vậy số dư được cập nhật ngay khi phiên đóng, không cần đăng nhập lại.
 */
public class AuctionClosedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final AuctionItem item;

    /** null nếu phiên hết hạn không có bid */
    private final Integer winnerId;

    /** null nếu phiên hết hạn không có bid */
    private final BigDecimal buyerNewBalance;

    /** Luôn có (seller luôn tồn tại) */
    private final BigDecimal sellerNewBalance;

    /** ID của seller (để client biết mình có phải seller không) */
    private final int sellerId;

    public AuctionClosedEvent(AuctionItem item,
                              Integer winnerId,
                              BigDecimal buyerNewBalance,
                              int sellerId,
                              BigDecimal sellerNewBalance) {
        this.item             = item;
        this.winnerId         = winnerId;
        this.buyerNewBalance  = buyerNewBalance;
        this.sellerId         = sellerId;
        this.sellerNewBalance = sellerNewBalance;
    }

    public AuctionItem   getItem()             { return item; }
    public Integer       getWinnerId()         { return winnerId; }
    public BigDecimal    getBuyerNewBalance()  { return buyerNewBalance; }
    public int           getSellerId()         { return sellerId; }
    public BigDecimal    getSellerNewBalance() { return sellerNewBalance; }

    /** Tiện ích: phiên có người thắng không? */
    public boolean hasWinner() { return winnerId != null; }
}