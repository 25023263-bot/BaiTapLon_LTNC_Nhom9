package service;

import model.AuctionItem;

public interface AuctionObserver {
    void update(AuctionItem item);
}