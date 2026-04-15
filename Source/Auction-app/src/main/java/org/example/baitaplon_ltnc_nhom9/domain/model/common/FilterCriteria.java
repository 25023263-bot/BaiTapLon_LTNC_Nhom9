package org.example.baitaplon_ltnc_nhom9.domain.model.common;


import org.example.baitaplon_ltnc_nhom9.domain.model.enums.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tiêu chí lọc dùng khi tìm kiếm vật phẩm đấu giá.
 * Sử dụng Builder pattern để tạo điều kiện lọc linh hoạt.
 */
public class FilterCriteria {

    private String keyword;          // Tìm theo tên/mô tả
    private String category;         // Lọc theo danh mục
    private String itemType;         // PHYSICAL | DIGITAL | null (all)
    private AuctionStatus status;    // Trạng thái phiên đấu giá
    private BigDecimal minPrice;     // Giá hiện tại tối thiểu
    private BigDecimal maxPrice;     // Giá hiện tại tối đa
    private Integer sellerId;        // Lọc theo người bán
    private LocalDateTime startFrom; // Bắt đầu từ ngày
    private LocalDateTime endBefore; // Kết thúc trước ngày
    private boolean activeOnly;      // Chỉ phiên đang mở

    // Sắp xếp
    private String sortBy;           // price | endTime | createdAt | title
    private boolean sortAscending;   // true = ASC, false = DESC

    private FilterCriteria() {}

    // ─── Builder ────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final FilterCriteria f = new FilterCriteria();

        public Builder keyword(String keyword) {
            f.keyword = keyword;
            return this;
        }
        public Builder category(String category) {
            f.category = category;
            return this;
        }
        public Builder itemType(String itemType) {
            f.itemType = itemType;
            return this;
        }
        public Builder status(AuctionStatus status) {
            f.status = status;
            return this;
        }
        public Builder priceRange(BigDecimal min, BigDecimal max) {
            f.minPrice = min;
            f.maxPrice = max;
            return this;
        }
        public Builder sellerId(int sellerId) {
            f.sellerId = sellerId;
            return this;
        }
        public Builder timeRange(LocalDateTime from, LocalDateTime before) {
            f.startFrom = from;
            f.endBefore = before;
            return this;
        }
        public Builder activeOnly(boolean activeOnly) {
            f.activeOnly = activeOnly;
            return this;
        }
        public Builder sortBy(String field, boolean ascending) {
            f.sortBy        = field;
            f.sortAscending = ascending;
            return this;
        }
        public FilterCriteria build() { return f; }
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    public boolean hasKeyword()   { return keyword  != null && !keyword.isBlank(); }
    public boolean hasCategory()  { return category != null && !category.isBlank(); }
    public boolean hasItemType()  { return itemType != null && !itemType.isBlank(); }
    public boolean hasStatus()    { return status   != null; }
    public boolean hasPriceRange(){ return minPrice != null || maxPrice != null; }
    public boolean hasSellerId()  { return sellerId != null; }
    public boolean hasTimeRange() { return startFrom != null || endBefore != null; }
    public boolean hasSort()      { return sortBy != null && !sortBy.isBlank(); }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public String getKeyword()           { return keyword; }
    public String getCategory()          { return category; }
    public String getItemType()          { return itemType; }
    public AuctionStatus getStatus()     { return status; }
    public BigDecimal getMinPrice()      { return minPrice; }
    public BigDecimal getMaxPrice()      { return maxPrice; }
    public Integer getSellerId()         { return sellerId; }
    public LocalDateTime getStartFrom()  { return startFrom; }
    public LocalDateTime getEndBefore()  { return endBefore; }
    public boolean isActiveOnly()        { return activeOnly; }
    public String getSortBy()            { return sortBy; }
    public boolean isSortAscending()     { return sortAscending; }

    @Override
    public String toString() {
        return String.format("FilterCriteria{keyword='%s', category='%s', status=%s, price=[%s-%s]}",
                keyword, category, status, minPrice, maxPrice);
    }
}