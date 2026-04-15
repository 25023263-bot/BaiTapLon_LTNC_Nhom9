package org.example.baitaplon_ltnc_nhom9.service;

import org.example.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;

public interface AuctionObserver {
    void update(AuctionItem item);
}