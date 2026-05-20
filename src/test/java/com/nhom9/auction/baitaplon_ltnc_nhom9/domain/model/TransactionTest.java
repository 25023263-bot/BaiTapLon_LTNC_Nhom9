package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho Transaction – kiểm tra trạng thái thanh toán.
 *
 * LƯU Ý: Các test về platformFee, totalPaid, sellerReceives, shippingFee
 * đã bị xóa vì Transaction hiện tại chưa có các fields/methods đó.
 * Transaction hiện tại được thiết kế đơn giản, chỉ lưu amount cơ bản.
 * Sẽ được thêm lại khi tích hợp cổng thanh toán thật.
 */
@DisplayName("Transaction – Trạng thái thanh toán")
class TransactionTest {

    private Transaction transaction;

    @BeforeEach
    void setUp() {
        // Dùng constructor 5 tham số hiện có: (auctionId, buyerId, sellerId, amount, paymentMethod)
        transaction = new Transaction(
                10,                          // auctionId
                42,                          // buyerId
                7,                           // sellerId
                new BigDecimal("1000000"),   // amount
                "WALLET"                     // paymentMethod
        );
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

    @Test
    @DisplayName("constructor: amount được lưu đúng")
    void constructor_amountStoredCorrectly() {
        assertEquals(new BigDecimal("1000000"), transaction.getAmount());
    }

    @Test
    @DisplayName("constructor: paymentMethod được lưu đúng")
    void constructor_paymentMethodStoredCorrectly() {
        assertEquals("WALLET", transaction.getPaymentMethod());
    }

    @Test
    @DisplayName("constructor: completedAt ban đầu là null")
    void constructor_completedAtIsNull() {
        assertNull(transaction.getCompletedAt());
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

    @Test
    @DisplayName("markFailed: completedAt được set")
    void markFailed_completedAtIsSet() {
        transaction.markFailed();

        assertNotNull(transaction.getCompletedAt());
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

    // ─── Constructor đầy đủ (load từ DB) ─────────────────────────────────────

    @Test
    @DisplayName("constructor đầy đủ: lưu đúng tất cả fields")
    void constructor_fullArgs_storesAllFields() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Transaction tx = new Transaction(
                1,                           // id
                10,                          // auctionId
                42,                          // buyerId
                7,                           // sellerId
                new BigDecimal("500000"),    // amount
                "WALLET",                    // paymentMethod
                PaymentStatus.COMPLETED,     // status
                now,                         // createdAt
                now                          // completedAt
        );

        assertEquals(1, tx.getId());
        assertEquals(10, tx.getAuctionId());
        assertEquals(42, tx.getBuyerId());
        assertEquals(7, tx.getSellerId());
        assertEquals(new BigDecimal("500000"), tx.getAmount());
        assertEquals(PaymentStatus.COMPLETED, tx.getPaymentStatus());
        assertTrue(tx.isCompleted());
    }
}
