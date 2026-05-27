package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.InsufficientBalanceException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Người mua – có ví tiền, có thể đấu giá và xem watchlist.
 */
public class Buyer extends User {

    /** Số dư ví (VND hoặc đơn vị tiền tệ nội bộ) */
    private BigDecimal walletBalance;

    // ─── Constructor ────────────────────────────────────────────────────────

    public Buyer() {
        super();
        this.role          = UserRole.BUYER;
        this.walletBalance = BigDecimal.ZERO;
    }

    public Buyer(int id, String username, String email, String passwordHash,
                 String fullName, String phone) {
        super(id, username, email, passwordHash, fullName, phone, UserRole.BUYER);
        this.walletBalance = BigDecimal.ZERO;
    }

    public Buyer(int id, String username, String email, String passwordHash,
                 String fullName, String phone, BigDecimal walletBalance,
                 boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, username, email, passwordHash, fullName, phone);
        this.walletBalance = walletBalance != null ? walletBalance : BigDecimal.ZERO;
        this.active        = active;
        this.createdAt     = createdAt;
        this.updatedAt     = updatedAt;
    }

    // ─── Business Logic ──────────────────────────────────────────────────────

    /**
     * Nạp tiền vào ví.
     * @param amount số tiền nạp (phải > 0)
     */
    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0.");
        walletBalance = walletBalance.add(amount);
        updatedAt     = LocalDateTime.now();
    }

    /**
     * Trừ tiền từ ví khi thanh toán.
     * @param amount số tiền cần trừ
     * @throws InsufficientBalanceException nếu số dư không đủ
     */
    public void deduct(BigDecimal amount) throws InsufficientBalanceException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Số tiền trừ phải lớn hơn 0.");
        if (walletBalance.compareTo(amount) < 0)
            throw new InsufficientBalanceException(
                    "Số dư ví không đủ. Hiện có: " + walletBalance + ", cần: " + amount);
        walletBalance = walletBalance.subtract(amount);
        updatedAt     = LocalDateTime.now();
    }

    /**
     * Kiểm tra ví có đủ tiền không.
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return walletBalance.compareTo(amount) >= 0;
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public BigDecimal getWalletBalance()             { return walletBalance; }
    public void setWalletBalance(BigDecimal balance) { this.walletBalance = balance; }

    @Override
    public String toString() {
        return String.format("Buyer{id=%d, username='%s', balance=%s}",
                id, username, walletBalance);
    }
}
