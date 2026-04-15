package org.example.baitaplon_ltnc_nhom9.exception;

import java.math.BigDecimal;

/**
 * Ném ra khi số tiền bid thấp hơn mức tối thiểu yêu cầu.
 */
public class BidTooLowException extends Exception {

    private final BigDecimal bidAmount;
    private final BigDecimal minimumRequired;

    public BidTooLowException(BigDecimal bidAmount, BigDecimal minimumRequired) {
        super(String.format("Bid %,.0f đ thấp hơn mức tối thiểu %,.0f đ.",
                bidAmount, minimumRequired));
        this.bidAmount       = bidAmount;
        this.minimumRequired = minimumRequired;
    }

    public BigDecimal getBidAmount()       { return bidAmount; }
    public BigDecimal getMinimumRequired() { return minimumRequired; }
}