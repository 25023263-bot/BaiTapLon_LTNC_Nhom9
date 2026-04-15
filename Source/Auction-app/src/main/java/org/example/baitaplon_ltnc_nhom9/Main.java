package org.example.baitaplon_ltnc_nhom9;

import org.example.baitaplon_ltnc_nhom9.domain.model.user.User;
import org.example.baitaplon_ltnc_nhom9.domain.model.user.*;
import org.example.baitaplon_ltnc_nhom9.domain.model.enums.*;
import org.example.baitaplon_ltnc_nhom9.service.*;
import org.example.baitaplon_ltnc_nhom9.exception.*;
import org.example.baitaplon_ltnc_nhom9.service.auction.AuctionHouse;
import org.example.baitaplon_ltnc_nhom9.service.auction.AuctionScheduler;
import org.example.baitaplon_ltnc_nhom9.service.auth.AuthService;
import org.example.baitaplon_ltnc_nhom9.service.notification.NotificationService;
import org.example.baitaplon_ltnc_nhom9.domain.model.item.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static AuctionHouse auctionHouse;
    private static AuthService authService;
    private static User currentUser;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // Load data
        auctionHouse = FileStorage.load();
        if (auctionHouse == null) {
            auctionHouse = new AuctionHouse();
            createSampleData();
        }
        authService = new AuthService(auctionHouse);
        // Setup scheduler and observer
        AuctionScheduler scheduler = new AuctionScheduler(auctionHouse);
        scheduler.start();
        auctionHouse.addObserver(new NotificationService());

        boolean running = true;
        while (running) {
            if (currentUser == null || !Boolean.parseBoolean(currentUser.isLoggedIn())) {
                showLoginMenu();
            } else {
                showMainMenu();
                int choice = readInt("Choose option: ");
                switch (choice) {
                    case 1: listAllItems(); break;
                    case 2: searchItems(); break;
                    case 3: placeBid(); break;
                    case 4: viewWatchlist(); break;
                    case 5: if (currentUser instanceof Seller) manageMyItems();
                    else if (currentUser instanceof Admin) adminPanel();
                    else System.out.println("Feature not available for your role.");
                        break;
                    case 6: logout(); break;
                    case 0: running = false; break;
                    default: System.out.println("Invalid option.");
                }
            }
        }
        // Save data before exit
        FileStorage.save(auctionHouse);
        scheduler.stop();
        System.out.println("Goodbye!");
    }

    private static void showLoginMenu() {
        System.out.println("\n=== Welcome to Auction App ===");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("0. Exit");
        int choice = readInt("Choose: ");
        switch (choice) {
            case 1: login(); break;
            case 2: register(); break;
            case 0: System.exit(0);
            default: System.out.println("Invalid choice.");
        }
    }

    private static void login() {
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        currentUser = authService.login(email, password);
        if (currentUser != null) {
            System.out.println("Login successful! Welcome " + currentUser.getName());
        } else {
            System.out.println("Login failed. Invalid credentials.");
        }
    }

    private static void register() {
        System.out.print("Name: ");
        String name = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.println("Role: 1. Buyer  2. Seller  3. Admin");
        int roleChoice = readInt("Choose role: ");
        User newUser;
        if (roleChoice == 1) {
            newUser = new Buyer(0, name, email, password);
        } else if (roleChoice == 2) {
            newUser = new Seller(0, name, email, password);
        } else {
            newUser = new Admin(0, name, email, password);
        }
        auctionHouse.addUser(newUser);
        System.out.println("Registration successful! Please login.");
    }

    private static void showMainMenu() {
        System.out.println("\n=== Main Menu ===");
        System.out.println("1. List all items");
        System.out.println("2. Search items");
        System.out.println("3. Place a bid");
        System.out.println("4. My watchlist (Buyer)");
        if (currentUser instanceof Seller) {
            System.out.println("5. Manage my items (Seller)");
        } else if (currentUser instanceof Admin) {
            System.out.println("5. Admin panel");
        } else {
            System.out.println("5. (Not available for Buyer)");
        }
        System.out.println("6. Logout");
        System.out.println("0. Exit");
    }

    private static void listAllItems() {
        List<AuctionItem> items = auctionHouse.getAllItems();
        if (items.isEmpty()) {
            System.out.println("No items available.");
        } else {
            for (AuctionItem item : items) {
                System.out.println(item);
            }
        }
    }

    private static void searchItems() {
        System.out.print("Enter keyword: ");
        String keyword = scanner.nextLine();
        List<AuctionItem> results = auctionHouse.search(keyword);
        if (results.isEmpty()) {
            System.out.println("No items found.");
        } else {
            results.forEach(System.out::println);
        }
    }

    private static void placeBid() {
        System.out.print("Enter item ID: ");
        int itemId = readInt("");
        System.out.print("Enter bid amount: ");
        double amount = readDouble("");
        try {
            auctionHouse.placeBid(currentUser.getId(), itemId, amount);
            System.out.println("Bid placed successfully!");
        } catch (Exception e) {
            System.out.println("Bid failed: " + e.getMessage());
        }
    }

    private static void viewWatchlist() {
        if (currentUser instanceof Buyer) {
            Buyer buyer = (Buyer) currentUser;
            List<AuctionItem> watchlist = buyer.getWatchlist();
            if (watchlist.isEmpty()) {
                System.out.println("Your watchlist is empty.");
            } else {
                watchlist.forEach(System.out::println);
            }
        } else {
            System.out.println("Only buyers have watchlist.");
        }
    }

    private static void manageMyItems() {
        if (currentUser instanceof Seller) {
            Seller seller = (Seller) currentUser;
            System.out.println("1. Create new item");
            System.out.println("2. List my items");
            int choice = readInt("Choose: ");
            if (choice == 1) {
                createNewItem(seller);
            } else if (choice == 2) {
                seller.getMyItems().forEach(System.out::println);
            }
        } else if (currentUser instanceof Admin) {
            adminPanel();
        }
    }

    private static void createNewItem(Seller seller) {
        System.out.print("Item name: ");
        String name = scanner.nextLine();
        System.out.print("Description: ");
        String desc = scanner.nextLine();
        System.out.print("Starting price: ");
        double startPrice = readDouble("");
        System.out.print("Min bid step: ");
        double step = readDouble("");
        System.out.println("Type: 1. Physical  2. Digital");
        int type = readInt("");
        AuctionItem item;
        if (type == 1) {
            System.out.print("Weight (kg): ");
            double weight = readDouble("");
            System.out.print("Dimensions: ");
            String dims = scanner.nextLine();
            System.out.print("Category: ");
            String cat = scanner.nextLine();
            item = new PhysicalItem(0, name, desc, startPrice, step, seller, weight, dims, cat);
        } else {
            System.out.print("Download link: ");
            String link = scanner.nextLine();
            item = new DigitalItem(0, name, desc, startPrice, step, seller, link);
        }
        auctionHouse.addItem(item);
        System.out.println("Item created. Now you can start auction.");
        System.out.print("Set auction duration in minutes: ");
        int minutes = readInt("");
        LocalDateTime endTime = LocalDateTime.now().plusMinutes(minutes);
        item.startAuction(endTime);
        System.out.println("Auction started. End time: " + endTime);
    }

    private static void adminPanel() {
        System.out.println("Admin features coming soon.");
        // Could list users, delete items, etc.
    }

    private static void logout() {
        authService.logout(currentUser);
        currentUser = null;
        System.out.println("Logged out.");
    }

    private static int readInt(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.println("Invalid number.");
            scanner.next();
        }
        int num = scanner.nextInt();
        scanner.nextLine(); // consume newline
        return num;
    }

    private static double readDouble(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextDouble()) {
            System.out.println("Invalid number.");
            scanner.next();
        }
        double num = scanner.nextDouble();
        scanner.nextLine();
        return num;
    }

    private static void createSampleData() {
        // Create a sample seller and buyer for testing
        Seller seller = new Seller(0, "John Seller", "seller@example.com", "pass");
        Buyer buyer = new Buyer(0, "Alice Buyer", "buyer@example.com", "pass");
        auctionHouse.addUser(seller);
        auctionHouse.addUser(buyer);
        // Add some balance
        buyer.addBalance(1000);
        seller.addBalance(100);
        // Create an item
        PhysicalItem laptop = new PhysicalItem(0, "Gaming Laptop", "High performance", 500.0, 10.0, seller, 2.5, "38x25x2 cm", "Electronics");
        auctionHouse.addItem(laptop);
        // Start auction
        laptop.startAuction(LocalDateTime.now().plusMinutes(30));
        System.out.println("Sample data created.");
    }
}
