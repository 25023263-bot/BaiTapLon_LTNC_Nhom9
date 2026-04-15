package service.auction;

import org.example.baitaplon_ltnc_nhom9.model.User;
import org.example.baitaplon_ltnc_nhom9.model.enums.AuctionStatus;

public interface Auctionable {
    void placeBid(User bidder, double amount) throws Exception;
    double getCurrentPrice();
    void closeAuction();
    AuctionStatus getStatus();
}