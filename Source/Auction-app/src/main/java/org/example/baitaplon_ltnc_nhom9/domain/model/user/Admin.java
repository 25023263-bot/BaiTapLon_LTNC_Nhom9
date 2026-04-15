package domain.model.user;

import org.example.baitaplon_ltnc_nhom9.model.enums.UserRole;

public class Admin extends User {
    public Admin(int id, String name, String email, String password) {
        super(id, name, email, password, UserRole.ADMIN);
    }

    @Override
    public double getDiscount() {
        return 0.0;
    }

    // Additional admin-specific methods can be added
    public void banUser(User user) {
        // Implementation would depend on persistence
        System.out.println("Admin " + getName() + " banned user " + user.getName());
    }
}