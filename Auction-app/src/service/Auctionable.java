package service;

import model.User;
import model.enums.AuctionStatus;

public interface Auctionable {
    void placeBid(User bidder, double amount) throws Exception;
    double getCurrentPrice();
    void closeAuction();
    AuctionStatus getStatus();
}