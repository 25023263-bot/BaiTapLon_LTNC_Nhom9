package org.example.baitaplon_ltnc_nhom9.domain.model.enums;

/**
 * Trạng thái của một phiên đấu giá.
 */
public enum AuctionStatus {

    /** Đã tạo nhưng chưa đến giờ bắt đầu */
    PENDING("Chờ mở"),

    /** Đang diễn ra, chấp nhận bid */
    ACTIVE("Đang đấu giá"),

    /** Đã kết thúc, có người thắng */
    CLOSED("Đã kết thúc"),

    /** Hết giờ mà không có bid nào */
    EXPIRED("Hết hạn – không có bid"),

    /** Người bán huỷ trước khi kết thúc */
    CANCELLED("Đã huỷ");

    private final String displayName;

    AuctionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}