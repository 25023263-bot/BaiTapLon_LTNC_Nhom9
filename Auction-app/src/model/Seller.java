package model;

import model.enums.UserRole;
import java.util.ArrayList;
import java.util.List;

public class Seller extends User {
    private List<AuctionItem> myItems;

    public Seller(int id, String name, String email, String password) {
        super(id, name, email, password, UserRole.SELLER);
        this.myItems = new ArrayList<>();
    }

    @Override
    public double getDiscount() {
        return 0.05; // Seller được giảm 5% phí giao dịch
    }

    public List<AuctionItem> getMyItems() {
        return myItems;
    }

    public void addItem(AuctionItem item) {
        myItems.add(item);
    }

    public void removeItem(AuctionItem item) {
        myItems.remove(item);
    }
}