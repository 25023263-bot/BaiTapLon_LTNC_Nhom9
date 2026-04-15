package org.example.baitaplon_ltnc_nhom9.exception;

import java.math.BigDecimal;

/**
 * Ném ra khi số dư ví không đủ để thực hiện thanh toán hoặc giữ cọc.
 */
public class InsufficientBalanceException extends Exception {

    private final BigDecimal available;
    private final BigDecimal required;

    public InsufficientBalanceException(String message) {
        super(message);
        this.available = null;
        this.required  = null;
    }

    public InsufficientBalanceException(BigDecimal available, BigDecimal required) {
        super(String.format("Số dư không đủ. Hiện có: %,.0f đ, cần: %,.0f đ.",
                available, required));
        this.available = available;
        this.required  = required;
    }

    public BigDecimal getAvailable() { return available; }
    public BigDecimal getRequired()  { return required; }
}