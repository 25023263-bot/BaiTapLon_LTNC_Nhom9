package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.mapper;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.AuctionRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.BidRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model.AuctionCardModel;

import java.util.List;

/**
 * Ánh xạ domain {@link AuctionItem} → {@link AuctionCardModel} cho UI.
 */
public final class AuctionCardMapper {

    private final AuctionRepository auctionRepo;
    private final BidRepository bidRepo;

    public AuctionCardMapper(AuctionRepository auctionRepo, BidRepository bidRepo) {
        this.auctionRepo = auctionRepo;
        this.bidRepo = bidRepo;
    }

    public AuctionCardModel toCard(AuctionItem dbItem) {
        int bidCount = 0;
        try {
            bidCount = bidRepo.countByAuctionId(dbItem.getId());
        } catch (Exception ignored) {
        }
        return toCard(dbItem, bidCount);
    }

    public AuctionCardModel toCard(AuctionItem dbItem, int bidCount) {
        String emoji = categoryEmoji(dbItem.getCategory());
        boolean isLive = dbItem.getStatus() == AuctionStatus.ACTIVE;
        String imageUrl = dbItem.getImageUrl() != null ? dbItem.getImageUrl() : "";
        String description = dbItem.getDescription() != null ? dbItem.getDescription() : "";
        double startingPrice = dbItem.getStartingPrice() != null
                ? dbItem.getStartingPrice().doubleValue() : 0;
        return new AuctionCardModel(
                String.valueOf(dbItem.getId()),
                dbItem.getTitle(),
                dbItem.getCategory(),
                emoji,
                dbItem.getCurrentPrice().doubleValue(),
                startingPrice,
                description,
                bidCount,
                isLive,
                dbItem.getEndTime(),
                emoji,
                imageUrl,
                dbItem.getSellerId()
        );
    }

    public List<AuctionCardModel> loadByStatus(AuctionStatus status) {
        try {
            return auctionRepo.findByStatus(status).stream()
                    .map(this::toCard)
                    .toList();
        } catch (Exception e) {
            System.err.println("Lỗi load auction từ DB: " + e.getMessage());
            return List.of();
        }
    }

    public static String categoryEmoji(String category) {
        if (category == null) return "📦";
        String name = stripCategoryPrefix(category);
        return switch (name) {
            case "Điện thoại", "Điện tử" -> "📱";
            case "Laptop" -> "💻";
            case "Phần mềm" -> "💿";
            case "Game" -> "🎮";
            case "Phụ kiện" -> "🎧";
            case "Đồng hồ" -> "⌚";
            case "Trang sức" -> "💎";
            case "Nghệ thuật" -> "🎨";
            case "Đồ cổ" -> "🏺";
            case "Xe hơi" -> "🚗";
            case "Nội thất" -> "🏡";
            case "Sưu tầm" -> "🏆";
            case "Bất động sản" -> "🏢";
            default -> "📦";
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
            case ACTIVE -> "● Đang đấu giá";
            case CLOSED -> "✓ Đã kết thúc";
            case EXPIRED -> "⏰ Hết hạn";
            case PENDING -> "◷ Chờ duyệt";
            case CANCELLED -> "✕ Đã hủy";
        };
    }
}
