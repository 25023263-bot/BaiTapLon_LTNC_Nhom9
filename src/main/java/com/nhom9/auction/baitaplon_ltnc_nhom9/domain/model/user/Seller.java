package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;

public class Seller extends User {
    public Seller(String userId, String username, String password) {
        super(userId, username, password, UserRole.SELLER);
    }
}