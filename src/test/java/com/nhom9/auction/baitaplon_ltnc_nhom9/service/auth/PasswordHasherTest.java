package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test cho PasswordHasher – kiểm tra bcrypt hash và validation mật khẩu.
 *
 * Lý do test class này:
 * - isStrong() là pure logic (không cần DB) → dễ test, quan trọng cho security
 * - hash() và verify() là core của authentication
 *
 * Lưu ý: BCrypt cố ý chạy chậm (rounds=12) để chống brute-force.
 * Trong test, mỗi hash() mất ~200-400ms là bình thường.
 */
@DisplayName("PasswordHasher – Hash và kiểm tra mật khẩu")
class PasswordHasherTest {

    // ─── isStrong() ───────────────────────────────────────────────────────────
    // Test isStrong() trước vì đây là pure logic, nhanh, không cần BCrypt

    @Test
    @DisplayName("isStrong: mật khẩu đủ điều kiện (chữ hoa + thường + số + ≥8 ký tự) → true")
    void isStrong_validPassword_returnsTrue() {
        assertTrue(PasswordHasher.isStrong("SecurePass1"));
    }

    @Test
    @DisplayName("isStrong: đúng 8 ký tự, đủ loại → true (biên dưới)")
    void isStrong_exactly8Chars_returnsTrue() {
        assertTrue(PasswordHasher.isStrong("Abc12345"));
    }

    @Test
    @DisplayName("isStrong: chỉ 7 ký tự → false (dưới biên)")
    void isStrong_sevenChars_returnsFalse() {
        assertFalse(PasswordHasher.isStrong("Ab1234c")); // đủ loại nhưng chỉ 7 ký tự
    }

    @Test
    @DisplayName("isStrong: không có chữ hoa → false")
    void isStrong_noUppercase_returnsFalse() {
        assertFalse(PasswordHasher.isStrong("password123"));
    }

    @Test
    @DisplayName("isStrong: không có chữ thường → false")
    void isStrong_noLowercase_returnsFalse() {
        assertFalse(PasswordHasher.isStrong("PASSWORD123"));
    }

    @Test
    @DisplayName("isStrong: không có số → false")
    void isStrong_noDigit_returnsFalse() {
        assertFalse(PasswordHasher.isStrong("PasswordOnly"));
    }

    @Test
    @DisplayName("isStrong: chuỗi rỗng → false")
    void isStrong_emptyString_returnsFalse() {
        assertFalse(PasswordHasher.isStrong(""));
    }

    @Test
    @DisplayName("isStrong: null → false (không ném NullPointerException)")
    void isStrong_null_returnsFalse() {
        assertFalse(PasswordHasher.isStrong(null));
    }

    @Test
    @DisplayName("isStrong: mật khẩu rất dài → true (không có giới hạn trên)")
    void isStrong_veryLongPassword_returnsTrue() {
        assertTrue(PasswordHasher.isStrong("MyVeryLongAndSecurePassword123456789"));
    }

    // ─── hash() ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("hash: trả về chuỗi BCrypt 60 ký tự bắt đầu bằng $2a$")
    void hash_validPassword_returnsBcryptHash() {
        String hash = PasswordHasher.hash("ValidPass1");

        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"),
                "BCrypt hash phải bắt đầu bằng $2a$ hoặc $2b$");
        assertEquals(60, hash.length(),
                "BCrypt hash luôn dài đúng 60 ký tự");
    }

    @Test
    @DisplayName("hash: hai lần hash cùng mật khẩu → kết quả KHÁC nhau (do salt ngẫu nhiên)")
    void hash_samePasswordTwice_producesDifferentHashes() {
        // Đây là tính năng bảo mật quan trọng của BCrypt:
        // mỗi lần hash dùng salt ngẫu nhiên → không thể tấn công rainbow table
        String hash1 = PasswordHasher.hash("ValidPass1");
        String hash2 = PasswordHasher.hash("ValidPass1");

        assertNotEquals(hash1, hash2,
                "BCrypt phải tạo salt ngẫu nhiên mỗi lần – hai hash phải khác nhau");
    }

    @Test
    @DisplayName("hash: mật khẩu null → ném IllegalArgumentException")
    void hash_null_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordHasher.hash(null));
    }

    @Test
    @DisplayName("hash: mật khẩu rỗng → ném IllegalArgumentException")
    void hash_emptyString_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordHasher.hash(""));
    }

    // ─── verify() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("verify: mật khẩu đúng → true")
    void verify_correctPassword_returnsTrue() {
        String raw  = "MyPassword9";
        String hash = PasswordHasher.hash(raw);

        assertTrue(PasswordHasher.verify(raw, hash));
    }

    @Test
    @DisplayName("verify: mật khẩu sai → false")
    void verify_wrongPassword_returnsFalse() {
        String hash = PasswordHasher.hash("CorrectPass1");

        assertFalse(PasswordHasher.verify("WrongPass99", hash));
    }

    @Test
    @DisplayName("verify: khác hoa/thường → false (BCrypt phân biệt hoa thường)")
    void verify_differentCase_returnsFalse() {
        String hash = PasswordHasher.hash("SecurePass1");

        assertFalse(PasswordHasher.verify("securepass1", hash));
    }

    @Test
    @DisplayName("verify: rawPassword = null → false (không crash)")
    void verify_nullRaw_returnsFalse() {
        String hash = PasswordHasher.hash("ValidPass1");

        assertFalse(PasswordHasher.verify(null, hash));
    }

    @Test
    @DisplayName("verify: hash = null → false (không crash)")
    void verify_nullHash_returnsFalse() {
        assertFalse(PasswordHasher.verify("ValidPass1", null));
    }

    @Test
    @DisplayName("verify: hash không hợp lệ → false (không ném exception)")
    void verify_invalidHashFormat_returnsFalse() {
        // Hash giả mạo không phải BCrypt format
        assertFalse(PasswordHasher.verify("SomePass1", "not-a-valid-hash"));
    }
}
