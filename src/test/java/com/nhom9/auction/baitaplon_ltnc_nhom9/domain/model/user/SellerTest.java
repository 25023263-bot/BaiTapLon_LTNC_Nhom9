package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho Seller – kiểm tra logic nhận tiền, rút tiền, rating và proxy Buyer.
 */
@DisplayName("Seller – Quản lý thu nhập và rating")
class SellerTest {

    private Seller seller;

    @BeforeEach
    void setUp() {
        seller = new Seller(2, "seller1", "seller@example.com",
                "hashed_pw", "Tran Thi B", "0987654321");
    }

    // ─── receivePayment() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("receivePayment: nhận tiền hợp lệ → earningsBalance tăng và totalSold tăng 1")
    void receivePayment_validAmount_earningsIncreasedAndSoldIncremented() {
        seller.receivePayment(new BigDecimal("2000000"));

        assertEquals(new BigDecimal("2000000"), seller.getEarningsBalance());
        assertEquals(1, seller.getTotalSold());
    }

    @Test
    @DisplayName("receivePayment: nhận nhiều lần → cộng dồn đúng")
    void receivePayment_multipleTimes_accumulates() {
        seller.receivePayment(new BigDecimal("1000000"));
        seller.receivePayment(new BigDecimal("500000"));

        assertEquals(new BigDecimal("1500000"), seller.getEarningsBalance());
        assertEquals(2, seller.getTotalSold());
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

    // ─── withdraw() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("withdraw: số dư đủ → rút thành công, số dư giảm")
    void withdraw_sufficientBalance_balanceDecreased() {
        seller.receivePayment(new BigDecimal("3000000"));
        seller.withdraw(new BigDecimal("1000000"));

        assertEquals(new BigDecimal("2000000"), seller.getEarningsBalance());
    }

    @Test
    @DisplayName("withdraw: rút toàn bộ → số dư về 0")
    void withdraw_fullAmount_balanceBecomesZero() {
        seller.receivePayment(new BigDecimal("1000000"));
        seller.withdraw(new BigDecimal("1000000"));

        assertEquals(BigDecimal.ZERO, seller.getEarningsBalance());
    }

    @Test
    @DisplayName("withdraw: số dư không đủ → ném IllegalStateException")
    void withdraw_insufficientBalance_throwsException() {
        seller.receivePayment(new BigDecimal("100000"));

        assertThrows(IllegalStateException.class,
                () -> seller.withdraw(new BigDecimal("500000")));
    }

    @Test
    @DisplayName("withdraw: earningsBalance = 0 → ném IllegalStateException")
    void withdraw_emptyBalance_throwsException() {
        assertThrows(IllegalStateException.class,
                () -> seller.withdraw(new BigDecimal("1")));
    }

    // ─── addRating() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("addRating: đánh giá đầu tiên → rating chính xác")
    void addRating_firstRating_ratingIsCorrect() {
        seller.addRating(4);

        assertEquals(4.0, seller.getRating(), 0.001);
        assertEquals(1, seller.getRatingCount());
    }

    @Test
    @DisplayName("addRating: hai đánh giá → trung bình cộng đúng")
    void addRating_twoRatings_averageIsCorrect() {
        seller.addRating(4);
        seller.addRating(2);

        // (4 + 2) / 2 = 3.0
        assertEquals(3.0, seller.getRating(), 0.001);
        assertEquals(2, seller.getRatingCount());
    }

    @Test
    @DisplayName("addRating: nhiều đánh giá → trung bình cộng dần đúng")
    void addRating_multipleRatings_rollingAverageIsCorrect() {
        seller.addRating(5);
        seller.addRating(3);
        seller.addRating(4);

        // (5 + 3 + 4) / 3 = 4.0
        assertEquals(4.0, seller.getRating(), 0.001);
        assertEquals(3, seller.getRatingCount());
    }

    @Test
    @DisplayName("addRating: điểm = 0 → ném IllegalArgumentException (dưới mức tối thiểu)")
    void addRating_scoreBelowMinimum_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> seller.addRating(0));
    }

    @Test
    @DisplayName("addRating: điểm = 6 → ném IllegalArgumentException (vượt tối đa)")
    void addRating_scoreAboveMaximum_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> seller.addRating(6));
    }

    @Test
    @DisplayName("addRating: điểm = 1 và 5 là biên hợp lệ → không ném exception")
    void addRating_boundaryValues_noException() {
        // 1 và 5 là giá trị biên hợp lệ
        assertDoesNotThrow(() -> seller.addRating(1));
        assertDoesNotThrow(() -> seller.addRating(5));
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

        // Buyer proxy phải có walletBalance = earningsBalance của Seller
        assertEquals(seller.getEarningsBalance(), proxy.getWalletBalance());
    }

    @Test
    @DisplayName("asBuyer: khi earningsBalance = 0 → proxy wallet = 0")
    void asBuyer_zeroEarnings_proxyWalletIsZero() {
        Buyer proxy = seller.asBuyer();

        assertEquals(BigDecimal.ZERO, proxy.getWalletBalance());
    }
}
