package org.example.baitaplon_ltnc_nhom9.service.payment;

import org.example.baitaplon_ltnc_nhom9.model.User;

public class CreditCardPayment implements PaymentMethod {
    @Override
    public boolean pay(double amount, User payer, User payee) {
        // Simulate external credit card processing
        System.out.println("Processing credit card payment of " + amount + " for user " + payer.getName());
        // Assume success
        payee.addBalance(amount);
        return true;
    }
}