package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho Bid domain model.
 *
 * Bid là đối tượng trung tâm của hệ thống đấu giá.
 * Test đảm bảo các constructor, getter/setter và equals hoạt động đúng.
 */
@DisplayName("Bid – Domain Model")
class BidTest {

    // ─── Constructor 3 tham số (dùng khi tạo bid mới) ────────────────────────

    @Test
    @DisplayName("Constructor 3-arg: auctionId, buyerId, amount được lưu đúng")
    void constructor3Arg_storesFields() {
        Bid bid = new Bid(10, 5, new BigDecimal("500000"));

        assertEquals(10, bid.getAuctionId());
        assertEquals(5,  bid.getBuyerId());
        assertEquals(new BigDecimal("500000"), bid.getAmount());
    }

    @Test
    @DisplayName("Constructor 3-arg: bidTime được set tự động (không null)")
    void constructor3Arg_bidTimeSetAutomatically() {
        Bid bid = new Bid(1, 2, new BigDecimal("100000"));

        assertNotNull(bid.getBidTime());
        // bidTime phải gần với thời điểm hiện tại (trong vòng 5 giây)
        assertTrue(bid.getBidTime().isAfter(LocalDateTime.now().minusSeconds(5)));
    }

    @Test
    @DisplayName("Constructor 3-arg: autoBid mặc định là false")
    void constructor3Arg_autoBidDefaultFalse() {
        Bid bid = new Bid(1, 2, new BigDecimal("100000"));

        assertFalse(bid.isAutoBid());
    }

    @Test
    @DisplayName("Constructor 3-arg: autoBidLimit mặc định là null")
    void constructor3Arg_autoBidLimitDefaultNull() {
        Bid bid = new Bid(1, 2, new BigDecimal("100000"));

        assertNull(bid.getAutoBidLimit());
    }

    // ─── Constructor 8 tham số (dùng khi load từ DB) ─────────────────────────

    @Test
    @DisplayName("Constructor 8-arg: tất cả field được lưu đúng")
    void constructor8Arg_storesAllFields() {
        LocalDateTime bidTime = LocalDateTime.now().minusMinutes(10);
        BigDecimal amount = new BigDecimal("750000");
        BigDecimal limit  = new BigDecimal("2000000");

        Bid bid = new Bid(99, 10, 5, "alice", amount, bidTime, true, limit);

        assertEquals(99,      bid.getId());
        assertEquals(10,      bid.getAuctionId());
        assertEquals(5,       bid.getBuyerId());
        assertEquals("alice", bid.getBuyerUsername());
        assertEquals(amount,  bid.getAmount());
        assertEquals(bidTime, bid.getBidTime());
        assertTrue(bid.isAutoBid());
        assertEquals(limit,   bid.getAutoBidLimit());
    }

    // ─── setAmount() — dùng trong resolveAutoBidConflict ─────────────────────

    @Test
    @DisplayName("setAmount: có thể thay đổi amount sau khi tạo")
    void setAmount_updatesCorrectly() {
        Bid bid = new Bid(1, 2, new BigDecimal("100000"));
        bid.setAmount(new BigDecimal("999000"));

        assertEquals(new BigDecimal("999000"), bid.getAmount());
    }

    // ─── autoBid fields ───────────────────────────────────────────────────────

    @Test
    @DisplayName("setAutoBid/setAutoBidLimit: đặt auto-bid cho Bid")
    void autoBidFields_setCorrectly() {
        Bid bid = new Bid(1, 2, new BigDecimal("100000"));
        bid.setAutoBid(true);
        bid.setAutoBidLimit(new BigDecimal("5000000"));

        assertTrue(bid.isAutoBid());
        assertEquals(new BigDecimal("5000000"), bid.getAutoBidLimit());
    }

    // ─── setBuyerUsername ─────────────────────────────────────────────────────

    @Test
    @DisplayName("setBuyerUsername: lưu và đọc đúng")
    void setBuyerUsername_storesCorrectly() {
        Bid bid = new Bid(1, 2, new BigDecimal("100000"));
        bid.setBuyerUsername("nguyen_van_a");

        assertEquals("nguyen_van_a", bid.getBuyerUsername());
    }

    // ─── equals / hashCode ────────────────────────────────────────────────────

    @Test
    @DisplayName("equals: cùng id → bằng nhau")
    void equals_sameId_returnsTrue() {
        Bid b1 = new Bid(1, 10, 5, "alice", new BigDecimal("100"), LocalDateTime.now(), false, null);
        Bid b2 = new Bid(1, 99, 88, "bob",  new BigDecimal("999"), LocalDateTime.now(), true,  new BigDecimal("9999"));

        assertEquals(b1, b2, "Hai Bid cùng id phải bằng nhau");
    }

    @Test
    @DisplayName("equals: id = 0 (chưa lưu DB) với id = 0 khác → bằng nhau theo id")
    void equals_bothIdZero_returnsTrue() {
        Bid b1 = new Bid(1, 2, new BigDecimal("100000"));
        Bid b2 = new Bid(1, 2, new BigDecimal("200000")); // khác amount
        // cả 2 đều chưa set id → id = 0

        assertEquals(b1, b2);
    }

    @Test
    @DisplayName("hashCode: cùng id → cùng hashCode")
    void hashCode_sameId_sameHash() {
        Bid b1 = new Bid(5, 10, 1, "a", BigDecimal.ONE, LocalDateTime.now(), false, null);
        Bid b2 = new Bid(5, 20, 2, "b", BigDecimal.TEN, LocalDateTime.now(), true,  BigDecimal.TEN);

        assertEquals(b1.hashCode(), b2.hashCode());
    }

    // ─── toString ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString: không ném exception, chứa thông tin id và amount")
    void toString_containsKeyInfo() {
        Bid bid = new Bid(7, 2, 3, "alice", new BigDecimal("500000"), LocalDateTime.now(), false, null);
        String str = bid.toString();

        assertNotNull(str);
        assertTrue(str.contains("7"));         // id
        assertTrue(str.contains("500000"));    // amount
    }
}
