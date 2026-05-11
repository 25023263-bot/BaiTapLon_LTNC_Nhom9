package com.nhom9.auction.baitaplon_ltnc_nhom9.config;

public class AppConfig {
    private AppConfig() {
    }

    public static final String SERVER_BASE_URL =
            System.getProperty("auction.server.baseUrl", "https://jaywalker-marrow-daycare.ngrok-free.dev");
}
