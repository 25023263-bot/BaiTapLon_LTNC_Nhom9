package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

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

    // ─── activate / deactivate ────────────────────────────────────────────────

    @Test
    @DisplayName("deactivate: admin active → bị vô hiệu hóa")
    void deactivate_activeAdmin_becomesInactive() {
        Admin admin = new Admin(1, "admin", "admin@example.com", "hash", "Admin", "090");

        admin.deactivate();

        assertFalse(admin.isActive());
    }

    @Test
    @DisplayName("activate: admin bị disabled → active trở lại")
    void activate_inactiveAdmin_becomesActive() {
        Admin admin = new Admin(1, "admin", "admin@example.com", "hash", "Admin", "090");
        admin.deactivate();

        admin.activate();

        assertTrue(admin.isActive());
    }

    // ─── toString() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString: chứa id và username")
    void toString_containsIdAndUsername() {
        Admin admin = new Admin(99, "superadmin", "sa@example.com", "hash", "SA", "090");

        String str = admin.toString();

        assertTrue(str.contains("99"));
        assertTrue(str.contains("superadmin"));
    }
}
