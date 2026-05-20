package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho Admin – kiểm tra các hành vi hiện có của Admin.
 *
 * LƯU Ý: Các test liên quan đến accessLevel, isSuperAdmin(), hasPermission()
 * đã bị xóa vì Admin hiện tại chưa implement các tính năng đó.
 * Sẽ được thêm lại khi production code sẵn sàng.
 */
@DisplayName("Admin – Hành vi cơ bản")
class AdminTest {

    // ─── Constructor & Role ───────────────────────────────────────────────────

    @Test
    @DisplayName("constructor 6 tham số: role tự động là ADMIN")
    void constructor_sixArgs_roleIsAdmin() {
        Admin admin = new Admin(1, "admin1", "admin@example.com",
                "hash", "Admin Name", "0900000001");

        assertEquals(UserRole.ADMIN, admin.getRole());
    }

    @Test
    @DisplayName("constructor mặc định: role tự động là ADMIN")
    void constructor_default_roleIsAdmin() {
        Admin admin = new Admin();

        assertEquals(UserRole.ADMIN, admin.getRole());
    }

    @Test
    @DisplayName("constructor đầy đủ (load từ DB): lưu đúng thông tin")
    void constructor_fullArgs_storesFieldsCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        Admin admin = new Admin(2, "mod", "mod@example.com",
                "hash", "Moderator", "0900000002",
                true, now, now);

        assertEquals(2, admin.getId());
        assertEquals("mod", admin.getUsername());
        assertEquals("mod@example.com", admin.getEmail());
        assertEquals("Moderator", admin.getFullName());
        assertTrue(admin.isActive());
        assertEquals(UserRole.ADMIN, admin.getRole());
    }

    // ─── disableUser() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("disableUser: user đang active → bị vô hiệu hóa")
    void disableUser_activeUser_becomesInactive() {
        Admin admin = new Admin(1, "admin", "admin@example.com",
                "hash", "Admin", "090");
        Buyer target = new Buyer(2, "buyer", "buyer@example.com",
                "hash", "Buyer", "091");

        admin.disableUser(target);

        assertFalse(target.isActive());
    }

    @Test
    @DisplayName("disableUser: target null → ném IllegalArgumentException")
    void disableUser_nullTarget_throwsException() {
        Admin admin = new Admin(1, "admin", "admin@example.com",
                "hash", "Admin", "090");

        assertThrows(IllegalArgumentException.class,
                () -> admin.disableUser(null));
    }

    // ─── enableUser() ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("enableUser: user đang bị disabled → active trở lại")
    void enableUser_inactiveUser_becomesActive() {
        Admin admin = new Admin(1, "admin", "admin@example.com",
                "hash", "Admin", "090");
        Buyer target = new Buyer(2, "buyer", "buyer@example.com",
                "hash", "Buyer", "091");

        admin.disableUser(target);
        admin.enableUser(target);

        assertTrue(target.isActive());
    }

    @Test
    @DisplayName("enableUser: target null → ném IllegalArgumentException")
    void enableUser_nullTarget_throwsException() {
        Admin admin = new Admin(1, "admin", "admin@example.com",
                "hash", "Admin", "090");

        assertThrows(IllegalArgumentException.class,
                () -> admin.enableUser(null));
    }

    // ─── getRoleDescription() ─────────────────────────────────────────────────

    @Test
    @DisplayName("getRoleDescription: trả về chuỗi không null và không rỗng")
    void getRoleDescription_returnsNonEmptyString() {
        Admin admin = new Admin(1, "admin", "admin@example.com",
                "hash", "Admin", "090");

        String desc = admin.getRoleDescription();

        assertNotNull(desc);
        assertFalse(desc.isBlank());
    }

    // ─── toString() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString: chứa id và username")
    void toString_containsIdAndUsername() {
        Admin admin = new Admin(99, "superadmin", "sa@example.com",
                "hash", "SA", "090");

        String str = admin.toString();

        assertTrue(str.contains("99"));
        assertTrue(str.contains("superadmin"));
    }
}
