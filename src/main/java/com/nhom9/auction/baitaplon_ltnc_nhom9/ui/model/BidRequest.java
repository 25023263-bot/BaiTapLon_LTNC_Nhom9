package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model;

/**
 * Kết quả trả về từ BidDialogController về ItemDetailCoordinator.
 *
 * Tại sao cần class này thay vì chỉ dùng Double?
 * → Dialog cần trả về 2 loại thông tin khác nhau:
 *   - Bid thủ công: chỉ cần 1 số (amount)
 *   - Auto-bid    : cần biết đây là auto-bid VÀ limit tối đa là bao nhiêu
 *
 * Nếu chỉ dùng Double, coordinator không có cách nào phân biệt được
 * "người dùng đặt 1.000.000đ thủ công" hay "người dùng đặt auto-bid limit 1.000.000đ".
 * → Kết quả: coordinator cứ gọi placeBid() → giá tối đa bị đặt thẳng luôn.
 *
 * Dùng record (Java 16+) vì đây là data class thuần túy:
 *   - Không có logic
 *   - Bất biến (immutable)
 *   - Constructor + getter được tự sinh
 *
 * Cách dùng:
 *   BidRequest.manual(500_000)           → bid thủ công 500.000đ
 *   BidRequest.auto(1_000_000)           → auto-bid với limit 1.000.000đ
 *
 *   request.isAuto()                     → true nếu là auto-bid
 *   request.amount()                     → số tiền (limit nếu auto, amount nếu thủ công)
 */
public record BidRequest(double amount, boolean isAuto) {

    /** Tạo bid thủ công với số tiền cụ thể */
    public static BidRequest manual(double amount) {
        return new BidRequest(amount, false);
    }

    /** Tạo auto-bid với giới hạn tối đa */
    public static BidRequest auto(double maxLimit) {
        return new BidRequest(maxLimit, true);
    }
}