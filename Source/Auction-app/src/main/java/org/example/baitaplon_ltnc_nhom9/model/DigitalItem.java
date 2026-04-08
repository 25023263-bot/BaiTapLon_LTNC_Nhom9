package org.example.baitaplon_ltnc_nhom9.model;

public class DigitalItem extends AuctionItem {
    private String downloadLink;
    private String licenseKey;

    public DigitalItem(int id, String name, String description, double startingPrice,
                       double minBidStep, User seller, String downloadLink) {
        super(id, name, description, startingPrice, minBidStep, seller);
        this.downloadLink = downloadLink;
    }

    public String getDownloadLink() { return downloadLink; }
    public String getLicenseKey() { return licenseKey; }
    public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }
}