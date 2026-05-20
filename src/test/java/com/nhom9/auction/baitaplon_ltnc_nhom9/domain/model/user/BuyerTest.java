package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;

import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho Buyer – kiểm tra logic ví tiền và các thao tác liên quan.
 *
 * LƯU Ý: Các test liên quan đến incrementWins() và getTotalWins() đã bị xóa
 * vì Buyer hiện tại chưa có field totalWins.
 * Sẽ được thêm lại khi production code sẵn sàng.
 */
@DisplayName("Buyer – Quản lý ví tiền")
class BuyerTest {

    private Buyer buyer;

    @BeforeEach
    void setUp() {
        buyer = new Buyer(1, "testuser", "test@example.com",
                "hashed_pw", "Nguyen Van A", "0901234567");
    }

    // ─── deposit() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deposit: nạp tiền hợp lệ → số dư tăng đúng")
    void deposit_validAmount_balanceIncreased() {
        buyer.deposit(new BigDecimal("500000"));

        assertEquals(new BigDecimal("500000"), buyer.getWalletBalance());
    }

    @Test
    @DisplayName("deposit: nạp nhiều lần → số dư cộng dồn đúng")
    void deposit_multipleTimes_balanceAccumulates() {
        buyer.deposit(new BigDecimal("200000"));
        buyer.deposit(new BigDecimal("300000"));

        assertEquals(new BigDecimal("500000"), buyer.getWalletBalance());
    }

    @Test
    @DisplayName("deposit: số tiền = 0 → ném IllegalArgumentException")
    void deposit_zeroAmount_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> buyer.deposit(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("deposit: số tiền âm → ném IllegalArgumentException")
    void deposit_negativeAmount_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> buyer.deposit(new BigDecimal("-100000")));
    }

    @Test
    @DisplayName("deposit: null → ném IllegalArgumentException")
    void deposit_null_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> buyer.deposit(null));
    }

    // ─── deduct() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deduct: số dư đủ → trừ tiền thành công")
    void deduct_sufficientBalance_balanceDecreased() throws InsufficientBalanceException {
        buyer.deposit(new BigDecimal("1000000"));
        buyer.deduct(new BigDecimal("300000"));

        assertEquals(new BigDecimal("700000"), buyer.getWalletBalance());
    }

    @Test
    @DisplayName("deduct: trừ toàn bộ số dư → số dư về 0")
    void deduct_exactBalance_balanceBecomesZero() throws InsufficientBalanceException {
        buyer.deposit(new BigDecimal("500000"));
        buyer.deduct(new BigDecimal("500000"));

        assertEquals(BigDecimal.ZERO, buyer.getWalletBalance());
    }

    @Test
    @DisplayName("deduct: số dư không đủ → ném InsufficientBalanceException")
    void deduct_insufficientBalance_throwsException() {
        buyer.deposit(new BigDecimal("100000"));

        assertThrows(InsufficientBalanceException.class,
                () -> buyer.deduct(new BigDecimal("200000")));
    }

    @Test
    @DisplayName("deduct: ví rỗng → ném InsufficientBalanceException")
    void deduct_emptyWallet_throwsException() {
        assertThrows(InsufficientBalanceException.class,
                () -> buyer.deduct(new BigDecimal("1")));
    }

    @Test
    @DisplayName("deduct: số tiền âm → ném IllegalArgumentException")
    void deduct_negativeAmount_throwsException() {
        buyer.deposit(new BigDecimal("500000"));

        assertThrows(IllegalArgumentException.class,
                () -> buyer.deduct(new BigDecimal("-1000")));
    }

    // ─── hasSufficientBalance() ───────────────────────────────────────────────

    @Test
    @DisplayName("hasSufficientBalance: số dư đủ → trả về true")
    void hasSufficientBalance_enoughMoney_returnsTrue() {
        buyer.deposit(new BigDecimal("1000000"));

        assertTrue(buyer.hasSufficientBalance(new BigDecimal("500000")));
    }

    @Test
    @DisplayName("hasSufficientBalance: số dư bằng đúng → trả về true (biên)")
    void hasSufficientBalance_exactAmount_returnsTrue() {
        buyer.deposit(new BigDecimal("500000"));

        assertTrue(buyer.hasSufficientBalance(new BigDecimal("500000")));
    }

    @Test
    @DisplayName("hasSufficientBalance: số dư thiếu → trả về false")
    void hasSufficientBalance_notEnoughMoney_returnsFalse() {
        buyer.deposit(new BigDecimal("100000"));

        assertFalse(buyer.hasSufficientBalance(new BigDecimal("200000")));
    }

    // ─── Constructor ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("constructor: role tự động là BUYER")
    void constructor_roleIsAutomaticallyBuyer() {
        assertEquals(com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole.BUYER,
                buyer.getRole());
    }

    @Test
    @DisplayName("constructor: ví ban đầu = 0")
    void constructor_initialWalletBalanceIsZero() {
        assertEquals(BigDecimal.ZERO, buyer.getWalletBalance());
    }
}
