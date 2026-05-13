package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Thông báo gửi đến một user cụ thể.
 *
 * Ai nhận thông báo khi có bid mới?
 *   1. Seller tạo phiên đấu giá (getSellerId())
 *   2. Các buyer đã từng bid vào phiên đó (DISTINCT buyer_id FROM bids)
 *      → những người này bị "outbid" → cần biết để ra giá lại
 *
 * Persistent: lưu vào bảng notifications — không mất khi restart.
 */
public class Notification {

    // ─── Loại thông báo ──────────────────────────────────────────────────────
    public enum Type {
        /** Có bid mới trên phiên — gửi cho seller */
        NEW_BID,
        /** Buyer bị người khác vượt giá — gửi cho các buyer đã bid trước */
        OUTBID,
        /** Phiên đấu giá kết thúc (có người thắng hoặc hết hạn) */
        AUCTION_CLOSED,
        /** Phiên đấu giá bắt đầu (PENDING → ACTIVE) */
        AUCTION_STARTED,
        /** Phiên đấu giá bị seller huỷ */
        AUCTION_CANCELLED,
        /** Anti-snipe kích hoạt: phiên được gia hạn */
        ANTI_SNIPE
    }

    // ─── Fields ──────────────────────────────────────────────────────────────
    private int           id;
    private int           userId;       // người nhận
    private Integer       auctionId;    // null = thông báo hệ thống (không liên quan phiên cụ thể)
    private Type          type;
    private String        message;
    private boolean       read;
    private LocalDateTime createdAt;

    // ─── Constructors ────────────────────────────────────────────────────────

    public Notification() {}

    /** Constructor tiện dụng cho NotificationService.push() */
    public Notification(int userId, Integer auctionId, Type type, String message) {
        this.userId    = userId;
        this.auctionId = auctionId;
        this.type      = type;
        this.message   = message;
        this.read      = false;
        this.createdAt = LocalDateTime.now();
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public int           getId()        { return id; }
    public void          setId(int id)  { this.id = id; }

    public int           getUserId()             { return userId; }
    public void          setUserId(int userId)   { this.userId = userId; }

    public Integer       getAuctionId()                { return auctionId; }
    public void          setAuctionId(Integer aId)     { this.auctionId = aId; }

    public Type          getType()               { return type; }
    public void          setType(Type type)      { this.type = type; }

    public String        getMessage()                  { return message; }
    public void          setMessage(String message)    { this.message = message; }

    public boolean       isRead()                { return read; }
    public void          setRead(boolean read)   { this.read = read; }

    public LocalDateTime getCreatedAt()                    { return createdAt; }
    public void          setCreatedAt(LocalDateTime t)     { this.createdAt = t; }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Chuỗi thời gian thân thiện để hiển thị trên UI: "14:30  12/05" */
    public String getFormattedTime() {
        if (createdAt == null) return "--";
        return createdAt.format(DateTimeFormatter.ofPattern("HH:mm  dd/MM"));
    }

    /** Icon emoji theo loại thông báo — dùng trực tiếp trong Label JavaFX */
    public String getIcon() {
        if (type == null) return "🔔";
        return switch (type) {
            case NEW_BID          -> "💰";
            case OUTBID           -> "⚡";
            case AUCTION_CLOSED   -> "🏆";
            case AUCTION_STARTED  -> "🔔";
            case AUCTION_CANCELLED -> "❌";
            case ANTI_SNIPE       -> "⏱";
        };
    }

    @Override
    public String toString() {
        return "Notification{id=" + id + ", userId=" + userId
                + ", type=" + type + ", read=" + read + "}";
    }
}