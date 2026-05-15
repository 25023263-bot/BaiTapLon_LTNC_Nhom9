package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.common;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho FilterCriteria và Builder pattern.
 *
 * FilterCriteria là pure data object (không phụ thuộc DB hay UI).
 * Test này đảm bảo Builder hoạt động đúng và các has*() method chính xác.
 */
@DisplayName("FilterCriteria – Builder pattern và utility methods")
class FilterCriteriaTest {

    // ─── Builder cơ bản ───────────────────────────────────────────────────────

    @Test
    @DisplayName("build: không set gì cả → tất cả has*() trả false")
    void build_empty_allHasMethodsReturnFalse() {
        FilterCriteria criteria = FilterCriteria.builder().build();

        assertFalse(criteria.hasKeyword());
        assertFalse(criteria.hasCategory());
        assertFalse(criteria.hasItemType());
        assertFalse(criteria.hasStatus());
        assertFalse(criteria.hasPriceRange());
        assertFalse(criteria.hasSellerId());
        assertFalse(criteria.hasTimeRange());
        assertFalse(criteria.hasSort());
    }

    @Test
    @DisplayName("build: set keyword → hasKeyword() true, giá trị đúng")
    void build_withKeyword_hasKeywordTrue() {
        FilterCriteria criteria = FilterCriteria.builder()
                .keyword("laptop gaming")
                .build();

        assertTrue(criteria.hasKeyword());
        assertEquals("laptop gaming", criteria.getKeyword());
    }

    @Test
    @DisplayName("build: keyword rỗng → hasKeyword() false")
    void build_blankKeyword_hasKeywordFalse() {
        FilterCriteria criteria = FilterCriteria.builder()
                .keyword("   ")  // chỉ có khoảng trắng
                .build();

        assertFalse(criteria.hasKeyword());
    }

    @Test
    @DisplayName("build: set category → hasCategory() true")
    void build_withCategory_hasCategoryTrue() {
        FilterCriteria criteria = FilterCriteria.builder()
                .category("Electronics")
                .build();

        assertTrue(criteria.hasCategory());
        assertEquals("Electronics", criteria.getCategory());
    }

    @Test
    @DisplayName("build: set status = ACTIVE → hasStatus() true")
    void build_withStatus_hasStatusTrue() {
        FilterCriteria criteria = FilterCriteria.builder()
                .status(AuctionStatus.ACTIVE)
                .build();

        assertTrue(criteria.hasStatus());
        assertEquals(AuctionStatus.ACTIVE, criteria.getStatus());
    }

    // ─── priceRange ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("build: set priceRange → hasPriceRange() true")
    void build_withPriceRange_hasPriceRangeTrue() {
        FilterCriteria criteria = FilterCriteria.builder()
                .priceRange(new BigDecimal("100000"), new BigDecimal("500000"))
                .build();

        assertTrue(criteria.hasPriceRange());
        assertEquals(new BigDecimal("100000"), criteria.getMinPrice());
        assertEquals(new BigDecimal("500000"), criteria.getMaxPrice());
    }

    @Test
    @DisplayName("build: chỉ set minPrice → hasPriceRange() true (một mình cũng đủ)")
    void build_onlyMinPrice_hasPriceRangeTrue() {
        FilterCriteria criteria = FilterCriteria.builder()
                .priceRange(new BigDecimal("100000"), null)
                .build();

        assertTrue(criteria.hasPriceRange());
        assertNull(criteria.getMaxPrice());
    }

    @Test
    @DisplayName("build: chỉ set maxPrice → hasPriceRange() true")
    void build_onlyMaxPrice_hasPriceRangeTrue() {
        FilterCriteria criteria = FilterCriteria.builder()
                .priceRange(null, new BigDecimal("1000000"))
                .build();

        assertTrue(criteria.hasPriceRange());
        assertNull(criteria.getMinPrice());
    }

    // ─── sellerId ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("build: set sellerId → hasSellerId() true")
    void build_withSellerId_hasSellerIdTrue() {
        FilterCriteria criteria = FilterCriteria.builder()
                .sellerId(42)
                .build();

        assertTrue(criteria.hasSellerId());
        assertEquals(42, criteria.getSellerId());
    }

    // ─── timeRange ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("build: set timeRange → hasTimeRange() true")
    void build_withTimeRange_hasTimeRangeTrue() {
        LocalDateTime from   = LocalDateTime.now().minusDays(7);
        LocalDateTime before = LocalDateTime.now();

        FilterCriteria criteria = FilterCriteria.builder()
                .timeRange(from, before)
                .build();

        assertTrue(criteria.hasTimeRange());
        assertEquals(from,   criteria.getStartFrom());
        assertEquals(before, criteria.getEndBefore());
    }

    // ─── sort ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("build: set sortBy endTime ASC → hasSort() true, giá trị đúng")
    void build_withSort_hasSortTrue() {
        FilterCriteria criteria = FilterCriteria.builder()
                .sortBy("endTime", true)
                .build();

        assertTrue(criteria.hasSort());
        assertEquals("endTime", criteria.getSortBy());
        assertTrue(criteria.isSortAscending());
    }

    @Test
    @DisplayName("build: sortBy rỗng → hasSort() false")
    void build_emptySortBy_hasSortFalse() {
        FilterCriteria criteria = FilterCriteria.builder()
                .sortBy("", true)
                .build();

        assertFalse(criteria.hasSort());
    }

    // ─── activeOnly ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("build: activeOnly=true → isActiveOnly() true")
    void build_activeOnly_returnsTrue() {
        FilterCriteria criteria = FilterCriteria.builder()
                .activeOnly(true)
                .build();

        assertTrue(criteria.isActiveOnly());
    }

    @Test
    @DisplayName("build: mặc định activeOnly = false")
    void build_defaultActiveOnly_returnsFalse() {
        FilterCriteria criteria = FilterCriteria.builder().build();

        assertFalse(criteria.isActiveOnly());
    }

    // ─── Builder chaining ─────────────────────────────────────────────────────

    @Test
    @DisplayName("build: chain nhiều điều kiện → tất cả được set đúng")
    void build_chaining_allFieldsSet() {
        FilterCriteria criteria = FilterCriteria.builder()
                .keyword("laptop")
                .category("Electronics")
                .status(AuctionStatus.ACTIVE)
                .priceRange(new BigDecimal("500000"), new BigDecimal("5000000"))
                .sortBy("price", false)
                .activeOnly(true)
                .build();

        assertTrue(criteria.hasKeyword());
        assertTrue(criteria.hasCategory());
        assertTrue(criteria.hasStatus());
        assertTrue(criteria.hasPriceRange());
        assertTrue(criteria.hasSort());
        assertTrue(criteria.isActiveOnly());
        assertFalse(criteria.isSortAscending()); // DESC
    }
}
