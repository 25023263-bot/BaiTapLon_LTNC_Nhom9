package model;

public class PhysicalItem extends AuctionItem {
    private double weight;
    private String dimensions;
    private String category;

    public PhysicalItem(int id, String name, String description, double startingPrice,
                        double minBidStep, User seller, double weight, String dimensions, String category) {
        super(id, name, description, startingPrice, minBidStep, seller);
        this.weight = weight;
        this.dimensions = dimensions;
        this.category = category;
    }

    // Additional getters and setters
    public double getWeight() { return weight; }
    public String getDimensions() { return dimensions; }
    public String getCategory() { return category; }
}