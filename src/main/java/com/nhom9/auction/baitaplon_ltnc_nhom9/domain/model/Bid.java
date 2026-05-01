package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer;

import java.util.Date;
import java.util.UUID;

public class Bid {
    private String bidId;
    private Buyer buyer;
    private double amount;
    private Date timestamp;

    public Bid(Buyer buyer, double amount) {
        this.bidId = UUID.randomUUID().toString();
        this.buyer = buyer;
        this.amount = amount;
        this.timestamp = new Date();
    }

    public String getBidId() { return bidId; }
    public Buyer getBuyer() { return buyer; }
    public double getAmount() { return amount; }
    public Date getTimestamp() { return timestamp; }
}