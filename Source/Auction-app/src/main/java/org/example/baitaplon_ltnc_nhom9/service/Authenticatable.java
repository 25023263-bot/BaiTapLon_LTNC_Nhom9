package org.example.baitaplon_ltnc_nhom9.service;

public interface Authenticatable {
    boolean login(String email, String password);
    void logout();
    boolean isLoggedIn();
    String getSessionToken();
}