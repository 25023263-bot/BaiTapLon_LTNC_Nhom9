package org.example.baitaplon_ltnc_nhom9.domain.model.enums;

/**
 * Vai trò của người dùng trong hệ thống.
 */
public enum UserRole {

    BUYER("Người mua"),
    SELLER("Người bán"),
    ADMIN("Quản trị viên");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse từ string (không phân biệt hoa thường).
     */
    public static UserRole fromString(String value) {
        for (UserRole role : values()) {
            if (role.name().equalsIgnoreCase(value)) return role;
        }
        throw new IllegalArgumentException("Không tìm thấy role: " + value);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
