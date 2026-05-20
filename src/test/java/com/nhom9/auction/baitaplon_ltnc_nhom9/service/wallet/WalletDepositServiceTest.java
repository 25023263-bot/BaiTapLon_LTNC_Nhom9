package com.nhom9.auction.baitaplon_ltnc_nhom9.service.wallet;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Admin;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho WalletDepositService.
 *
 * <p>Tại sao test service này?</p>
 * <ul>
 *   <li>parseAmount() xử lý nhiều dạng input (có dấu chấm, dấu phẩy, khoảng trắng) → dễ sai</li>
 *   <li>deposit() có rollback nếu DB fail → phải đảm bảo RAM không bị cập nhật sai</li>
 *   <li>Logic phân biệt Buyer/Seller/Admin phải đúng</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletDepositService — Nạp tiền vào ví")
class WalletDepositServiceTest {

    @Mock private UserRepository userRepo;

    private WalletDepositService depositService;

    @BeforeEach
    void setUp() {
        depositService = new WalletDepositService(userRepo);
    }

    // =========================================================================
    // parseAmount() tests
    // =========================================================================

    @Nested
    @DisplayName("parseAmount()")
    class ParseAmountTests {

        @Test
        @DisplayName("Số nguyên thông thường → parse đúng")
        void parseAmount_plainNumber_parsesCorrectly() {
            assertEquals(new BigDecimal("50000"), depositService.parseAmount("50000"));
        }

        @ParameterizedTest(name = "Input=''{0}'' → 100000")
        @ValueSource(strings = { "100,000", "100.000", "100 000", " 100000 " })
        @DisplayName("Số có dấu phân cách hoặc khoảng trắng → loại bỏ, parse đúng")
        void parseAmount_withSeparators_parsesCorrectly(String input) {
            assertEquals(new BigDecimal("100000"), depositService.parseAmount(input));
        }

        @Test
        @DisplayName("Chuỗi rỗng → IllegalArgumentException")
        void parseAmount_empty_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> depositService.parseAmount(""));
        }

        @Test
        @DisplayName("Chỉ có chữ (không có số) → IllegalArgumentException")
        void parseAmount_noDigits_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> depositService.parseAmount("abc"));
        }
    }

    // =========================================================================
    // validateAmount() tests
    // =========================================================================

    @Nested
    @DisplayName("validateAmount()")
    class ValidateAmountTests {

        @Test
        @DisplayName("Đúng bằng MIN_AMOUNT (10.000đ) → không throw")
        void validateAmount_exactMinimum_doesNotThrow() {
            assertDoesNotThrow(
                    () -> depositService.validateAmount(WalletDepositService.MIN_AMOUNT));
        }

        @Test
        @DisplayName("Lớn hơn MIN_AMOUNT → không throw")
        void validateAmount_aboveMinimum_doesNotThrow() {
            assertDoesNotThrow(
                    () -> depositService.validateAmount(new BigDecimal("50000")));
        }

        @Test
        @DisplayName("Dưới MIN_AMOUNT (9.999đ) → IllegalArgumentException")
        void validateAmount_belowMinimum_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> depositService.validateAmount(new BigDecimal("9999")));
        }

        @Test
        @DisplayName("Số 0 → IllegalArgumentException")
        void validateAmount_zero_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> depositService.validateAmount(BigDecimal.ZERO));
        }
    }

    // =========================================================================
    // deposit() tests — Buyer
    // =========================================================================

    @Nested
    @DisplayName("deposit() — Buyer")
    class BuyerDepositTests {

        @Test
        @DisplayName("Buyer: nạp tiền thành công → walletBalance tăng, DB được cập nhật")
        void deposit_buyer_updatesBalanceAndCallsRepo() throws Exception {
            Buyer buyer = new Buyer(1, "buyer1", "b@test.com", "hash", "Test", "090");
            buyer.setWalletBalance(new BigDecimal("100000"));

            depositService.deposit(buyer, new BigDecimal("50000"));

            // RAM đã cập nhật
            assertEquals(new BigDecimal("150000"), buyer.getWalletBalance());
            // DB được gọi
            verify(userRepo).updateWalletBalance(eq(1), eq(new BigDecimal("150000")));
        }

        @Test
        @DisplayName("Buyer: DB fail → rollback RAM về số dư cũ, re-throw SQLException")
        void deposit_buyer_dbFails_rollsBackAndRethrows() throws Exception {
            Buyer buyer = new Buyer(1, "buyer1", "b@test.com", "hash", "Test", "090");
            buyer.setWalletBalance(new BigDecimal("100000"));

            // Giả lập DB lỗi
            doThrow(new SQLException("DB error"))
                    .when(userRepo).updateWalletBalance(anyInt(), any());

            assertThrows(SQLException.class,
                    () -> depositService.deposit(buyer, new BigDecimal("50000")));

            // Sau khi rollback, balance phải về giá trị ban đầu
            assertEquals(new BigDecimal("100000"), buyer.getWalletBalance(),
                    "RAM phải được rollback về số dư cũ khi DB fail");
        }
    }

    // =========================================================================
    // deposit() tests — Seller
    // =========================================================================

    @Nested
    @DisplayName("deposit() — Seller")
    class SellerDepositTests {

        @Test
        @DisplayName("Seller: nạp tiền thành công → earningsBalance tăng, DB được cập nhật")
        void deposit_seller_updatesEarningsAndCallsRepo() throws Exception {
            Seller seller = new Seller(2, "seller1", "s@test.com", "hash", "Test", "091");
            seller.setEarningsBalance(new BigDecimal("200000"));

            depositService.deposit(seller, new BigDecimal("100000"));

            assertEquals(new BigDecimal("300000"), seller.getEarningsBalance());
            verify(userRepo).updateEarningsBalance(eq(2), eq(new BigDecimal("300000")));
        }

        @Test
        @DisplayName("Seller: DB fail → rollback earningsBalance về cũ")
        void deposit_seller_dbFails_rollsBack() throws Exception {
            Seller seller = new Seller(2, "seller1", "s@test.com", "hash", "Test", "091");
            seller.setEarningsBalance(new BigDecimal("200000"));

            doThrow(new SQLException("DB error"))
                    .when(userRepo).updateEarningsBalance(anyInt(), any());

            assertThrows(SQLException.class,
                    () -> depositService.deposit(seller, new BigDecimal("100000")));

            assertEquals(new BigDecimal("200000"), seller.getEarningsBalance(),
                    "earningsBalance phải được rollback");
        }

        @Test
        @DisplayName("Seller: earningsBalance null ban đầu → vẫn nạp được (treat as 0)")
        void deposit_seller_nullEarnings_treatedAsZero() throws Exception {
            Seller seller = new Seller(2, "seller1", "s@test.com", "hash", "Test", "091");
            seller.setEarningsBalance(null);

            depositService.deposit(seller, new BigDecimal("50000"));

            assertEquals(new BigDecimal("50000"), seller.getEarningsBalance());
        }
    }

    // =========================================================================
    // deposit() tests — Admin
    // =========================================================================

    @Test
    @DisplayName("Admin không hỗ trợ nạp tiền → IllegalStateException")
    void deposit_admin_throwsIllegalStateException() {
        Admin admin = new Admin(3, "admin1", "a@test.com", "hash", "Admin", "092");

        assertThrows(IllegalStateException.class,
                () -> depositService.deposit(admin, new BigDecimal("50000")));

        verifyNoInteractions(userRepo);
    }
}
