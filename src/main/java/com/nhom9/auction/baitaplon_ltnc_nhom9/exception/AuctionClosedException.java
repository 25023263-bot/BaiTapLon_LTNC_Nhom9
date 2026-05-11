package com.nhom9.auction.baitaplon_ltnc_nhom9.exception;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;

/**
 * Thrown when placing a bid on an auction that is no longer active.
 */
public class AuctionClosedException extends Exception {

    private final int auctionId;
    private final AuctionStatus currentStatus;

    public AuctionClosedException(int auctionId, AuctionStatus currentStatus) {
        super(String.format("Phiên đấu giá #%d không còn nhận bid (trạng thái: %s).",
                auctionId, currentStatus.getDisplayName()));
        this.auctionId     = auctionId;
        this.currentStatus = currentStatus;
    }

    public int getAuctionId()                 { return auctionId; }
    public AuctionStatus getCurrentStatus()   { return currentStatus; }
}
