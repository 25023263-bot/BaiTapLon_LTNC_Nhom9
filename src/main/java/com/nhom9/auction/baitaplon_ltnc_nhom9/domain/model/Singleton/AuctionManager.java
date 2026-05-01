package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Singleton;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Auction;

import java.util.*;

public class AuctionManager {
    private static AuctionManager instance;
    private final Map<String, Auction> auctions;

    private AuctionManager() {
        auctions = new HashMap<>();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void addAuction(Auction auction) {
        auctions.put(auction.getAuctionId(), auction);
    }

    public Auction getAuction(String id) {
        return auctions.get(id);
    }

    public List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions.values());
    }
}
