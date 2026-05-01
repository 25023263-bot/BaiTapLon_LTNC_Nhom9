package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item;

public abstract class Item {
    protected String itemId;
    protected String name;
    protected double startingPrice;
    protected String description;

    public Item(String itemId, String name, double startingPrice, String description) {
        this.itemId = itemId;
        this.name = name;
        this.startingPrice = startingPrice;
        this.description = description;
    }

    public abstract String getCategory();

    // Getters
    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public double getStartingPrice() { return startingPrice; }
    public String getDescription() { return description; }
}