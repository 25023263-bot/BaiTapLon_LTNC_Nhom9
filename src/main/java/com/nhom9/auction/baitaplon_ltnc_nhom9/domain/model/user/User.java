package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;

public class User {
    private int id;
    private String fullName;
    private String username;
    private double balance;

    public User(int id, String fullName, String username, double balance) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.balance = balance;
    }

    // Getters
    public int getId()          { return id; }
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    public double getBalance()  { return balance; }
}
