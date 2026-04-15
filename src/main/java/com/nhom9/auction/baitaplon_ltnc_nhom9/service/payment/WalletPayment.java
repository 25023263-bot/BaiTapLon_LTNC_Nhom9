package com.nhom9.auction.baitaplon_ltnc_nhom9.service.payment;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.Buyer;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.user.User;
import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.InsufficientBalanceException;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.UserRepository;

import java.math.BigDecimal;
import java.util.logging.Logger;

/**
 * Thanh toán bằng ví nội bộ (số dư trong hệ thống).
 */
public class WalletPayment implements PaymentMethod {

    private static final Logger LOG = Logger.getLogger(WalletPayment.class.getName());

    private final UserRepository userRepo;

    public WalletPayment(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public String getMethodName() { return "WALLET"; }

    @Override
    public String processPayment(int userId, BigDecimal amount, int itemId)
            throws InsufficientBalanceException, Exception {

        Buyer buyer = loadBuyer(userId);

        if (!buyer.hasSufficientBalance(amount))
            throw new InsufficientBalanceException(buyer.getWalletBalance(), amount);

        buyer.deduct(amount);
        userRepo.updateWalletBalance(userId, buyer.getWalletBalance());

        String ref = "WALLET-" + userId + "-" + itemId + "-" + System.currentTimeMillis();
        LOG.info(String.format("Wallet payment: user #%d, amount=%,.0f, ref=%s", userId, amount, ref));
        return ref;
    }

    @Override
    public boolean isAvailable(int userId) {
        try {
            return userRepo.findById(userId)
                    .map(u -> u instanceof Buyer)
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void refund(String transactionRef, BigDecimal amount, int userId) throws Exception {
        Buyer buyer = loadBuyer(userId);
        buyer.deposit(amount);
        userRepo.updateWalletBalance(userId, buyer.getWalletBalance());
        LOG.info(String.format("Wallet refund: user #%d, amount=%,.0f, ref=%s", userId, amount, transactionRef));
    }

    // ─── Nạp tiền vào ví ─────────────────────────────────────────────────────

    /**
     * Nạp tiền vào ví (từ Admin hoặc ngoài hệ thống).
     */
    public void topUp(int userId, BigDecimal amount) throws Exception {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0.");
        Buyer buyer = loadBuyer(userId);
        buyer.deposit(amount);
        userRepo.updateWalletBalance(userId, buyer.getWalletBalance());
        LOG.info(String.format("Top-up: user #%d, amount=%,.0f", userId, amount));
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Buyer loadBuyer(int userId) throws Exception {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user #" + userId));
        if (!(user instanceof Buyer))
            throw new IllegalStateException("User #" + userId + " không phải Buyer.");
        return (Buyer) user;
    }
}