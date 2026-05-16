package com.nhom9.auction.baitaplon_ltnc_nhom9.service.wallet;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Seller;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.UserRepository;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Nạp tiền vào ví (Buyer) hoặc số dư thu nhập (Seller) — đồng bộ session + DB.
 */
public final class WalletDepositService {

    public static final BigDecimal MIN_AMOUNT = new BigDecimal("10000");

    private final UserRepository userRepo;

    public WalletDepositService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public BigDecimal parseAmount(String raw) {
        String digits = raw.trim().replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập số tiền.");
        }
        try {
            return new BigDecimal(digits);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số tiền không hợp lệ.");
        }
    }

    public void validateAmount(BigDecimal amount) {
        if (amount.compareTo(MIN_AMOUNT) < 0) {
            throw new IllegalArgumentException("Số tiền tối thiểu là 10.000 ₫.");
        }
    }

    /**
     * Cộng tiền vào user đang trong session và lưu DB. Rollback RAM nếu lưu DB thất bại.
     */
    public void deposit(User user, BigDecimal amount) throws SQLException {
        if (user instanceof Buyer buyer) {
            buyer.deposit(amount);
            try {
                userRepo.updateWalletBalance(buyer.getId(), buyer.getWalletBalance());
            } catch (SQLException e) {
                buyer.setWalletBalance(buyer.getWalletBalance().subtract(amount));
                throw e;
            }
        } else if (user instanceof Seller seller) {
            BigDecimal prev = seller.getEarningsBalance() != null
                    ? seller.getEarningsBalance() : BigDecimal.ZERO;
            BigDecimal next = prev.add(amount);
            seller.setEarningsBalance(next);
            try {
                userRepo.updateEarningsBalance(seller.getId(), next);
            } catch (SQLException e) {
                seller.setEarningsBalance(prev);
                throw e;
            }
        } else {
            throw new IllegalStateException("Tài khoản không hỗ trợ nạp tiền.");
        }
    }
}
