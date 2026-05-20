package com.nhom9.auction.baitaplon_ltnc_nhom9.server.protocol;

import java.io.Serializable;

/**
 * Object client gửi lên server qua socket.
 * Type cho biết client muốn làm gì.
 * Payload là dữ liệu kèm theo (DTO tương ứng với từng type).
 */
public class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        LOGIN,
        REGISTER,
        GET_AUCTIONS,
        GET_AUCTION_DETAIL,
        PLACE_BID,
        PLACE_AUTO_BID,
        CANCEL_AUCTION,
        DEPOSIT_WALLET,
        LOGOUT,
        // --- Seller ---
        CREATE_LISTING,
        UPGRADE_TO_SELLER,
        UPDATE_AUCTION,
        // --- Admin ---
        GET_USERS,
        TOGGLE_USER_LOCK,
        // --- Notifications ---
        GET_NOTIFICATIONS,
        MARK_NOTIFICATION_READ,
        MARK_ALL_NOTIFICATIONS_READ,
        CLEAR_NOTIFICATIONS
    }

    private final Type type;
    private final Object payload;

    public Request(Type type, Object payload) {
        this.type    = type;
        this.payload = payload;
    }

    public Type getType()      { return type; }
    public Object getPayload() { return payload; }

    @Override
    public String toString() {
        return "Request{type=" + type + "}";
    }
}