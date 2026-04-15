package org.example.baitaplon_ltnc_nhom9.service;

public class PasswordHasher {
    public static String hash(String plain) {
        return Integer.toHexString(plain.hashCode());
    }
}