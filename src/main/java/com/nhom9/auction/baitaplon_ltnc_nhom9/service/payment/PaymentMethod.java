package com.nhom9.auction.baitaplon_ltnc_nhom9.service.payment;

import com.nhom9.auction.baitaplon_ltnc_nhom9.exception.InsufficientBalanceException;

import java.math.BigDecimal;

/**
 * Hợp đồng cho các phương thức thanh toán.
 */
public interface PaymentMethod {

    /**
     * Tên phương thức thanh toán.
     */
    String getMethodName();

    /**
     * Thực hiện thanh toán.
     * @param userId   người thanh toán
     * @param amount   số tiền
     * @param itemId   vật phẩm liên quan
     * @return mã tham chiếu giao dịch (hoặc "WALLET-txId" với ví)
     * @throws InsufficientBalanceException nếu không đủ tiền
     */
    String processPayment(int userId, BigDecimal amount, int itemId)
            throws InsufficientBalanceException, Exception;

    /**
     * Kiểm tra phương thức có khả dụng cho user không.
     */
    boolean isAvailable(int userId);

    /**
     * Hoàn tiền (refund) một giao dịch.
     * @param transactionRef mã tham chiếu từ processPayment
     */
    void refund(String transactionRef, BigDecimal amount, int userId) throws Exception;
}