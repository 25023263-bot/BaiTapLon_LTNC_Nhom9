package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho PhysicalItem – logic riêng của vật phẩm vật lý.
 */
@DisplayName("PhysicalItem – Vật phẩm vật lý")
class PhysicalItemTest {

    private static final LocalDateTime START = LocalDateTime.now().minusHours(1);
    private static final LocalDateTime END   = LocalDateTime.now().plusHours(2);

    // Helper tạo item nhanh với currentPrice mặc định = startingPrice
    private PhysicalItem makeItem(BigDecimal startingPrice, BigDecimal shippingCost) {
        return new PhysicalItem(
                1, 10,
                "Test Physical Item", "Mô tả", "Category",
                startingPrice, new BigDecimal("10000"),
                START, END,
                "NEW", 500.0, "20x10x5 cm", "HN", shippingCost, false
        );
    }

    // ─── getTotalCostForBuyer() ───────────────────────────────────────────────

    @Test
    @DisplayName("getTotalCostForBuyer: có phí ship → tổng = currentPrice + shippingCost")
    void getTotalCostForBuyer_withShipping_returnsPriceAndShipping() {
        PhysicalItem item = makeItem(new BigDecimal("500000"), new BigDecimal("30000"));

        // currentPrice = 500K, ship = 30K → total = 530K
        BigDecimal expected = new BigDecimal("530000");
        assertEquals(expected, item.getTotalCostForBuyer());
    }

    @Test
    @DisplayName("getTotalCostForBuyer: ship = 0 → tổng = currentPrice")
    void getTotalCostForBuyer_freeShipping_equalsCurrentPrice() {
        PhysicalItem item = makeItem(new BigDecimal("500000"), BigDecimal.ZERO);

        assertEquals(new BigDecimal("500000"), item.getTotalCostForBuyer());
    }

    @Test
    @DisplayName("getTotalCostForBuyer: ship = null → tổng = currentPrice (không crash)")
    void getTotalCostForBuyer_nullShipping_equalsCurrentPrice() {
        PhysicalItem item = makeItem(new BigDecimal("500000"), null);

        // Phải xử lý được null shippingCost, không ném NullPointerException
        assertEquals(new BigDecimal("500000"), item.getTotalCostForBuyer());
    }

    // ─── isFreeShipping() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("isFreeShipping: shippingCost = 0 → true")
    void isFreeShipping_zeroCost_returnsTrue() {
        PhysicalItem item = makeItem(new BigDecimal("200000"), BigDecimal.ZERO);

        assertTrue(item.isFreeShipping());
    }

    @Test
    @DisplayName("isFreeShipping: shippingCost = null → true")
    void isFreeShipping_nullCost_returnsTrue() {
        PhysicalItem item = makeItem(new BigDecimal("200000"), null);

        assertTrue(item.isFreeShipping());
    }

    @Test
    @DisplayName("isFreeShipping: shippingCost > 0 → false")
    void isFreeShipping_withCost_returnsFalse() {
        PhysicalItem item = makeItem(new BigDecimal("200000"), new BigDecimal("25000"));

        assertFalse(item.isFreeShipping());
    }

    // ─── getItemType() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getItemType() → trả về 'PHYSICAL'")
    void getItemType_returnsPhysical() {
        PhysicalItem item = makeItem(new BigDecimal("100000"), BigDecimal.ZERO);

        assertEquals("PHYSICAL", item.getItemType());
    }

    // ─── isValidItem() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("isValidItem: condition rỗng → false (điều kiện riêng của PhysicalItem)")
    void isValidItem_emptyCondition_returnsFalse() {
        PhysicalItem item = new PhysicalItem(
                2, 10, "Valid Title", "desc", "cat",
                new BigDecimal("100000"), new BigDecimal("10000"),
                START, END,
                "",  // condition rỗng
                500.0, null, "HN", BigDecimal.ZERO, false
        );

        // PhysicalItem yêu cầu condition phải có giá trị
        assertFalse(item.isValidItem());
    }

    @Test
    @DisplayName("isValidItem: tất cả hợp lệ → true")
    void isValidItem_allFieldsValid_returnsTrue() {
        PhysicalItem item = new PhysicalItem(
                3, 10, "Laptop Used", "Like new", "Electronics",
                new BigDecimal("5000000"), new BigDecimal("100000"),
                START, END,
                "LIKE_NEW", 1500.0, "35x25x3 cm", "HCM",
                new BigDecimal("50000"), true
        );

        assertTrue(item.isValidItem());
    }
}
