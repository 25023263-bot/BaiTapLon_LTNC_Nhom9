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

        bidder1 = new Buyer("u1", "alice", "pass");
        bidder2 = new Buyer("u2", "bob", "pass");
    }

    @Test
    void testValidBid() {
        Bid bid = new Bid(bidder1, 150);

        assertDoesNotThrow(() -> auction.placeBid(bid));

        assertEquals(150, auction.getCurrentHighestBid());
        assertEquals(bidder1, auction.getHighestBuyer());
    }

    @Test
    void testInvalidLowBid() {
        Bid bid = new Bid(bidder1, 50);

        assertThrows(InvalidBidException.class, () -> auction.placeBid(bid));
    }

    @Test
    void testAuctionFinishedBid() {
        auction.finish();

        Bid bid = new Bid(bidder2, 200);

        assertThrows(AuctionClosedException.class, () -> auction.placeBid(bid));
    }
}