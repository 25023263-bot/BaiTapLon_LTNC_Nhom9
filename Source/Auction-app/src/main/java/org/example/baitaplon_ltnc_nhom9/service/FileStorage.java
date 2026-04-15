package org.example.baitaplon_ltnc_nhom9.service;

import org.example.baitaplon_ltnc_nhom9.service.auction.AuctionHouse;

import java.io.*;

public class FileStorage {
    private static final String DATA_FILE = "auction_data.ser";

    public static void save(AuctionHouse auctionHouse) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(auctionHouse);
            System.out.println("Data saved successfully.");
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    public static AuctionHouse load() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            AuctionHouse ah = (AuctionHouse) ois.readObject();
            System.out.println("Data loaded successfully.");
            return ah;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading data: " + e.getMessage());
            return null;
        }
    }
}