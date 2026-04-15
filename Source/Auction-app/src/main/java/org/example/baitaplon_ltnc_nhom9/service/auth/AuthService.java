package org.example.baitaplon_ltnc_nhom9.service;

import org.example.baitaplon_ltnc_nhom9.domain.model.user.User;

public class AuthService {
    private AuctionHouse auctionHouse;

    public AuthService(AuctionHouse auctionHouse) {
        this.auctionHouse = auctionHouse;
    }

    public User login(String email, String password) {
        User user = auctionHouse.getUserByEmail(email);
        if (user != null && user.login(email, password)) {
            return user;
        }
        return null;
    }

    public void logout(User user) {
        if (user != null) {
            user.logout();
        }
    }

    public boolean isLoggedIn(User user) {
        return user != null && user.isLoggedIn();
    }
}