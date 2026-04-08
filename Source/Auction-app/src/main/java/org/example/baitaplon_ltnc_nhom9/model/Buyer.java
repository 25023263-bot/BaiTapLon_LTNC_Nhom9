package org.example.baitaplon_ltnc_nhom9.model;

import org.example.baitaplon_ltnc_nhom9.model.enums.UserRole;
import java.util.ArrayList;
import java.util.List;

public class Buyer extends User {
    private List<AuctionItem> watchlist;
    private List<Bid> myBids;

    public Buyer(int id, String name, String email, String password) {
        super(id, name, email, password, UserRole.BUYER);
        this.watchlist = new ArrayList<>();
        this.myBids = new ArrayList<>();
    }

    @Override
    public double getDiscount() {
        return 0.0; // Buyer không được giảm phí
    }

    public List<AuctionItem> getWatchlist() {
        return watchlist;
    }

    public void addToWatchlist(AuctionItem item) {
        if (!watchlist.contains(item)) {
            watchlist.add(item);
        }
    }

    public void removeFromWatchlist(AuctionItem item) {
        watchlist.remove(item);
    }

    public List<Bid> getMyBids() {
        return myBids;
    }

    public void addBid(Bid bid) {
        myBids.add(bid);
    }
}