package org.example.baitaplon_ltnc_nhom9.service.auction;

import org.example.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import org.example.baitaplon_ltnc_nhom9.domain.model.user.User;

public interface Auctionable {
    void placeBid(User bidder, double amount) throws Exception;
    double getCurrentPrice();
    void closeAuction();
    AuctionStatus getStatus();
}