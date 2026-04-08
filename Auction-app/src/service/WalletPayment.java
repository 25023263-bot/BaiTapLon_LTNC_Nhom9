package service;

import model.User;

public class WalletPayment implements PaymentMethod {
    @Override
    public boolean pay(double amount, User payer, User payee) {
        if (payer.deductBalance(amount)) {
            payee.addBalance(amount);
            System.out.println("Payment of " + amount + " from " + payer.getName() + " to " + payee.getName() + " successful.");
            return true;
        } else {
            System.out.println("Payment failed: insufficient balance.");
            return false;
        }
    }
}