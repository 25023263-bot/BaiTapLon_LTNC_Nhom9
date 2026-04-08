package org.example.baitaplon_ltnc_nhom9.service;

import org.example.baitaplon_ltnc_nhom9.model.*;
import org.example.baitaplon_ltnc_nhom9.model.enums.AuctionStatus;
import org.example.baitaplon_ltnc_nhom9.exception.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class AuctionHouse implements Searchable<AuctionItem>, Serializable {
    private static final long serialVersionUID = 1L;

    private Map<Integer, User> users = new HashMap<>();
    private Map<Integer, AuctionItem> items = new HashMap<>();
    private transient List<AuctionObserver> observers = new ArrayList<>();
    private int nextUserId = 1;
    private int nextItemId = 1;

    // User management
    public void addUser(User user) {
        user.setId(nextUserId++);   // SỬA: dùng setter
        users.put(user.getId(), user);
    }

    public User getUserById(int id) {
        return users.get(id);
    }

    public User getUserByEmail(String email) {
        return users.values().stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst()
                .orElse(null);
    }

    // Item management
    public void addItem(AuctionItem item) {
        item.setId(nextItemId++);   // SỬA: dùng setter
        items.put(item.getId(), item);
        if (item.getSeller() instanceof Seller) {
            ((Seller) item.getSeller()).addItem(item);
        }
    }

    public AuctionItem getItemById(int id) {
        return items.get(id);
    }

    public List<AuctionItem> getAllItems() {
        return new ArrayList<>(items.values());
    }

    // Place bid
    public void placeBid(int userId, int itemId, double amount)
            throws InsufficientBalanceException, BidTooLowException, AuctionClosedException, AuthenticationException {
        User user = users.get(userId);
        if (user == null || !user.isLoggedIn()) {
            throw new AuthenticationException("User not logged in");
        }
        AuctionItem item = items.get(itemId);
        if (item == null) throw new IllegalArgumentException("Item not found");
        if (item.getStatus() != AuctionStatus.ACTIVE) {
            throw new AuctionClosedException("Auction is not active");
        }
        if (amount <= item.getCurrentPrice() + item.getMinBidStep()) {
            throw new BidTooLowException("Bid must be at least " + (item.getCurrentPrice() + item.getMinBidStep()));
        }
        if (user.getBalance() < amount) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
        try {
            item.placeBid(user, amount);
            notifyObservers(item);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Observer pattern
    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers(AuctionItem item) {
        for (AuctionObserver obs : observers) {
            obs.update(item);
        }
    }

    // Searchable implementation
    @Override
    public List<AuctionItem> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return getAllItems();
        String lowerKeyword = keyword.toLowerCase();
        return items.values().stream()
                .filter(item -> item.getName().toLowerCase().contains(lowerKeyword) ||
                        item.getDescription().toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());
    }

    @Override
    public List<AuctionItem> filter(FilterCriteria criteria) {
        return items.values().stream()
                .filter(item -> criteria.getNameContains() == null ||
                        item.getName().toLowerCase().contains(criteria.getNameContains().toLowerCase()))
                .filter(item -> criteria.getMinPrice() == null || item.getCurrentPrice() >= criteria.getMinPrice())
                .filter(item -> criteria.getMaxPrice() == null || item.getCurrentPrice() <= criteria.getMaxPrice())
                .filter(item -> criteria.getStatus() == null || item.getStatus() == criteria.getStatus())
                .filter(item -> criteria.getSellerId() == null || item.getSeller().getId() == criteria.getSellerId())
                .filter(item -> {
                    if (criteria.getEndingSoon() != null && criteria.getEndingSoon()) {
                        return item.getEndTime() != null &&
                                item.getEndTime().isBefore(LocalDateTime.now().plusHours(1));
                    }
                    return true;
                })
                .filter(item -> {
                    if (criteria.getCategory() != null && item instanceof PhysicalItem) {
                        return criteria.getCategory().equalsIgnoreCase(((PhysicalItem) item).getCategory());
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Page<AuctionItem> search(String keyword, int page, int pageSize) {
        List<AuctionItem> results = search(keyword);
        int start = page * pageSize;
        int end = Math.min(start + pageSize, results.size());
        List<AuctionItem> content = results.subList(start, end);
        return new Page<>(content, page, pageSize, results.size());
    }

    public List<AuctionItem> getActiveItems() {
        return items.values().stream()
                .filter(i -> i.getStatus() == AuctionStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    public Map<Integer, User> getUsers() { return users; }
    public Map<Integer, AuctionItem> getItems() { return items; }
}