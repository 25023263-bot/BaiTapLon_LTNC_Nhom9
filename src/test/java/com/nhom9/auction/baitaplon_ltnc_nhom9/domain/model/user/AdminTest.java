package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho Admin – kiểm tra logic phân quyền theo accessLevel.
 */
@DisplayName("Admin – Phân quyền theo cấp độ")
class AdminTest {

    // ─── isSuperAdmin() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("isSuperAdmin: accessLevel = 2 → trả về true")
    void isSuperAdmin_levelTwo_returnsTrue() {
        Admin admin = new Admin(1, "superadmin", "sa@example.com",
                "hash", "Super Admin", "0900000001", 2);

        assertTrue(admin.isSuperAdmin());
    }

    @Test
    @DisplayName("isSuperAdmin: accessLevel >= 3 → vẫn là super admin")
    void isSuperAdmin_levelThreeOrAbove_returnsTrue() {
        Admin admin = new Admin(1, "superadmin", "sa@example.com",
                "hash", "Super Admin", "0900000001", 3);

        assertTrue(admin.isSuperAdmin());
    }

    @Test
    @DisplayName("isSuperAdmin: accessLevel = 1 → trả về false (chỉ là moderator)")
    void isSuperAdmin_levelOne_returnsFalse() {
        Admin admin = new Admin(2, "moderator", "mod@example.com",
                "hash", "Moderator", "0900000002", 1);

        assertFalse(admin.isSuperAdmin());
    }

    // ─── hasPermission() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("hasPermission: accessLevel đủ → trả về true")
    void hasPermission_sufficientLevel_returnsTrue() {
        Admin admin = new Admin(1, "admin", "admin@example.com",
                "hash", "Admin", "0900000003", 2);

        assertTrue(admin.hasPermission(1));
        assertTrue(admin.hasPermission(2));
    }

    @Test
    @DisplayName("hasPermission: accessLevel không đủ → trả về false")
    void hasPermission_insufficientLevel_returnsFalse() {
        Admin admin = new Admin(1, "mod", "mod@example.com",
                "hash", "Mod", "0900000004", 1);

        assertFalse(admin.hasPermission(2));
    }

    @Test
    @DisplayName("hasPermission: accessLevel bằng đúng yêu cầu → trả về true (biên)")
    void hasPermission_exactLevel_returnsTrue() {
        Admin admin = new Admin(1, "admin", "admin@example.com",
                "hash", "Admin", "0900000005", 2);

        assertTrue(admin.hasPermission(2));
    }

    // ─── getRoleDescription() ─────────────────────────────────────────────────

    @Test
    @DisplayName("getRoleDescription: super admin → mô tả chứa 'Super Admin'")
    void getRoleDescription_superAdmin_containsSuperAdminText() {
        Admin admin = new Admin(1, "sa", "sa@example.com",
                "hash", "SA", "09", 2);

        assertTrue(admin.getRoleDescription().contains("Super Admin"));
    }

    @Test
    @DisplayName("getRoleDescription: moderator → mô tả chứa 'Moderator'")
    void getRoleDescription_moderator_containsModeratorText() {
        Admin admin = new Admin(1, "mod", "mod@example.com",
                "hash", "Mod", "09", 1);

        assertTrue(admin.getRoleDescription().contains("Moderator"));
    }

    // ─── Role check ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("constructor: role tự động là ADMIN")
    void constructor_roleIsAutomaticallyAdmin() {
        Admin admin = new Admin(1, "admin", "admin@example.com",
                "hash", "Admin", "09", 1);

        assertEquals(com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole.ADMIN,
                admin.getRole());
    }
}
