package com.nhom9.auction.baitaplon_ltnc_nhom9.ui.mapper;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.ItemDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.AuctionRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.repository.BidRepository;
import com.nhom9.auction.baitaplon_ltnc_nhom9.ui.model.AuctionCardModel;

import java.util.List;

/**
 * Ánh xạ domain {@link AuctionItem} → {@link AuctionCardModel} cho UI.
 *
 * <h3>Thay đổi so với phiên bản cũ:</h3>
 * Bổ sung {@link #toCardSimple(AuctionItem)} — static method không cần
 * {@link BidRepository}. Dùng khi dữ liệu đến từ server qua socket
 * (bidCount đã có trong AuctionItem hoặc không cần thiết ở màn hình này).
 *
 * Instance method {@link #toCard(AuctionItem)} vẫn giữ nguyên cho
 * các component cũ còn dùng trực tiếp repository.
 */
public final class AuctionCardMapper {

    private final AuctionRepository auctionRepo;
    private final BidRepository bidRepo;

    public AuctionCardMapper(AuctionRepository auctionRepo, BidRepository bidRepo) {
        this.auctionRepo = auctionRepo;
        this.bidRepo = bidRepo;
    }

    // ── Instance methods (dùng khi có BidRepository) ─────────────────────────

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

    // ── Static method (dùng khi không có BidRepository — socket path) ─────────

    /**
     * Chuyển đổi AuctionItem sang AuctionCardModel mà không cần BidRepository.
     *
     * <p>Dùng trong luồng socket (HomeCatalogPresenter, ItemDetailCoordinator)
     * khi dữ liệu đến từ server, không cần query DB thêm để đếm bid.
     * bidCount mặc định về 0; nếu server gửi kèm trong ItemDTO thì
     * dùng {@link #toCardFromDTO} thay thế.
     */
    public static AuctionCardModel toCardSimple(AuctionItem dbItem) {
        String emoji = categoryEmoji(dbItem.getCategory());
        boolean isLive = dbItem.getStatus() == AuctionStatus.ACTIVE;
        String imageUrl = dbItem.getImageUrl() != null ? dbItem.getImageUrl() : "";
        String description = dbItem.getDescription() != null ? dbItem.getDescription() : "";
        double startingPrice = dbItem.getStartingPrice() != null
                ? dbItem.getStartingPrice().doubleValue() : 0;
        double currentPrice = dbItem.getCurrentPrice() != null
                ? dbItem.getCurrentPrice().doubleValue() : startingPrice;

        return new AuctionCardModel(
                String.valueOf(dbItem.getId()),
                dbItem.getTitle(),
                dbItem.getCategory(),
                emoji,
                currentPrice,
                startingPrice,
                description,
                0,        // bidCount không có qua socket đơn giản
                isLive,
                dbItem.getEndTime(),
                emoji,
                imageUrl,
                dbItem.getSellerId()
        );
    }

    /**
     * Chuyển đổi AuctionItem sang AuctionCardModel với bidCount được cung cấp.
     *
     * <p>Dùng khi server gửi kèm bidCount (ví dụ trong ItemDTO.totalBids).
     */
    public static AuctionCardModel toCardSimple(AuctionItem dbItem, int bidCount) {
        AuctionCardModel base = toCardSimple(dbItem);
        return new AuctionCardModel(
                base.id(), base.title(), base.category(), base.categoryEmoji(),
                base.currentBid(), base.startingPrice(), base.description(),
                bidCount,
                base.isLive(), base.endTime(), base.imagePlaceholderEmoji(),
                base.imageUrl(), base.sellerId()
        );
    }

    // ── Shared static helpers ─────────────────────────────────────────────────

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
    /**
     * Chuyển ItemDTO (từ server qua socket) sang AuctionCardModel.
     * Dùng totalBids từ DTO thay vì hardcode 0 như toCardSimple(AuctionItem).
     *
     * <p>Đây là method nên dùng sau khi server đổi GET_AUCTIONS trả ItemDTO.
     */
    public static AuctionCardModel toCardFromDTO(ItemDTO dto) {
        String emoji = categoryEmoji(dto.getCategory());
        boolean isLive = dto.getStatus() == AuctionStatus.ACTIVE;
        double startingPrice = dto.getStartingPrice() != null
                ? dto.getStartingPrice().doubleValue() : 0;
        double currentPrice = dto.getCurrentPrice() != null
                ? dto.getCurrentPrice().doubleValue() : startingPrice;
        String imageUrl = dto.getImageUrl() != null ? dto.getImageUrl() : "";
        String description = dto.getDescription() != null ? dto.getDescription() : "";

        return new AuctionCardModel(
                String.valueOf(dto.getId()),
                dto.getTitle(),
                dto.getCategory(),
                emoji,
                currentPrice,
                startingPrice,
                description,
                dto.getTotalBids(),   // bidCount thực từ DB — không phải hardcode 0
                isLive,
                dto.getEndTime(),
                emoji,
                imageUrl,
                dto.getSellerId()
        );
    }

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
