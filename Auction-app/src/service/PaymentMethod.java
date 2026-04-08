package service;

import model.User;

public interface PaymentMethod {
    boolean pay(double amount, User payer, User payee);
}