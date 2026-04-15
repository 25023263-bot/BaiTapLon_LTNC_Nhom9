package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.helpers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Tiện ích định dạng thời gian và tính đếm ngược cho UI.
 */
public class DateTimeUtils {

    private DateTimeUtils() {}

    private static final DateTimeFormatter FULL    = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter SHORT   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE    = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME    = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ─── Format ───────────────────────────────────────────────────────────────

    public static String formatFull(LocalDateTime t)  { return t != null ? t.format(FULL)  : "–"; }
    public static String formatShort(LocalDateTime t) { return t != null ? t.format(SHORT) : "–"; }
    public static String formatDate(LocalDateTime t)  { return t != null ? t.format(DATE)  : "–"; }
    public static String formatTime(LocalDateTime t)  { return t != null ? t.format(TIME)  : "–"; }

    // ─── Countdown ───────────────────────────────────────────────────────────

    /**
     * Định dạng thời gian còn lại thành chuỗi dễ đọc.
     * Ví dụ: "2 ngày 3 giờ", "45 phút 12 giây", "Đã kết thúc"
     */
    public static String formatCountdown(LocalDateTime endTime) {
        if (endTime == null) return "–";
        long totalSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);

        if (totalSeconds <= 0) return "Đã kết thúc";

        long days    = totalSeconds / 86400;
        long hours   = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (days > 0)    return String.format("%d ngày %d giờ", days, hours);
        if (hours > 0)   return String.format("%d giờ %d phút", hours, minutes);
        if (minutes > 0) return String.format("%d phút %d giây", minutes, seconds);
        return String.format("%d giây", seconds);
    }

    /**
     * Định dạng đếm ngược dạng HH:MM:SS (dùng cho timer nhỏ).
     */
    public static String formatCountdownShort(LocalDateTime endTime) {
        if (endTime == null) return "00:00:00";
        long totalSeconds = Math.max(0, ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime));

        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Màu sắc CSS dựa trên thời gian còn lại:
     * - > 1 giờ  : "countdown-normal"  (xanh)
     * - 10-60 ph : "countdown-warning" (vàng)
     * - < 10 ph  : "countdown-urgent"  (đỏ)
     */
    public static String getCountdownStyleClass(LocalDateTime endTime) {
        if (endTime == null) return "countdown-normal";
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);
        if (seconds <= 0)       return "countdown-expired";
        if (seconds < 600)      return "countdown-urgent";
        if (seconds < 3600)     return "countdown-warning";
        return "countdown-normal";
    }

    // ─── Relative ─────────────────────────────────────────────────────────────

    /**
     * "vừa xong", "5 phút trước", "2 giờ trước", "3 ngày trước"
     */
    public static String formatRelative(LocalDateTime past) {
        if (past == null) return "–";
        long seconds = ChronoUnit.SECONDS.between(past, LocalDateTime.now());

        if (seconds < 60)    return "vừa xong";
        if (seconds < 3600)  return (seconds / 60) + " phút trước";
        if (seconds < 86400) return (seconds / 3600) + " giờ trước";
        return (seconds / 86400) + " ngày trước";
    }

    // ─── Currency ─────────────────────────────────────────────────────────────

    /**
     * Format tiền tệ: 1500000 → "1.500.000 đ"
     */
    public static String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) return "0 đ";
        return String.format("%,.0f đ", amount);
    }
}