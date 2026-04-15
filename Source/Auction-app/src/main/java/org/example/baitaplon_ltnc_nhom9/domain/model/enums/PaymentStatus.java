package org.example.baitaplon_ltnc_nhom9.domain.model.enums;

/**
 * Trạng thái thanh toán của một giao dịch.
 */
public enum PaymentStatus {

    /** Chờ xác nhận thanh toán */
    PENDING("Chờ thanh toán"),

    /** Đã thanh toán thành công */
    COMPLETED("Hoàn thành"),

    /** Thanh toán thất bại */
    FAILED("Thất bại"),

    /** Đã hoàn tiền cho người mua */
    REFUNDED("Đã hoàn tiền");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == REFUNDED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}