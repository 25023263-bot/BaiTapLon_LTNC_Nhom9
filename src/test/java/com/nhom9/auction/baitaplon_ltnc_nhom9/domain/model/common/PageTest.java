package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho Page – kiểm tra logic phân trang.
 *
 * Phân trang sai là một trong những lỗi khó debug nhất:
 * - Off-by-one (hiển thị 11 item thay vì 12)
 * - Trang cuối tính sai (tổng 13 item, pageSize 12 → phải có 2 trang)
 */
@DisplayName("Page – Logic phân trang")
class PageTest {

    // ─── totalPages ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("totalPages: 12 item, pageSize 12 → 1 trang")
    void totalPages_exactlyOnePage_returnsOne() {
        Page<String> page = new Page<>(makeList(12), 0, 12, 12);

        assertEquals(1, page.getTotalPages());
    }

    @Test
    @DisplayName("totalPages: 13 item, pageSize 12 → 2 trang (ceiling)")
    void totalPages_onePlusRemainder_returnsTwo() {
        // 13 / 12 = 1.08... → ceiling = 2
        Page<String> page = new Page<>(makeList(12), 0, 12, 13);

        assertEquals(2, page.getTotalPages());
    }

    @Test
    @DisplayName("totalPages: 0 item → 0 trang")
    void totalPages_noItems_returnsZero() {
        Page<String> page = new Page<>(Collections.emptyList(), 0, 12, 0);

        assertEquals(0, page.getTotalPages());
    }

    @Test
    @DisplayName("totalPages: 25 item, pageSize 10 → 3 trang")
    void totalPages_twentyFiveItemsPageTen_returnsThree() {
        Page<String> page = new Page<>(makeList(10), 0, 10, 25);

        // 25 / 10 = 2.5 → ceiling = 3
        assertEquals(3, page.getTotalPages());
    }

    // ─── isFirstPage / isLastPage ─────────────────────────────────────────────

    @Test
    @DisplayName("isFirstPage: pageNumber = 0 → true")
    void isFirstPage_pageZero_returnsTrue() {
        Page<String> page = new Page<>(makeList(5), 0, 12, 20);

        assertTrue(page.isFirstPage());
        assertFalse(page.hasPreviousPage());
    }

    @Test
    @DisplayName("isFirstPage: pageNumber = 1 → false")
    void isFirstPage_pageOne_returnsFalse() {
        Page<String> page = new Page<>(makeList(5), 1, 12, 20);

        assertFalse(page.isFirstPage());
        assertTrue(page.hasPreviousPage());
    }

    @Test
    @DisplayName("isLastPage: đang ở trang cuối → true")
    void isLastPage_onLastPage_returnsTrue() {
        // 13 item, pageSize 12 → 2 trang. Page 1 (index) là trang cuối.
        Page<String> page = new Page<>(makeList(1), 1, 12, 13);

        assertTrue(page.isLastPage());
        assertFalse(page.hasNextPage());
    }

    @Test
    @DisplayName("isLastPage: chưa đến trang cuối → false")
    void isLastPage_notOnLastPage_returnsFalse() {
        Page<String> page = new Page<>(makeList(12), 0, 12, 13);

        assertFalse(page.isLastPage());
        assertTrue(page.hasNextPage());
    }

    // ─── hasContent ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("hasContent: có item → true")
    void hasContent_withItems_returnsTrue() {
        Page<String> page = new Page<>(makeList(3), 0, 12, 3);

        assertTrue(page.hasContent());
    }

    @Test
    @DisplayName("hasContent: không có item → false")
    void hasContent_empty_returnsFalse() {
        Page<String> page = new Page<>(Collections.emptyList(), 0, 12, 0);

        assertFalse(page.hasContent());
    }

    // ─── getOffset ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOffset: trang 0, pageSize 12 → offset = 0")
    void getOffset_firstPage_returnsZero() {
        Page<String> page = new Page<>(makeList(12), 0, 12, 100);

        assertEquals(0, page.getOffset());
    }

    @Test
    @DisplayName("getOffset: trang 1, pageSize 12 → offset = 12")
    void getOffset_secondPage_returnsTwelve() {
        Page<String> page = new Page<>(makeList(12), 1, 12, 100);

        assertEquals(12, page.getOffset());
    }

    @Test
    @DisplayName("getOffset: trang 2, pageSize 12 → offset = 24")
    void getOffset_thirdPage_returnsTwentyFour() {
        Page<String> page = new Page<>(makeList(12), 2, 12, 100);

        assertEquals(24, page.getOffset());
    }

    // ─── empty factory ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Page.empty: trả về trang rỗng đúng chuẩn")
    void empty_returnsEmptyPage() {
        Page<String> page = Page.empty(12);

        assertFalse(page.hasContent());
        assertEquals(0, page.getTotalElements());
        assertEquals(0, page.getTotalPages());
        assertTrue(page.isFirstPage());
    }

    // ─── null content ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("null content → không crash, content là danh sách rỗng")
    void nullContent_treatedAsEmpty() {
        Page<String> page = new Page<>(null, 0, 12, 0);

        assertFalse(page.hasContent());
        assertNotNull(page.getContent());
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    /** Tạo list giả có n phần tử để test */
    private List<String> makeList(int n) {
        String[] arr = new String[n];
        Arrays.fill(arr, "item");
        return Arrays.asList(arr);
    }
}
