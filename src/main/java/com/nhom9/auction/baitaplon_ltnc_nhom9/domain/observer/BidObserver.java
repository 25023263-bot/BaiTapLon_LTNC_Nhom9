package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.observer;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Auction;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;

public interface BidObserver {
    void onBidPlaced(Bid bid, Auction auction);
}