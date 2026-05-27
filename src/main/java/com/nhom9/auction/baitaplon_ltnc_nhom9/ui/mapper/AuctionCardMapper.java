package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.mapper;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.ItemDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model.AuctionCardModel;

/**
 * Ánh xạ {@link ItemDTO} (từ server qua socket) → {@link AuctionCardModel} cho UI.
 *
 * <p>Class này chỉ chứa static methods — không cần khởi tạo instance.
 * Tất cả dữ liệu đều đến từ server qua socket dưới dạng {@link ItemDTO},
 * nên không cần inject {@code AuctionRepository} hay {@code BidRepository}.
 */
public final class AuctionCardMapper {

    private AuctionCardMapper() {}

    /**
     * Chuyển đổi {@link ItemDTO} (từ server) sang {@link AuctionCardModel}.
     * Sử dụng {@code totalBids} thực từ DB thay vì hardcode 0.
     */
    public static AuctionCardModel toCardFromDTO(ItemDTO dto) {
        String emoji        = categoryEmoji(dto.getCategory());
        boolean isLive      = dto.getStatus() == AuctionStatus.ACTIVE;
        double startingPrice = dto.getStartingPrice() != null
                ? dto.getStartingPrice().doubleValue() : 0;
        double currentPrice  = dto.getCurrentPrice() != null
                ? dto.getCurrentPrice().doubleValue() : startingPrice;
        String imageUrl      = dto.getImageUrl()     != null ? dto.getImageUrl()     : "";
        String description   = dto.getDescription()  != null ? dto.getDescription()  : "";

        return new AuctionCardModel(
                String.valueOf(dto.getId()),
                dto.getTitle(),
                dto.getCategory(),
                emoji,
                currentPrice,
                startingPrice,
                description,
                dto.getTotalBids(),
                isLive,
                dto.getEndTime(),
                emoji,
                imageUrl,
                dto.getSellerId()
        );
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    public static String categoryEmoji(String category) {
        if (category == null) return "📦";
        String name = stripCategoryPrefix(category);
        return switch (name) {
            case "Điện thoại", "Điện tử" -> "📱";
            case "Laptop"                -> "💻";
            case "Phần mềm"              -> "💿";
            case "Game"                  -> "🎮";
            case "Phụ kiện"              -> "🎧";
            case "Đồng hồ"               -> "⌚";
            case "Trang sức"             -> "💎";
            case "Nghệ thuật"            -> "🎨";
            case "Đồ cổ"                 -> "🏺";
            case "Xe hơi"                -> "🚗";
            case "Nội thất"              -> "🏡";
            case "Sưu tầm"               -> "🏆";
            case "Bất động sản"          -> "🏢";
            default                      -> "📦";
        };
    }

    /** Bỏ emoji đầu chuỗi danh mục từ ComboBox (vd. "⌚ Đồng hồ" → "Đồng hồ"). */
    public static String stripCategoryPrefix(String category) {
        if (category == null || category.isBlank()) return "";
        int space = category.indexOf(' ');
        if (space > 0 && space < category.length() - 1) {
            return category.substring(space + 1).trim();
        }
        return category.trim();
    }

    public static String statusDisplay(AuctionStatus status) {
        if (status == null) return "Không rõ";
        return switch (status) {
            case ACTIVE    -> "● Đang đấu giá";
            case CLOSED    -> "✓ Đã kết thúc";
            case EXPIRED   -> "⏰ Hết hạn";
            case PENDING   -> "◷ Chờ duyệt";
            case CANCELLED -> "✕ Đã hủy";
        };
    }
}
