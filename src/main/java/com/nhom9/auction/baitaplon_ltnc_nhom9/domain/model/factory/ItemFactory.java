package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.factory;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.Art;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.Electronics;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.Item;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.Vehicle;

public class ItemFactory {
    public static Item createItem(String type, String itemId, String name,
                                  double startingPrice, String description) {
        return switch (type.toLowerCase()) {
            case "electronics" -> new Electronics(itemId, name, startingPrice, description);
            case "art" -> new Art(itemId, name, startingPrice, description);
            case "vehicle" -> new Vehicle(itemId, name, startingPrice, description);
            default -> throw new IllegalArgumentException("Unknown item type: " + type);
        };
    }
}
