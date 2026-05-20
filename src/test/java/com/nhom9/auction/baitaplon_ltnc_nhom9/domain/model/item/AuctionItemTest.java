package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho AuctionItem (dùng PhysicalItem vì AuctionItem là abstract).
 *
 * Lý do dùng PhysicalItem: AuctionItem là abstract class, không thể new trực tiếp.
 * PhysicalItem kế thừa toàn bộ business logic từ AuctionItem.
 */
@DisplayName("AuctionItem – Logic nghiệp vụ đấu giá")
class AuctionItemTest {

    private PhysicalItem activeItem;

    private static final LocalDateTime START    = LocalDateTime.now().minusHours(1);
    private static final LocalDateTime END      = LocalDateTime.now().plusHours(2);
    private static final LocalDateTime PAST_END = LocalDateTime.now().minusMinutes(5);

    @BeforeEach
    void setUp() {
        // PhysicalItem constructor: (id, sellerId, title, description, category,
        //                            startingPrice, minBidIncrement, startTime, endTime)
        activeItem = new PhysicalItem(
                1, 10,
                "Laptop Gaming Test", "Mô tả test", "Electronics",
                new BigDecimal("100000"), new BigDecimal("10000"),
                START, END
        );
        activeItem.setStatus(AuctionStatus.ACTIVE);
    }

    // ─── isValidBid() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("isValidBid: đúng bằng currentPrice + increment → hợp lệ (giá trị biên)")
    void isValidBid_exactMinimum_returnsTrue() {
        // 100.000 + 10.000 = 110.000 → hợp lệ
        assertTrue(activeItem.isValidBid(new BigDecimal("110000")));
    }

    @Test
    @DisplayName("isValidBid: cao hơn minimum → hợp lệ")
    void isValidBid_aboveMinimum_returnsTrue() {
        assertTrue(activeItem.isValidBid(new BigDecimal("200000")));
    }

    @Test
    @DisplayName("isValidBid: dưới minimum 1đ → không hợp lệ (giá trị biên)")
    void isValidBid_justBelowMinimum_returnsFalse() {
        assertFalse(activeItem.isValidBid(new BigDecimal("109999")));
    }

    @Test
    @DisplayName("isValidBid: đúng bằng currentPrice (không tăng) → không hợp lệ")
    void isValidBid_sameAsCurrentPrice_returnsFalse() {
        assertFalse(activeItem.isValidBid(new BigDecimal("100000")));
    }

    @Test
    @DisplayName("isValidBid: 0 → không hợp lệ")
    void isValidBid_zero_returnsFalse() {
        assertFalse(activeItem.isValidBid(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("isValidBid: null → false (không ném NullPointerException)")
    void isValidBid_null_returnsFalse() {
        assertFalse(activeItem.isValidBid(null));
    }

    // ─── updateCurrentBid() ───────────────────────────────────────────────────

    @Test
    @DisplayName("updateCurrentBid: giá và bidderId được cập nhật đúng")
    void updateCurrentBid_updatesCorrectly() {
        activeItem.updateCurrentBid(new BigDecimal("150000"), 42);

        assertEquals(new BigDecimal("150000"), activeItem.getCurrentPrice());
        assertEquals(42, activeItem.getLeadingBidderId());
    }

    @Test
    @DisplayName("updateCurrentBid: gọi nhiều lần → giữ giá trị cuối cùng")
    void updateCurrentBid_calledTwice_latestValueWins() {
        activeItem.updateCurrentBid(new BigDecimal("150000"), 42);
        activeItem.updateCurrentBid(new BigDecimal("200000"), 99);

        assertEquals(new BigDecimal("200000"), activeItem.getCurrentPrice());
        assertEquals(99, activeItem.getLeadingBidderId());
    }

    // ─── getNextMinimumBid() ──────────────────────────────────────────────────

    @Test
    @DisplayName("getNextMinimumBid: ban đầu = startingPrice + increment")
    void getNextMinimumBid_initialState_correctValue() {
        assertEquals(new BigDecimal("110000"), activeItem.getNextMinimumBid());
    }

    @Test
    @DisplayName("getNextMinimumBid: sau bid mới → tính lại từ currentPrice mới")
    void getNextMinimumBid_afterBidUpdate_recalculated() {
        activeItem.updateCurrentBid(new BigDecimal("250000"), 5);
        assertEquals(new BigDecimal("260000"), activeItem.getNextMinimumBid());
    }

    // ─── hasBids() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("hasBids: chưa có bid (leadingBidderId = 0) → false")
    void hasBids_noBids_returnsFalse() {
        assertFalse(activeItem.hasBids());
    }

    @Test
    @DisplayName("hasBids: đã có bid → true")
    void hasBids_afterBid_returnsTrue() {
        activeItem.updateCurrentBid(new BigDecimal("150000"), 7);
        assertTrue(activeItem.hasBids());
    }

    // ─── isActive() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("isActive: status = ACTIVE → true")
    void isActive_activeStatus_returnsTrue() {
        assertTrue(activeItem.isActive());
    }

    @Test
    @DisplayName("isActive: status = PENDING → false")
    void isActive_pendingStatus_returnsFalse() {
        activeItem.setStatus(AuctionStatus.PENDING);
        assertFalse(activeItem.isActive());
    }

    @Test
    @DisplayName("isActive: status = CLOSED → false")
    void isActive_closedStatus_returnsFalse() {
        activeItem.setStatus(AuctionStatus.CLOSED);
        assertFalse(activeItem.isActive());
    }

    // ─── getRemainingSeconds() ────────────────────────────────────────────────

    @Test
    @DisplayName("getRemainingSeconds: còn thời gian → > 0")
    void getRemainingSeconds_notExpired_returnsPositive() {
        assertTrue(activeItem.getRemainingSeconds() > 0);
    }

    @Test
    @DisplayName("getRemainingSeconds: đã hết giờ → trả về 0 (không âm)")
    void getRemainingSeconds_expired_returnsZero() {
        PhysicalItem expiredItem = new PhysicalItem(
                3, 10, "Expired Item", "desc", "cat",
                new BigDecimal("100000"), new BigDecimal("10000"),
                START, PAST_END // endTime đã qua
        );
        assertEquals(0L, expiredItem.getRemainingSeconds());
    }

    @Test
    @DisplayName("getRemainingSeconds: endTime = null → trả về 0")
    void getRemainingSeconds_nullEndTime_returnsZero() {
        PhysicalItem item = new PhysicalItem();
        item.setEndTime(null);
        assertEquals(0L, item.getRemainingSeconds());
    }

    // ─── PhysicalItem.getItemType() / isValidItem() ───────────────────────────

    @Test
    @DisplayName("getItemType → 'PHYSICAL'")
    void getItemType_returnsPhysical() {
        assertEquals("PHYSICAL", activeItem.getItemType());
    }

    @Test
    @DisplayName("isValidItem: item đầy đủ thông tin → true")
    void isValidItem_validData_returnsTrue() {
        assertTrue(activeItem.isValidItem());
    }

    @Test
    @DisplayName("isValidItem: title null → false")
    void isValidItem_nullTitle_returnsFalse() {
        PhysicalItem item = new PhysicalItem(
                5, 10, null, "desc", "cat",
                new BigDecimal("100000"), new BigDecimal("10000"),
                START, END
        );
        assertFalse(item.isValidItem());
    }

    @Test
    @DisplayName("isValidItem: title rỗng → false")
    void isValidItem_emptyTitle_returnsFalse() {
        PhysicalItem item = new PhysicalItem(
                5, 10, "   ", "desc", "cat",
                new BigDecimal("100000"), new BigDecimal("10000"),
                START, END
        );
        assertFalse(item.isValidItem());
    }

    @Test
    @DisplayName("isValidItem: startingPrice = 0 → false")
    void isValidItem_zeroStartingPrice_returnsFalse() {
        PhysicalItem item = new PhysicalItem(
                6, 10, "Item", "desc", "cat",
                BigDecimal.ZERO, new BigDecimal("10000"),
                START, END
        );
        assertFalse(item.isValidItem());
    }

    @Test
    @DisplayName("isValidItem: endTime trước startTime → false")
    void isValidItem_endBeforeStart_returnsFalse() {
        PhysicalItem item = new PhysicalItem(
                7, 10, "Item", "desc", "cat",
                new BigDecimal("100000"), new BigDecimal("10000"),
                END, START // đảo ngược: end < start
        );
        assertFalse(item.isValidItem());
    }

    // ─── equals / hashCode ────────────────────────────────────────────────────

    @Test
    @DisplayName("equals: cùng id → bằng nhau")
    void equals_sameId_returnsTrue() {
        PhysicalItem other = new PhysicalItem(
                1, 99, "Other Title", "desc", "cat",
                new BigDecimal("999"), new BigDecimal("1"),
                START, END
        );
        assertEquals(activeItem, other);
    }

    @Test
    @DisplayName("equals: khác id → không bằng nhau")
    void equals_differentId_returnsFalse() {
        PhysicalItem other = new PhysicalItem(
                99, 10, "Laptop", "desc", "cat",
                new BigDecimal("100000"), new BigDecimal("10000"),
                START, END
        );
        assertNotEquals(activeItem, other);
    }
}
