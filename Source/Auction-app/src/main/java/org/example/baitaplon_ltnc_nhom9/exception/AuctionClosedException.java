package org.example.baitaplon_ltnc_nhom9.exception;

import org.example.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;

/**
 * Ném ra khi cố đặt bid vào phiên đấu giá đã đóng, hết hạn hoặc bị huỷ.
 */
public class AuctionClosedException extends Exception {

    private final int itemId;
    private final AuctionStatus currentStatus;

    public AuctionClosedException(int itemId, AuctionStatus currentStatus) {
        super(String.format("Phiên đấu giá #%d không còn nhận bid (trạng thái: %s).",
                itemId, currentStatus.getDisplayName()));
        this.itemId        = itemId;
        this.currentStatus = currentStatus;
    }

    public int getItemId()                    { return itemId; }
    public AuctionStatus getCurrentStatus()   { return currentStatus; }
}