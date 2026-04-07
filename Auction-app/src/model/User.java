package model;

import model.enums.UserRole;
import service.Authenticatable;
import java.time.LocalDateTime;
import java.util.UUID;

public abstract class User implements Authenticatable {
    protected int id;
    protected String name;
    protected String email;
    protected String passwordHash;
    protected double balance;
    protected UserRole role;
    protected boolean loggedIn;
    protected String sessionToken;
    protected LocalDateTime lastLogin;

    public User(int id, String name, String email, String password, UserRole role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = hashPassword(password);
        this.balance = 0.0;
        this.role = role;
        this.loggedIn = false;
    }

    private String hashPassword(String plain) {
        // Trong thực tế nên dùng BCrypt, ở đây chỉ mã hóa đơn giản
        return Integer.toHexString(plain.hashCode());
    }

    protected boolean checkPassword(String plain) {
        return hashPassword(plain).equals(this.passwordHash);
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean login(String email, String password) {
        if (this.email.equals(email) && checkPassword(password)) {
            this.loggedIn = true;
            this.sessionToken = generateToken();
            this.lastLogin = LocalDateTime.now();
            return true;
        }
        return false;
    }

    @Override
    public void logout() {
        this.loggedIn = false;
        this.sessionToken = null;
    }

    @Override
    public boolean isLoggedIn() {
        return loggedIn;
    }

    @Override
    public String getSessionToken() {
        return sessionToken;
    }

    // Getters and setters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public UserRole getRole() { return role; }
    public LocalDateTime getLastLogin() { return lastLogin; }

    public void addBalance(double amount) {
        this.balance += amount;
    }

    public boolean deductBalance(double amount) {
        if (balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }

    // Abstract method for discount (can be used for transaction fees)
    public abstract double getDiscount();

    @Override
    public String toString() {
        return String.format("User{id=%d, name='%s', email='%s', role=%s, balance=%.2f}",
                id, name, email, role, balance);
    }
}