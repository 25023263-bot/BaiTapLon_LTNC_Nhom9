package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho Bid – đặc biệt là logic auto-bid.
 *
 * Logic auto-bid là phần phức tạp và quan trọng nhất của hệ thống:
 * - canAutoBidUp(): kiểm tra người dùng còn đủ limit để counter không
 * - calculateAutoBidAmount(): tính mức bid tiếp theo (rivalBid + increment, max = autoBidLimit)
 */
@DisplayName("Bid – Logic Auto-bid")
class BidTest {

    private Bid autoBid;     // Bid có auto-bid với limit = 500K
    private Bid manualBid;   // Bid thủ công không có auto-bid

    @BeforeEach
    void setUp() {
        // Auto-bid: limit 500K
        autoBid = new Bid(
                1, 10, 42, "alice",
                new BigDecimal("200000"),    // bid lần đầu ở 200K
                LocalDateTime.now(),
                true,                         // isAutoBid = true
                new BigDecimal("500000")      // autoBidLimit = 500K
        );

        // Bid thủ công (không auto)
        manualBid = new Bid(
                2, 10, 99, "bob",
                new BigDecimal("250000"),
                LocalDateTime.now(),
                false,   // isAutoBid = false
                null     // không có limit
        );
    }

    // ─── canAutoBidUp() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("canAutoBidUp: autoBid=true, limit đủ để cover mức mới → true")
    void canAutoBidUp_autoBidEnabledAndLimitSufficient_returnsTrue() {
        // Cần bid lên 300K, limit là 500K → còn đủ
        assertTrue(autoBid.canAutoBidUp(new BigDecimal("300000")));
    }

    @Test
    @DisplayName("canAutoBidUp: autoBid=true, mức mới đúng bằng limit → true (biên)")
    void canAutoBidUp_exactlyAtLimit_returnsTrue() {
        // 500K đúng bằng limit → vẫn cho phép
        assertTrue(autoBid.canAutoBidUp(new BigDecimal("500000")));
    }

    @Test
    @DisplayName("canAutoBidUp: autoBid=true, mức mới vượt limit → false")
    void canAutoBidUp_exceedsLimit_returnsFalse() {
        // 501K > 500K limit → không thể counter
        assertFalse(autoBid.canAutoBidUp(new BigDecimal("501000")));
    }

    @Test
    @DisplayName("canAutoBidUp: autoBid=false → luôn false (bid thủ công)")
    void canAutoBidUp_manualBid_returnsFalse() {
        assertFalse(manualBid.canAutoBidUp(new BigDecimal("100000")));
    }

    @Test
    @DisplayName("canAutoBidUp: autoBid=true nhưng autoBidLimit=null → false")
    void canAutoBidUp_autoBidEnabledButNullLimit_returnsFalse() {
        Bid noLimitAutoBid = new Bid(
                3, 10, 77, "charlie",
                new BigDecimal("100000"),
                LocalDateTime.now(),
                true,   // auto-bid bật
                null    // nhưng không có limit → trường hợp lỗi dữ liệu
        );

        assertFalse(noLimitAutoBid.canAutoBidUp(new BigDecimal("200000")));
    }

    // ─── calculateAutoBidAmount() ─────────────────────────────────────────────

    @Test
    @DisplayName("calculateAutoBidAmount: counter thành công → trả về rivalBid + increment")
    void calculateAutoBidAmount_canCounter_returnsRivalPlusIncrement() {
        BigDecimal increment  = new BigDecimal("10000");
        BigDecimal rivalBid   = new BigDecimal("300000");

        // Mức counter = 300K + 10K = 310K (< limit 500K → ok)
        BigDecimal result = autoBid.calculateAutoBidAmount(increment, rivalBid);

        assertEquals(new BigDecimal("310000"), result);
    }

    @Test
    @DisplayName("calculateAutoBidAmount: counter vượt limit → trả về null (không thể counter)")
    void calculateAutoBidAmount_exceedsLimit_returnsNull() {
        BigDecimal increment = new BigDecimal("10000");
        BigDecimal rivalBid  = new BigDecimal("495000");

        // Cần 495K + 10K = 505K, nhưng limit chỉ 500K → không thể counter
        BigDecimal result = autoBid.calculateAutoBidAmount(increment, rivalBid);

        assertNull(result);
    }

    @Test
    @DisplayName("calculateAutoBidAmount: rivalBid + increment = limit → trả về limit (biên)")
    void calculateAutoBidAmount_exactlyAtLimit_returnsLimit() {
        BigDecimal increment = new BigDecimal("10000");
        BigDecimal rivalBid  = new BigDecimal("490000");

        // 490K + 10K = 500K = limit → cho phép
        BigDecimal result = autoBid.calculateAutoBidAmount(increment, rivalBid);

        assertEquals(new BigDecimal("500000"), result);
    }

    @Test
    @DisplayName("calculateAutoBidAmount: bid thủ công → luôn trả về null")
    void calculateAutoBidAmount_manualBid_returnsNull() {
        BigDecimal result = manualBid.calculateAutoBidAmount(
                new BigDecimal("10000"), new BigDecimal("100000"));

        assertNull(result);
    }

    @Test
    @DisplayName("calculateAutoBidAmount: autoBidLimit=null → trả về null")
    void calculateAutoBidAmount_nullLimit_returnsNull() {
        Bid noLimit = new Bid(4, 10, 88, "dave",
                new BigDecimal("100000"), LocalDateTime.now(), true, null);

        BigDecimal result = noLimit.calculateAutoBidAmount(
                new BigDecimal("10000"), new BigDecimal("200000"));

        assertNull(result);
    }

    // ─── Constructor & Basic fields ───────────────────────────────────────────

    @Test
    @DisplayName("constructor 3 tham số: autoBid mặc định = false")
    void shortConstructor_autoBidDefaultsFalse() {
        Bid simpleBid = new Bid(10, 42, new BigDecimal("100000"));

        assertFalse(simpleBid.isAutoBid());
        assertNull(simpleBid.getAutoBidLimit());
    }

    @Test
    @DisplayName("constructor 3 tham số: bidTime được tự động set")
    void shortConstructor_bidTimeIsSetAutomatically() {
        Bid simpleBid = new Bid(10, 42, new BigDecimal("100000"));

        assertNotNull(simpleBid.getBidTime());
    }

    @Test
    @DisplayName("equals: hai Bid cùng id → bằng nhau")
    void equals_samId_returnsTrue() {
        Bid bid1 = new Bid(1, 10, 42, "alice",
                new BigDecimal("100000"), LocalDateTime.now(), false, null);
        Bid bid2 = new Bid(1, 99, 77, "bob",
                new BigDecimal("999999"), LocalDateTime.now(), true, new BigDecimal("1000000"));

        // Hai bid cùng id=1 → equals theo contract
        assertEquals(bid1, bid2);
    }

    @Test
    @DisplayName("equals: hai Bid khác id → không bằng nhau")
    void equals_differentId_returnsFalse() {
        Bid bid1 = new Bid(1, 10, 42, "alice",
                new BigDecimal("100000"), LocalDateTime.now(), false, null);
        Bid bid2 = new Bid(2, 10, 42, "alice",
                new BigDecimal("100000"), LocalDateTime.now(), false, null);

        assertNotEquals(bid1, bid2);
    }
}
