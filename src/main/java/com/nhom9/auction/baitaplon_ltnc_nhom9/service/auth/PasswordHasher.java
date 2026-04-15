package com.nhom9.auction.baitaplon_ltnc_nhom9.service.auth;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Tiện ích hash và kiểm tra mật khẩu dùng BCrypt.
 * Dependency: org.mindrot:jbcrypt:0.4 (thêm vào pom.xml)
 */
public class PasswordHasher {

    private PasswordHasher() {}

    /** Số vòng BCrypt (12 = cân bằng giữa bảo mật và tốc độ) */
    private static final int ROUNDS = 12;

    /**
     * Hash mật khẩu thô thành BCrypt hash.
     * @param rawPassword mật khẩu người dùng nhập
     * @return chuỗi BCrypt hash (60 ký tự)
     */
    public static String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank())
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(ROUNDS));
    }

    /**
     * Kiểm tra mật khẩu thô có khớp hash đã lưu không.
     * @param rawPassword mật khẩu người dùng nhập
     * @param hashed      hash đã lưu trong DB
     * @return true nếu khớp
     */
    public static boolean verify(String rawPassword, String hashed) {
        if (rawPassword == null || hashed == null) return false;
        try {
            return BCrypt.checkpw(rawPassword, hashed);
        } catch (Exception e) {
            // Hash không hợp lệ
            return false;
        }
    }

    /**
     * Kiểm tra mật khẩu đủ mạnh:
     * - Ít nhất 8 ký tự
     * - Có chữ hoa, chữ thường, chữ số
     */
    public static boolean isStrong(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8) return false;
        boolean hasUpper  = rawPassword.chars().anyMatch(Character::isUpperCase);
        boolean hasLower  = rawPassword.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit  = rawPassword.chars().anyMatch(Character::isDigit);
        return hasUpper && hasLower && hasDigit;
    }
}