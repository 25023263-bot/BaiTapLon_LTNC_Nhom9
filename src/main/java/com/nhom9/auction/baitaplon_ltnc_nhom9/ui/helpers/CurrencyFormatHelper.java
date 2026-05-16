package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers;

import java.math.BigDecimal;

/** Định dạng tiền tệ cho UI. */
public final class CurrencyFormatHelper {

    private CurrencyFormatHelper() {}

    public static String formatPrice(double price) {
        if (price >= 1_000_000) {
            return String.format("đ%.2fM", price / 1_000_000);
        }
        return String.format("đ%,.0f", price);
    }

    public static String formatVnd(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return String.format("%,.0f ₫", amount);
    }
}
