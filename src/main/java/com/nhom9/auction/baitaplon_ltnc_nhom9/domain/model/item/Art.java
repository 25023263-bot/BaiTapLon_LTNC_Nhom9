package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item;

public class Art extends Item {
    public Art(String itemId, String name, double startingPrice, String description) {
        super(itemId, name, startingPrice, description);
    }

    @Override
    public String getCategory() {
        return "Art";
    }
}