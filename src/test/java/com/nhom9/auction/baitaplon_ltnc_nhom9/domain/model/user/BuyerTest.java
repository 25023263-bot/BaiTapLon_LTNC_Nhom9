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
 * Nguyên tắc viết test: mỗi test chỉ kiểm tra MỘT hành vi cụ thể.
 * Tên test theo format: methodName_condition_expectedResult
 */
@DisplayName("Buyer – Quản lý ví tiền")
class BuyerTest {

    // Đối tượng dùng lại trong mỗi test, được tạo mới trước mỗi test (@BeforeEach)
    private Buyer buyer;

    @BeforeEach
    void setUp() {
        // Tạo một Buyer mới với ví bắt đầu = 0
        buyer = new Buyer(1, "testuser", "test@example.com",
                "hashed_pw", "Nguyen Van A", "0901234567");
    }

    // ─── deposit() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deposit: nạp tiền hợp lệ → số dư tăng đúng")
    void deposit_validAmount_balanceIncreased() {
        buyer.deposit(new BigDecimal("500000"));

        // 0 + 500_000 = 500_000
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
        // Khi nạp 0đ – đây là dữ liệu không hợp lệ, phải bị từ chối
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

        // Cố trừ 200_000 khi chỉ có 100_000
        assertThrows(InsufficientBalanceException.class,
                () -> buyer.deduct(new BigDecimal("200000")));
    }

    @Test
    @DisplayName("deduct: ví rỗng → ném InsufficientBalanceException")
    void deduct_emptyWallet_throwsException() {
        // Ví = 0, cố trừ 1đ
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

        // Kiểm tra biên: đúng bằng → vẫn hợp lệ
        assertTrue(buyer.hasSufficientBalance(new BigDecimal("500000")));
    }

    @Test
    @DisplayName("hasSufficientBalance: số dư thiếu → trả về false")
    void hasSufficientBalance_notEnoughMoney_returnsFalse() {
        buyer.deposit(new BigDecimal("100000"));

        assertFalse(buyer.hasSufficientBalance(new BigDecimal("200000")));
    }

    // ─── incrementWins() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("incrementWins: ban đầu = 0, gọi 1 lần → totalWins = 1")
    void incrementWins_once_totalWinsIsOne() {
        buyer.incrementWins();

        assertEquals(1, buyer.getTotalWins());
    }

    @Test
    @DisplayName("incrementWins: gọi nhiều lần → tích lũy đúng")
    void incrementWins_multipleTimes_accumulates() {
        buyer.incrementWins();
        buyer.incrementWins();
        buyer.incrementWins();

        assertEquals(3, buyer.getTotalWins());
    }

    // ─── asBuyer (inherited from Seller logic – tested via constructor) ────────

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

    @Test
    @DisplayName("constructor: totalWins ban đầu = 0")
    void constructor_initialTotalWinsIsZero() {
        assertEquals(0, buyer.getTotalWins());
    }
}
