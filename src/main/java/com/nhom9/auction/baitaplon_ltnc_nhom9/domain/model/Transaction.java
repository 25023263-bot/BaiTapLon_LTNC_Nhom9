package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
// Transaction — tính platformFee, sellerReceives, markCompleted/Failed/Refunded
/**
 * Giao dịch tài chính sau khi phiên đấu giá kết thúc thành công.
 */
public class Transaction {

    private int id;
    private int itemId;
    private int buyerId;
    private int sellerId;

    /** Số tiền thanh toán (giá thắng đấu giá) */
    private BigDecimal amount;

    /** Phí vận chuyển (0 với vật phẩm số) */
    private BigDecimal shippingFee;

    /** Phí nền tảng (VD: 2% hoa hồng) */
    private BigDecimal platformFee;

    /** Tổng số tiền buyer trả = amount + shippingFee */
    private BigDecimal totalPaid;

    /** Số tiền seller nhận = amount - platformFee */
    private BigDecimal sellerReceives;

    private PaymentStatus paymentStatus;

    /** Phương thức thanh toán: WALLET, CREDIT_CARD */
    private String paymentMethod;

    /** Mã tham chiếu giao dịch bên ngoài (nếu có) */
    private String externalRef;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    // ─── Constructor ────────────────────────────────────────────────────────

    public Transaction() {}

    /**
     * Constructor tạo giao dịch mới khi phiên đấu giá kết thúc.
     */
    public Transaction(int itemId, int buyerId, int sellerId,
                       BigDecimal amount, BigDecimal shippingFee,
                       double platformFeeRate, String paymentMethod) {
        this.itemId        = itemId;
        this.buyerId       = buyerId;
        this.sellerId      = sellerId;
        this.amount        = amount;
        this.shippingFee   = shippingFee != null ? shippingFee : BigDecimal.ZERO;
        this.platformFee   = amount.multiply(BigDecimal.valueOf(platformFeeRate));
        this.totalPaid     = amount.add(this.shippingFee);
        this.sellerReceives = amount.subtract(this.platformFee);
        this.paymentMethod = paymentMethod;
        this.paymentStatus = PaymentStatus.PENDING;
        this.createdAt     = LocalDateTime.now();
    }

    public Transaction(int id, int itemId, int buyerId, int sellerId,
                       BigDecimal amount, BigDecimal shippingFee, BigDecimal platformFee,
                       BigDecimal totalPaid, BigDecimal sellerReceives,
                       PaymentStatus paymentStatus, String paymentMethod,
                       String externalRef, LocalDateTime createdAt, LocalDateTime completedAt) {
        this.id              = id;
        this.itemId          = itemId;
        this.buyerId         = buyerId;
        this.sellerId        = sellerId;
        this.amount          = amount;
        this.shippingFee     = shippingFee;
        this.platformFee     = platformFee;
        this.totalPaid       = totalPaid;
        this.sellerReceives  = sellerReceives;
        this.paymentStatus   = paymentStatus;
        this.paymentMethod   = paymentMethod;
        this.externalRef     = externalRef;
        this.createdAt       = createdAt;
        this.completedAt     = completedAt;
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

    public int getItemId()                              { return itemId; }
    public void setItemId(int itemId)                   { this.itemId = itemId; }

    public int getBuyerId()                             { return buyerId; }
    public void setBuyerId(int buyerId)                 { this.buyerId = buyerId; }

    public int getSellerId()                            { return sellerId; }
    public void setSellerId(int sellerId)               { this.sellerId = sellerId; }

    public BigDecimal getAmount()                       { return amount; }
    public void setAmount(BigDecimal amount)            { this.amount = amount; }

    public BigDecimal getShippingFee()                  { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee)  { this.shippingFee = shippingFee; }

    public BigDecimal getPlatformFee()                  { return platformFee; }
    public void setPlatformFee(BigDecimal platformFee)  { this.platformFee = platformFee; }

    public BigDecimal getTotalPaid()                    { return totalPaid; }
    public void setTotalPaid(BigDecimal totalPaid)      { this.totalPaid = totalPaid; }

    public BigDecimal getSellerReceives()               { return sellerReceives; }
    public void setSellerReceives(BigDecimal amount)    { this.sellerReceives = amount; }

    public PaymentStatus getPaymentStatus()             { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus status)  { this.paymentStatus = status; }

    public String getPaymentMethod()                    { return paymentMethod; }
    public void setPaymentMethod(String method)         { this.paymentMethod = method; }

    public String getExternalRef()                      { return externalRef; }
    public void setExternalRef(String ref)              { this.externalRef = ref; }

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
        return String.format("Transaction{id=%d, itemId=%d, amount=%s, status=%s}",
                id, itemId, totalPaid, paymentStatus);
    }
}