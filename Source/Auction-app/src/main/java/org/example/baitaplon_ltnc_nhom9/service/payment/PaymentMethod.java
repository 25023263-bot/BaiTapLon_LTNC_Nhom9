package org.example.baitaplon_ltnc_nhom9.service;

import org.example.baitaplon_ltnc_nhom9.model.User;
import org.example.baitaplon_ltnc_nhom9.model.User;

public interface PaymentMethod {
    boolean pay(double amount, User payer, User payee);

}