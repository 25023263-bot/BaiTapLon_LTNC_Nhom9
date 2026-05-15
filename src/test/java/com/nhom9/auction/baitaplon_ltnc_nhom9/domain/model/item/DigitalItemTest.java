package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho DigitalItem – kiểm tra logic riêng của vật phẩm kỹ thuật số.
 */
@DisplayName("DigitalItem – Vật phẩm kỹ thuật số")
class DigitalItemTest {

    private static final LocalDateTime NOW   = LocalDateTime.now();
    private static final LocalDateTime START = NOW.minusHours(1);
    private static final LocalDateTime END   = NOW.plusHours(2);

    private DigitalItem validItem;

    @BeforeEach
    void setUp() {
        validItem = new DigitalItem(
                1, 5,
                "Game Key CS2", "Key game CS2 mới 100%", "Gaming",
                new BigDecimal("200000"), new BigDecimal("10000"), null,
                START, END,
                "GAME_CODE", "Windows",
                null,       // fileSizeMB
                null,       // expiryDate – không hết hạn
                "XXXXX-YYYYY-ZZZZZ-AAAAA-BBBBB",  // deliveryContent
                true        // replacementGuarantee
        );
    }

    // ─── getItemType() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getItemType() → trả về 'DIGITAL'")
    void getItemType_returnsDigital() {
        assertEquals("DIGITAL", validItem.getItemType());
    }

    // ─── getShippingCost() ────────────────────────────────────────────────────

    @Test
    @DisplayName("getShippingCost: vật phẩm số luôn miễn phí ship → trả về 0")
    void getShippingCost_alwaysZero() {
        assertEquals(BigDecimal.ZERO, validItem.getShippingCost());
    }

    // ─── getTotalCostForBuyer() ───────────────────────────────────────────────

    @Test
    @DisplayName("getTotalCostForBuyer: không có ship → tổng = currentPrice")
    void getTotalCostForBuyer_equalsCurrentPrice() {
        // currentPrice mặc định = startingPrice = 200K (chưa có bid)
        assertEquals(validItem.getCurrentPrice(), validItem.getTotalCostForBuyer());
    }

    // ─── isExpired() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("isExpired: không có expiryDate → không bao giờ hết hạn")
    void isExpired_noExpiryDate_returnsFalse() {
        // validItem có expiryDate = null
        assertFalse(validItem.isExpired());
    }

    @Test
    @DisplayName("isExpired: expiryDate trong quá khứ → đã hết hạn")
    void isExpired_pastExpiryDate_returnsTrue() {
        DigitalItem expiredKey = new DigitalItem(
                2, 5, "Old Key", "desc", "cat",
                new BigDecimal("100000"), new BigDecimal("5000"), null,
                START, END,
                "GAME_CODE", "Windows", null,
                NOW.minusDays(1),  // hết hạn hôm qua
                "KEY-EXPIRED",
                false
        );

        assertTrue(expiredKey.isExpired());
    }

    @Test
    @DisplayName("isExpired: expiryDate trong tương lai → chưa hết hạn")
    void isExpired_futureExpiryDate_returnsFalse() {
        DigitalItem futureKey = new DigitalItem(
                3, 5, "Valid Key", "desc", "cat",
                new BigDecimal("100000"), new BigDecimal("5000"), null,
                START, END,
                "GAME_CODE", "Windows", null,
                NOW.plusYears(1),  // hết hạn 1 năm sau
                "KEY-VALID",
                true
        );

        assertFalse(futureKey.isExpired());
    }

    // ─── isValidItem() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("isValidItem: item hợp lệ đầy đủ → true")
    void isValidItem_validData_returnsTrue() {
        assertTrue(validItem.isValidItem());
    }

    @Test
    @DisplayName("isValidItem: thiếu deliveryContent → false")
    void isValidItem_emptyDeliveryContent_returnsFalse() {
        DigitalItem noKey = new DigitalItem(
                4, 5, "No Key Item", "desc", "cat",
                new BigDecimal("100000"), new BigDecimal("5000"), null,
                START, END,
                "GAME_CODE", "Windows", null, null,
                "",  // deliveryContent rỗng
                false
        );

        assertFalse(noKey.isValidItem());
    }

    @Test
    @DisplayName("isValidItem: thiếu digitalType → false")
    void isValidItem_emptyDigitalType_returnsFalse() {
        DigitalItem noType = new DigitalItem(
                5, 5, "No Type Item", "desc", "cat",
                new BigDecimal("100000"), new BigDecimal("5000"), null,
                START, END,
                "",  // digitalType rỗng
                "Windows", null, null, "SOME-KEY",
                false
        );

        assertFalse(noType.isValidItem());
    }
}
