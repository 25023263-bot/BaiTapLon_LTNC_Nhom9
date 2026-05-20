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
 * Lý do test PhysicalItem thay vì AuctionItem:
 * AuctionItem là abstract class → không thể khởi tạo trực tiếp.
 * PhysicalItem kế thừa toàn bộ logic của AuctionItem.
 * Các test isValidBid, updateCurrentBid... đang test phần của AuctionItem.
 */
@DisplayName("AuctionItem – Logic nghiệp vụ đấu giá")
class AuctionItemTest {

    private PhysicalItem activeItem;
    private PhysicalItem pendingItem;

    private static final LocalDateTime NOW      = LocalDateTime.now();
    private static final LocalDateTime START    = NOW.minusHours(1);
    private static final LocalDateTime END      = NOW.plusHours(2);
    private static final LocalDateTime PAST_END = NOW.minusMinutes(5);

    @BeforeEach
    void setUp() {
        activeItem = new PhysicalItem(
                1, 10,
                "Laptop Gaming Test", "Mô tả test", "Electronics",
                new BigDecimal("100000"), new BigDecimal("10000"),
                START, END
        );
        activeItem.setStatus(AuctionStatus.ACTIVE);

        pendingItem = new PhysicalItem(
                2, 10,
                "Item Pending", "Mô tả", "Category",
                new BigDecimal("50000"), new BigDecimal("5000"),
                NOW.plusHours(1), NOW.plusHours(3)
        );
    }

    // ─── isValidBid() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("isValidBid: bid đúng bằng currentPrice + increment → hợp lệ (biên)")
    void isValidBid_exactMinimum_returnsTrue() {
        assertTrue(activeItem.isValidBid(new BigDecimal("110000")));
    }

    @Test
    @DisplayName("isValidBid: bid cao hơn mức tối thiểu → hợp lệ")
    void isValidBid_aboveMinimum_returnsTrue() {
        assertTrue(activeItem.isValidBid(new BigDecimal("200000")));
    }

    @Test
    @DisplayName("isValidBid: bid thấp hơn mức tối thiểu 1đ → không hợp lệ (biên)")
    void isValidBid_justBelowMinimum_returnsFalse() {
        assertFalse(activeItem.isValidBid(new BigDecimal("109999")));
    }

    @Test
    @DisplayName("isValidBid: bid bằng currentPrice (không tăng increment) → không hợp lệ")
    void isValidBid_sameAsCurrentPrice_returnsFalse() {
        assertFalse(activeItem.isValidBid(new BigDecimal("100000")));
    }

    @Test
    @DisplayName("isValidBid: bid = 0 → không hợp lệ")
    void isValidBid_zero_returnsFalse() {
        assertFalse(activeItem.isValidBid(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("isValidBid: null → không hợp lệ (không ném exception)")
    void isValidBid_null_returnsFalse() {
        assertFalse(activeItem.isValidBid(null));
    }

    // ─── updateCurrentBid() ───────────────────────────────────────────────────

    @Test
    @DisplayName("updateCurrentBid: giá và bidderId được cập nhật đúng")
    void updateCurrentBid_validBid_priceAndBidderUpdated() {
        activeItem.updateCurrentBid(new BigDecimal("150000"), 42);
        assertEquals(new BigDecimal("150000"), activeItem.getCurrentPrice());
        assertEquals(42, activeItem.getLeadingBidderId());
    }

    @Test
    @DisplayName("updateCurrentBid: gọi liên tiếp → giá trị cuối cùng được giữ lại")
    void updateCurrentBid_calledTwice_latestValueKept() {
        activeItem.updateCurrentBid(new BigDecimal("150000"), 42);
        activeItem.updateCurrentBid(new BigDecimal("200000"), 99);
        assertEquals(new BigDecimal("200000"), activeItem.getCurrentPrice());
        assertEquals(99, activeItem.getLeadingBidderId());
    }

    // ─── getNextMinimumBid() ──────────────────────────────────────────────────

    @Test
    @DisplayName("getNextMinimumBid: lần đầu = currentPrice + increment")
    void getNextMinimumBid_initialState_returnsStartingPluIncrement() {
        assertEquals(new BigDecimal("110000"), activeItem.getNextMinimumBid());
    }

    @Test
    @DisplayName("getNextMinimumBid: sau khi có bid mới → tính lại từ currentPrice mới")
    void getNextMinimumBid_afterBidUpdate_recalculated() {
        activeItem.updateCurrentBid(new BigDecimal("250000"), 5);
        assertEquals(new BigDecimal("260000"), activeItem.getNextMinimumBid());
    }

    // ─── hasBids() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("hasBids: chưa có bid nào → false")
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
        assertFalse(pendingItem.isActive());
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
                START, PAST_END
        );
        assertEquals(0L, expiredItem.getRemainingSeconds());
    }

    // ─── getItemType() / isValidItem() ───────────────────────────────────────

    @Test
    @DisplayName("PhysicalItem.getItemType() → trả về 'PHYSICAL'")
    void getItemType_physicalItem_returnsPhysical() {
        assertEquals("PHYSICAL", activeItem.getItemType());
    }

    @Test
    @DisplayName("isValidItem: item đầy đủ thông tin → true")
    void isValidItem_validData_returnsTrue() {
        assertTrue(activeItem.isValidItem());
    }

    @Test
    @DisplayName("isValidItem: title rỗng → false")
    void isValidItem_emptyTitle_returnsFalse() {
        PhysicalItem noTitle = new PhysicalItem(
                5, 10, "", "desc", "cat",
                new BigDecimal("100000"), new BigDecimal("10000"),
                START, END
        );
        assertFalse(noTitle.isValidItem());
    }

    @Test
    @DisplayName("isValidItem: startingPrice = 0 → false")
    void isValidItem_zeroStartingPrice_returnsFalse() {
        PhysicalItem freeItem = new PhysicalItem(
                6, 10, "Free Item", "desc", "cat",
                BigDecimal.ZERO, new BigDecimal("10000"),
                START, END
        );
        assertFalse(freeItem.isValidItem());
    }

    @Test
    @DisplayName("isValidItem: endTime trước startTime → false")
    void isValidItem_endBeforeStart_returnsFalse() {
        PhysicalItem badTime = new PhysicalItem(
                7, 10, "Bad Time Item", "desc", "cat",
                new BigDecimal("100000"), new BigDecimal("10000"),
                END, START   // đảo ngược: end < start
        );
        assertFalse(badTime.isValidItem());
    }
}
