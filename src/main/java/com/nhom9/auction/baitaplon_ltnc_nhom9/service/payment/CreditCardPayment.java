package com.nhom9.auction.baitaplon_ltnc_nhom9.service.payment;

import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.InsufficientBalanceException;

import java.math.BigDecimal;
import java.util.logging.Logger;

/**
 * Thanh toán bằng thẻ tín dụng.
 *
 * Đây là stub – trong thực tế sẽ tích hợp với Stripe/VNPay/MoMo API.
 * Hiện tại luôn thành công để test flow.
 */
public class CreditCardPayment implements PaymentMethod {

    private static final Logger LOG = Logger.getLogger(CreditCardPayment.class.getName());

    /** Phí giao dịch thẻ: 1.5% */
    private static final double CARD_FEE_RATE = 0.015;

    @Override
    public String getMethodName() { return "CREDIT_CARD"; }

    @Override
    public String processPayment(int userId, BigDecimal amount, int itemId)
            throws InsufficientBalanceException, Exception {

        // Tính phí thẻ
        BigDecimal fee   = amount.multiply(BigDecimal.valueOf(CARD_FEE_RATE));
        BigDecimal total = amount.add(fee);

        LOG.info(String.format("Credit card charge: user #%d, amount=%,.0f, fee=%,.0f, total=%,.0f",
                userId, amount, fee, total));

        // TODO: Gọi payment gateway API ở đây
        // Ví dụ Stripe: PaymentIntent.create(...)

        // Stub: luôn thành công
        String ref = "CC-" + userId + "-" + itemId + "-" + System.currentTimeMillis();
        LOG.info("Credit card payment OK: ref=" + ref);
        return ref;
    }

    @Override
    public boolean isAvailable(int userId) {
        // TODO: Kiểm tra user đã thêm thẻ chưa
        return true;
    }

    @Override
    public void refund(String transactionRef, BigDecimal amount, int userId) throws Exception {
        // TODO: Gọi refund API của payment gateway
        LOG.info(String.format("Credit card refund: user #%d, amount=%,.0f, ref=%s",
                userId, amount, transactionRef));
    }

    public double getCardFeeRate() { return CARD_FEE_RATE; }

    /**
     * Tính tổng số tiền bao gồm phí thẻ.
     */
    public BigDecimal calculateTotal(BigDecimal amount) {
        return amount.add(amount.multiply(BigDecimal.valueOf(CARD_FEE_RATE)));
    }
}