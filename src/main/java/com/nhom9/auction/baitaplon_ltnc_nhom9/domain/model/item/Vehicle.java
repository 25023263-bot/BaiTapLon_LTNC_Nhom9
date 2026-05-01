package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item;

public class Vehicle extends Item {
    public Vehicle(String itemId, String name, double startingPrice, String description) {
        super(itemId, name, startingPrice, description);
    }

    @Override
    public String getCategory() {
        return "Vehicle";
    }
}
