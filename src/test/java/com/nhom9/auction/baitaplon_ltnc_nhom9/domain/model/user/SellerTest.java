package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho Seller – kiểm tra logic nhận tiền và proxy Buyer.
 *
 * LƯU Ý: Các test sau đã bị xóa vì Seller hiện tại chưa implement:
 *   - getTotalSold()   → Seller chưa có field totalSold
 *   - withdraw()       → Seller chưa có method rút tiền
 *   - addRating()      → Seller chưa có hệ thống rating
 *   - getRating()
 *   - getRatingCount()
 * Sẽ được thêm lại khi production code sẵn sàng.
 */
@DisplayName("Seller – Quản lý thu nhập và proxy buyer")
class SellerTest {

    private Seller seller;

    @BeforeEach
    void setUp() {
        seller = new Seller(2, "seller1", "seller@example.com",
                "hashed_pw", "Tran Thi B", "0987654321");
    }

    // ─── receivePayment() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("receivePayment: nhận tiền hợp lệ → earningsBalance tăng đúng")
    void receivePayment_validAmount_earningsIncreased() {
        seller.receivePayment(new BigDecimal("2000000"));

        assertEquals(new BigDecimal("2000000"), seller.getEarningsBalance());
    }

    @Test
    @DisplayName("receivePayment: nhận nhiều lần → cộng dồn đúng")
    void receivePayment_multipleTimes_accumulates() {
        seller.receivePayment(new BigDecimal("1000000"));
        seller.receivePayment(new BigDecimal("500000"));

        assertEquals(new BigDecimal("1500000"), seller.getEarningsBalance());
    }

    @Test
    @DisplayName("receivePayment: số tiền = 0 → ném IllegalArgumentException")
    void receivePayment_zeroAmount_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> seller.receivePayment(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("receivePayment: số tiền âm → ném IllegalArgumentException")
    void receivePayment_negativeAmount_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> seller.receivePayment(new BigDecimal("-500000")));
    }

    @Test
    @DisplayName("receivePayment: null → ném IllegalArgumentException")
    void receivePayment_null_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> seller.receivePayment(null));
    }

    // ─── Constructor ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("constructor: role tự động là SELLER")
    void constructor_roleIsAutomaticallySeller() {
        assertEquals(com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole.SELLER,
                seller.getRole());
    }

    @Test
    @DisplayName("constructor: earningsBalance ban đầu = 0")
    void constructor_initialEarningsIsZero() {
        assertEquals(BigDecimal.ZERO, seller.getEarningsBalance());
    }

    // ─── asBuyer() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("asBuyer: proxy mang đúng id và username của Seller")
    void asBuyer_proxyHasCorrectIdentity() {
        Buyer proxy = seller.asBuyer();

        assertEquals(seller.getId(), proxy.getId());
        assertEquals(seller.getUsername(), proxy.getUsername());
    }

    @Test
    @DisplayName("asBuyer: proxy dùng earningsBalance làm ví")
    void asBuyer_proxyWalletEqualsEarningsBalance() {
        seller.receivePayment(new BigDecimal("5000000"));
        Buyer proxy = seller.asBuyer();

        assertEquals(seller.getEarningsBalance(), proxy.getWalletBalance());
    }

    @Test
    @DisplayName("asBuyer: khi earningsBalance = 0 → proxy wallet = 0")
    void asBuyer_zeroEarnings_proxyWalletIsZero() {
        Buyer proxy = seller.asBuyer();

        assertEquals(BigDecimal.ZERO, proxy.getWalletBalance());
    }
}
