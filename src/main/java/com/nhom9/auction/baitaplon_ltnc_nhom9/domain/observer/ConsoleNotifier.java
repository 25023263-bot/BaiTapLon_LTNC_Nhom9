package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.observer;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Auction;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;

public class ConsoleNotifier implements BidObserver {
    @Override
    public void onBidPlaced(Bid bid, Auction auction) {
        System.out.printf("[NOTIFY] New bid: %.2f by %s on item %s (Auction %s)%n",
                bid.getAmount(),
                bid.getBuyer().getUsername(),
                auction.getItem().getName(),
                auction.getAuctionId());
    }
}
