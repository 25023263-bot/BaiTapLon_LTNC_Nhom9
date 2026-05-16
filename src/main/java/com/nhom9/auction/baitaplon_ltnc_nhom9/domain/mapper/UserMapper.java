package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.mapper;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.UserDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Admin;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;

/**
 * Chuyển đổi giữa User model và UserDTO.
 * Không truyền passwordHash ra DTO.
 */
public class UserMapper {

    private UserMapper() {} // Utility class

    // ─── Model → DTO ─────────────────────────────────────────────────────────

    public static UserDTO toDTO(User user) {
        if (user == null) return null;

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setActive(user.isActive());
        dto.setCreatedAt(user.getCreatedAt());

        // Thông tin đặc thù theo role
        if (user instanceof Buyer buyer) {
            dto.setWalletBalance(buyer.getWalletBalance());

        } else if (user instanceof Seller seller) {
            dto.setEarningsBalance(seller.getEarningsBalance());
        }
        // Admin không có trường đặc thù nào cần map

        return dto;
    }

    // ─── DTO → Model ─────────────────────────────────────────────────────────

    /**
     * Tạo Buyer từ DTO (dùng khi đăng ký hoặc load từ DB).
     * passwordHash phải được set riêng bởi AuthService.
     */
    public static Buyer toBuyer(UserDTO dto) {
        if (dto == null) return null;
        Buyer buyer = new Buyer();
        applyCommonFields(buyer, dto);
        buyer.setWalletBalance(dto.getWalletBalance());
        return buyer;
    }

    public static Seller toSeller(UserDTO dto) {
        if (dto == null) return null;
        Seller seller = new Seller();
        applyCommonFields(seller, dto);
        seller.setEarningsBalance(dto.getEarningsBalance());
        return seller;
    }

    public static Admin toAdmin(UserDTO dto) {
        if (dto == null) return null;
        Admin admin = new Admin();
        applyCommonFields(admin, dto);
        return admin;
    }

    /**
     * Áp dụng các trường chung từ DTO vào User.
     */
    private static void applyCommonFields(User user, UserDTO dto) {
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setRole(dto.getRole());
        user.setActive(dto.isActive());
        user.setCreatedAt(dto.getCreatedAt());
    }

    /**
     * Cập nhật thông tin profile của User từ DTO (partial update).
     */
    public static void updateProfileFromDTO(User user, UserDTO dto) {
        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getPhone()    != null) user.setPhone(dto.getPhone());
        if (dto.getEmail()    != null) user.setEmail(dto.getEmail());
    }
}
