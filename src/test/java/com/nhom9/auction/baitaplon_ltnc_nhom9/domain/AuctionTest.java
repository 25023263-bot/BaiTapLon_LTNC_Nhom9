package com.nhom9.auction.baitaplon_ltnc_nhom9.domain;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Auction;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.factory.ItemFactory;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.Item;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.InvalidBidException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.AuctionClosedException;

import org.junit.jupiter.api.*;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    Auction auction;
    Item item;
    Buyer bidder1, bidder2;

    @BeforeEach
    void setUp() {
        item = ItemFactory.createItem("electronics", "1", "Laptop", 100, "new");

        auction = new Auction(item, new Date(), new Date(System.currentTimeMillis() + 3600000));

        auction.start();

        bidder1 = new Buyer("u1", "Alice", "pass");
        bidder2 = new Buyer("u2", "Bob", "pass");
    }

    // Test bid hợp lệ
    @Test
    void testValidBid() {
        Bid bid = new Bid(bidder1, 150);

        assertDoesNotThrow(() -> auction.placeBid(bid));

        assertEquals(150, auction.getCurrentHighestBid());
    }

    // Test bid thấp hơn giá hiện tại
    @Test
    void testInvalidLowBid() {
        Bid bid = new Bid(bidder1, 50);

        assertThrows(
                InvalidBidException.class,
                () -> auction.placeBid(bid)
        );
    }

    // Test bid bằng giá hiện tại
    @Test
    void testEqualBidRejected() {
        Bid bid = new Bid(bidder1, 100);

        assertThrows(
                InvalidBidException.class,
                () -> auction.placeBid(bid)
        );
    }

    // Test bid khi auction đã kết thúc
    @Test
    void testAuctionFinishedBid() {
        auction.finish();

        Bid bid = new Bid(bidder2, 200);

        assertThrows(
                AuctionClosedException.class,
                () -> auction.placeBid(bid)
        );
    }

    // Test cập nhật người thắng sau bid hợp lệ
    @Test
    void testHighestBuyerUpdated() throws Exception {
        Bid bid = new Bid(bidder1, 200);

        auction.placeBid(bid);

        assertEquals(bidder1, auction.getHighestBuyer());
    }

    // Test nhiều người bid liên tiếp
    @Test
    void testHigherBidWins() throws Exception {

        auction.placeBid(new Bid(bidder1, 150));

        auction.placeBid(new Bid(bidder2, 200));

        assertEquals(200, auction.getCurrentHighestBid());

        assertEquals(bidder2, auction.getHighestBuyer());
    }

    // Test bid khi auction chưa start
    @Test
    void testBidBeforeAuctionStarts() {

        Auction newAuction = new Auction(
                item,
                new Date(),
                new Date(System.currentTimeMillis() + 3600000)
        );

        Bid bid = new Bid(bidder1, 150);

        assertThrows(
                AuctionClosedException.class,
                () -> newAuction.placeBid(bid)
        );
    }
}