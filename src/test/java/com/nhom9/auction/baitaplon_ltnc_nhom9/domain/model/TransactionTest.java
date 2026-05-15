package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho Transaction – kiểm tra tính toán tài chính và trạng thái thanh toán.
 *
 * Đây là logic quan trọng nhất từ góc độ kinh doanh:
 * - platformFee phải được tính đúng (2% mặc định)
 * - sellerReceives = amount - platformFee
 * - totalPaid = amount + shippingFee
 */
@DisplayName("Transaction – Tính toán tài chính")
class TransactionTest {

    // Platform fee rate = 2% (giống AppConfig.PLATFORM_FEE_RATE)
    private static final double PLATFORM_FEE_RATE = 0.02;

    private Transaction transaction;

    @BeforeEach
    void setUp() {
        // Tạo transaction: bid thắng 1.000.000đ, ship 30.000đ, fee 2%
        transaction = new Transaction(
                10,    // auctionId
                42,    // buyerId
                7,     // sellerId
                new BigDecimal("1000000"),  // amount (giá thắng)
                new BigDecimal("30000"),    // shippingFee
                PLATFORM_FEE_RATE,          // 2%
                "WALLET"                    // paymentMethod
        );
    }

    // ─── Tính toán tài chính ──────────────────────────────────────────────────

    @Test
    @DisplayName("platformFee: 2% của 1.000.000đ = 20.000đ")
    void platformFee_twoPercent_calculatedCorrectly() {
        // 1.000.000 * 0.02 = 20.000
        BigDecimal expectedFee = new BigDecimal("1000000")
                .multiply(BigDecimal.valueOf(PLATFORM_FEE_RATE));

        assertEquals(0, expectedFee.compareTo(transaction.getPlatformFee()),
                "Platform fee phải bằng amount * feeRate");
    }

    @Test
    @DisplayName("totalPaid: buyer phải trả amount + shippingFee = 1.030.000đ")
    void totalPaid_amountPlusShipping_calculatedCorrectly() {
        // 1.000.000 + 30.000 = 1.030.000
        BigDecimal expected = new BigDecimal("1030000");

        assertEquals(0, expected.compareTo(transaction.getTotalPaid()),
                "Buyer phải trả amount + shipping");
    }

    @Test
    @DisplayName("sellerReceives: seller nhận amount - platformFee = 980.000đ")
    void sellerReceives_amountMinusFee_calculatedCorrectly() {
        // 1.000.000 - 20.000 = 980.000
        BigDecimal expected = new BigDecimal("980000");

        assertEquals(0, expected.compareTo(transaction.getSellerReceives()),
                "Seller nhận = amount - platformFee (không bao gồm shipping)");
    }

    @Test
    @DisplayName("shippingFee = null → tự động thay bằng 0, không crash")
    void constructor_nullShippingFee_defaultsToZero() {
        Transaction noShip = new Transaction(
                11, 42, 7,
                new BigDecimal("500000"),
                null,  // null shipping
                PLATFORM_FEE_RATE,
                "WALLET"
        );

        // totalPaid = 500K + 0 = 500K
        assertEquals(0, new BigDecimal("500000").compareTo(noShip.getTotalPaid()));
    }

    @Test
    @DisplayName("feeRate = 0 → platformFee = 0, seller nhận đủ 100%")
    void constructor_zeroFeeRate_sellerReceivesFullAmount() {
        Transaction noFee = new Transaction(
                12, 42, 7,
                new BigDecimal("1000000"),
                BigDecimal.ZERO,
                0.0,  // không thu phí
                "WALLET"
        );

        assertEquals(0, BigDecimal.ZERO.compareTo(noFee.getPlatformFee()));
        assertEquals(0, new BigDecimal("1000000").compareTo(noFee.getSellerReceives()));
    }

    // ─── Trạng thái ban đầu ───────────────────────────────────────────────────

    @Test
    @DisplayName("trạng thái ban đầu: PENDING")
    void initialStatus_isPending() {
        assertTrue(transaction.isPending());
        assertFalse(transaction.isCompleted());
        assertFalse(transaction.isFailed());
    }

    @Test
    @DisplayName("createdAt: được set tự động khi tạo")
    void createdAt_setAutomatically() {
        assertNotNull(transaction.getCreatedAt());
    }

    // ─── markCompleted() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("markCompleted: status chuyển sang COMPLETED")
    void markCompleted_statusBecomesCompleted() {
        transaction.markCompleted();

        assertTrue(transaction.isCompleted());
        assertFalse(transaction.isPending());
        assertEquals(PaymentStatus.COMPLETED, transaction.getPaymentStatus());
    }

    @Test
    @DisplayName("markCompleted: completedAt được set")
    void markCompleted_completedAtIsSet() {
        transaction.markCompleted();

        assertNotNull(transaction.getCompletedAt());
    }

    // ─── markFailed() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("markFailed: status chuyển sang FAILED")
    void markFailed_statusBecomesFailed() {
        transaction.markFailed();

        assertTrue(transaction.isFailed());
        assertFalse(transaction.isPending());
        assertEquals(PaymentStatus.FAILED, transaction.getPaymentStatus());
    }

    // ─── markRefunded() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("markRefunded: status chuyển sang REFUNDED")
    void markRefunded_statusBecomesRefunded() {
        transaction.markRefunded();

        assertEquals(PaymentStatus.REFUNDED, transaction.getPaymentStatus());
        assertFalse(transaction.isPending());
        assertFalse(transaction.isCompleted());
        assertFalse(transaction.isFailed());
    }

    @Test
    @DisplayName("markRefunded: completedAt được set")
    void markRefunded_completedAtIsSet() {
        transaction.markRefunded();

        assertNotNull(transaction.getCompletedAt());
    }
}
