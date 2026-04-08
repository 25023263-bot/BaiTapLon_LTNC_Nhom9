package service;

import model.User;

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