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

    /** Tổng số vật phẩm đã bán thành công */
    private int totalSold;

    /** Điểm đánh giá trung bình (1.0 – 5.0) */
    private double rating;

    /** Số lượng đánh giá */
    private int ratingCount;

    // ─── Constructor ────────────────────────────────────────────────────────

    public Seller() {
        super();
        this.role            = UserRole.SELLER;
        this.earningsBalance = BigDecimal.ZERO;
        this.totalSold       = 0;
        this.rating          = 0.0;
        this.ratingCount     = 0;
    }

    public Seller(int id, String username, String email, String passwordHash,
                  String fullName, String phone) {
        super(id, username, email, passwordHash, fullName, phone, UserRole.SELLER);
        this.earningsBalance = BigDecimal.ZERO;
        this.totalSold       = 0;
        this.rating          = 0.0;
        this.ratingCount     = 0;
    }

    public Seller(int id, String username, String email, String passwordHash,
                  String fullName, String phone, BigDecimal earningsBalance,
                  int totalSold, double rating, int ratingCount,
                  boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, username, email, passwordHash, fullName, phone);
        this.earningsBalance = earningsBalance != null ? earningsBalance : BigDecimal.ZERO;
        this.totalSold       = totalSold;
        this.rating          = rating;
        this.ratingCount     = ratingCount;
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
        totalSold++;
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

    /**
     * Thêm một đánh giá mới và tính lại điểm trung bình.
     * @param score điểm từ 1 đến 5
     */
    public void addRating(int score) {
        if (score < 1 || score > 5)
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5.");
        // Tính lại trung bình cộng dần
        rating      = ((rating * ratingCount) + score) / (ratingCount + 1);
        ratingCount++;
        updatedAt   = LocalDateTime.now();
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
     * <p><b>Lưu ý:</b> Buyer trả về chỉ dùng cho mục đích đặt bid.
     * Khi thanh toán thực sự, hệ thống sẽ trừ tiền qua {@code userRepo}
     * dựa trên {@code id} — vì vậy cột nào bị trừ (wallet hay earnings)
     * phụ thuộc vào logic trong {@code UserRepository}.</p>
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
        // Seller dùng earningsBalance (tiền kiếm được từ bán hàng) làm ví để đặt bid
        proxy.setWalletBalance(this.earningsBalance != null ? this.earningsBalance : BigDecimal.ZERO);
        return proxy;
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public BigDecimal getEarningsBalance()                  { return earningsBalance; }
    public void setEarningsBalance(BigDecimal balance)      { this.earningsBalance = balance; }

    public int getTotalSold()                               { return totalSold; }
    public void setTotalSold(int totalSold)                 { this.totalSold = totalSold; }

    public double getRating()                               { return rating; }
    public void setRating(double rating)                    { this.rating = rating; }

    public int getRatingCount()                             { return ratingCount; }
    public void setRatingCount(int ratingCount)             { this.ratingCount = ratingCount; }

    @Override
    public String toString() {
        return String.format("Seller{id=%d, username='%s', earnings=%s, sold=%d, rating=%.1f}",
                id, username, earningsBalance, totalSold, rating);
    }
}