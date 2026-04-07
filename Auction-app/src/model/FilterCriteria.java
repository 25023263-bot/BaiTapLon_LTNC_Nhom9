package model;

import model.enums.AuctionStatus;

public class FilterCriteria {
    private String nameContains;
    private Double minPrice;
    private Double maxPrice;
    private AuctionStatus status;
    private Integer sellerId;
    private Boolean endingSoon; // if true, endTime is within next hour
    private String category; // only for PhysicalItem

    private FilterCriteria(Builder builder) {
        this.nameContains = builder.nameContains;
        this.minPrice = builder.minPrice;
        this.maxPrice = builder.maxPrice;
        this.status = builder.status;
        this.sellerId = builder.sellerId;
        this.endingSoon = builder.endingSoon;
        this.category = builder.category;
    }

    public static class Builder {
        private String nameContains;
        private Double minPrice;
        private Double maxPrice;
        private AuctionStatus status;
        private Integer sellerId;
        private Boolean endingSoon;
        private String category;

        public Builder nameContains(String nameContains) {
            this.nameContains = nameContains;
            return this;
        }
        public Builder minPrice(double minPrice) {
            this.minPrice = minPrice;
            return this;
        }
        public Builder maxPrice(double maxPrice) {
            this.maxPrice = maxPrice;
            return this;
        }
        public Builder status(AuctionStatus status) {
            this.status = status;
            return this;
        }
        public Builder sellerId(int sellerId) {
            this.sellerId = sellerId;
            return this;
        }
        public Builder endingSoon(boolean endingSoon) {
            this.endingSoon = endingSoon;
            return this;
        }
        public Builder category(String category) {
            this.category = category;
            return this;
        }
        public FilterCriteria build() {
            return new FilterCriteria(this);
        }
    }

    // Getters
    public String getNameContains() { return nameContains; }
    public Double getMinPrice() { return minPrice; }
    public Double getMaxPrice() { return maxPrice; }
    public AuctionStatus getStatus() { return status; }
    public Integer getSellerId() { return sellerId; }
    public Boolean getEndingSoon() { return endingSoon; }
    public String getCategory() { return category; }
}