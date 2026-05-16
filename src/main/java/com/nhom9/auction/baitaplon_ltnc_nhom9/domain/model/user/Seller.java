package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Người bán – có thể đăng vật phẩm đấu giá và nhận tiền khi bán thành công.
 */
public class Seller extends User {

    /** Số dư nhận được từ các phiên đấu giá thành công */
    private BigDecimal earningsBalance;

    // ─── Constructor ────────────────────────────────────────────────────────

    public Seller() {
        super();
        this.role            = UserRole.SELLER;
        this.earningsBalance = BigDecimal.ZERO;
    }

    public Seller(int id, String username, String email, String passwordHash,
                  String fullName, String phone) {
        super(id, username, email, passwordHash, fullName, phone, UserRole.SELLER);
        this.earningsBalance = BigDecimal.ZERO;
    }

    public Seller(int id, String username, String email, String passwordHash,
                  String fullName, String phone, BigDecimal earningsBalance,
                  boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, username, email, passwordHash, fullName, phone);
        this.earningsBalance = earningsBalance != null ? earningsBalance : BigDecimal.ZERO;
        this.active          = active;
        this.createdAt       = createdAt;
        this.updatedAt       = updatedAt;
    }

    // ─── Business Logic ──────────────────────────────────────────────────────

    /**
     * Cộng tiền vào tài khoản khi phiên đấu giá kết thúc thành công.
     */
    public void receivePayment(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Số tiền nhận phải lớn hơn 0.");
        earningsBalance = earningsBalance.add(amount);
        updatedAt = LocalDateTime.now();
    }

    /**
     * Rút tiền từ tài khoản (withdraw).
     */
    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Số tiền rút phải lớn hơn 0.");
        if (earningsBalance.compareTo(amount) < 0)
            throw new IllegalStateException("Số dư không đủ để rút: " + amount);
        earningsBalance = earningsBalance.subtract(amount);
        updatedAt       = LocalDateTime.now();
    }

    @Override
    public String getRoleDescription() {
        return "Người bán – có thể đăng vật phẩm, quản lý phiên đấu giá.";
    }

    /**
     * Tạo một Buyer tạm thời đại diện cho Seller khi tham gia đấu giá.
     *
     * <p>Tại sao cần method này?</p>
     * <p>Hệ thống dùng kế thừa để phân biệt vai trò (Seller/Buyer extends User),
     * nên một Seller không thể tự nhiên đặt bid vì {@code AuctionHouse.loadBuyer()}
     * yêu cầu đối tượng phải là {@code Buyer}. Method này tạo ra một Buyer
     * "proxy" mang đầy đủ thông tin của Seller, dùng {@code earningsBalance}
     * làm ví để kiểm tra số dư khi đặt bid.</p>
     *
     * @return Buyer proxy mang thông tin của Seller này
     */
    public Buyer asBuyer() {
        Buyer proxy = new Buyer();
        proxy.setId(this.id);
        proxy.setUsername(this.username);
        proxy.setEmail(this.email);
        proxy.setPasswordHash(this.passwordHash);
        proxy.setFullName(this.fullName);
        proxy.setPhone(this.phone);
        proxy.setActive(this.active);
        proxy.setCreatedAt(this.createdAt);
        proxy.setUpdatedAt(this.updatedAt);
        proxy.setWalletBalance(this.earningsBalance != null ? this.earningsBalance : BigDecimal.ZERO);
        return proxy;
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public BigDecimal getEarningsBalance()             { return earningsBalance; }
    public void setEarningsBalance(BigDecimal balance) { this.earningsBalance = balance; }

    @Override
    public String toString() {
        return String.format("Seller{id=%d, username='%s', earnings=%s}",
                id, username, earningsBalance);
    }
}
