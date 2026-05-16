package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Giao dịch tài chính sau khi phiên đấu giá kết thúc thành công.
 *
 * Giữ đơn giản ở giai đoạn này: chỉ lưu thông tin cốt lõi.
 * Các trường như platformFee, sellerReceives, externalRef sẽ thêm lại
 * khi tích hợp cổng thanh toán thật (VNPay, Stripe, v.v.).
 */
public class Transaction {

    private int id;
    private int auctionId;
    private int buyerId;
    private int sellerId;

    /** Số tiền thắng đấu giá (chưa bao gồm phí ship) */
    private BigDecimal amount;

    /** Phương thức thanh toán: WALLET */
    private String paymentMethod;

    private PaymentStatus paymentStatus;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    // ─── Constructor ────────────────────────────────────────────────────────

    public Transaction() {}

    /**
     * Constructor tạo giao dịch mới khi phiên đấu giá kết thúc.
     */
    public Transaction(int auctionId, int buyerId, int sellerId,
                       BigDecimal amount, String paymentMethod) {
        this.auctionId     = auctionId;
        this.buyerId       = buyerId;
        this.sellerId      = sellerId;
        this.amount        = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = PaymentStatus.PENDING;
        this.createdAt     = LocalDateTime.now();
    }

    public Transaction(int id, int auctionId, int buyerId, int sellerId,
                       BigDecimal amount, String paymentMethod,
                       PaymentStatus paymentStatus,
                       LocalDateTime createdAt, LocalDateTime completedAt) {
        this.id            = id;
        this.auctionId     = auctionId;
        this.buyerId       = buyerId;
        this.sellerId      = sellerId;
        this.amount        = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.createdAt     = createdAt;
        this.completedAt   = completedAt;
    }

    // ─── Business Logic ──────────────────────────────────────────────────────

    public void markCompleted() {
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.completedAt   = LocalDateTime.now();
    }

    public void markFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
        this.completedAt   = LocalDateTime.now();
    }

    public void markRefunded() {
        this.paymentStatus = PaymentStatus.REFUNDED;
        this.completedAt   = LocalDateTime.now();
    }

    public boolean isCompleted() { return paymentStatus == PaymentStatus.COMPLETED; }
    public boolean isPending()   { return paymentStatus == PaymentStatus.PENDING; }
    public boolean isFailed()    { return paymentStatus == PaymentStatus.FAILED; }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public int getId()                                  { return id; }
    public void setId(int id)                           { this.id = id; }

    public int getAuctionId()                           { return auctionId; }
    public void setAuctionId(int auctionId)             { this.auctionId = auctionId; }

    public int getBuyerId()                             { return buyerId; }
    public void setBuyerId(int buyerId)                 { this.buyerId = buyerId; }

    public int getSellerId()                            { return sellerId; }
    public void setSellerId(int sellerId)               { this.sellerId = sellerId; }

    public BigDecimal getAmount()                       { return amount; }
    public void setAmount(BigDecimal amount)            { this.amount = amount; }

    public String getPaymentMethod()                    { return paymentMethod; }
    public void setPaymentMethod(String method)         { this.paymentMethod = method; }

    public PaymentStatus getPaymentStatus()             { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus status)  { this.paymentStatus = status; }

    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(LocalDateTime t)           { this.createdAt = t; }

    public LocalDateTime getCompletedAt()               { return completedAt; }
    public void setCompletedAt(LocalDateTime t)         { this.completedAt = t; }

    // ─── Object ──────────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        return id == ((Transaction) o).id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("Transaction{id=%d, auctionId=%d, amount=%s, status=%s}",
                id, auctionId, amount, paymentStatus);
    }
}
