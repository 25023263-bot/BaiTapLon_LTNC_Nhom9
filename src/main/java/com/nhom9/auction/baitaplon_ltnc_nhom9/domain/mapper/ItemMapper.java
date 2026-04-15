package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.mapper;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.ItemDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.DigitalItem;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.PhysicalItem;

/**
 * Chuyển đổi giữa AuctionItem model và ItemDTO.
 */
public class ItemMapper {

    private ItemMapper() {}

    // ─── Model → DTO ─────────────────────────────────────────────────────────

    public static ItemDTO toDTO(AuctionItem item) {
        if (item == null) return null;

        ItemDTO dto = new ItemDTO();
        dto.setId(item.getId());
        dto.setSellerId(item.getSellerId());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setCategory(item.getCategory());
        dto.setImageUrl(item.getImageUrl());
        dto.setItemType(item.getItemType());
        dto.setStartingPrice(item.getStartingPrice());
        dto.setMinBidIncrement(item.getMinBidIncrement());
        dto.setBuyNowPrice(item.getBuyNowPrice());
        dto.setCurrentPrice(item.getCurrentPrice());
        dto.setLeadingBidderId(item.getLeadingBidderId());
        dto.setStatus(item.getStatus());
        dto.setStartTime(item.getStartTime());
        dto.setEndTime(item.getEndTime());
        dto.setCreatedAt(item.getCreatedAt());

        if (item instanceof PhysicalItem p) {
            dto.setCondition(p.getCondition());
            dto.setWeightGrams(p.getWeightGrams());
            dto.setDimensions(p.getDimensions());
            dto.setLocation(p.getLocation());
            dto.setShippingCost(p.getShippingCost());
            dto.setAllowPickup(p.isAllowPickup());

        } else if (item instanceof DigitalItem d) {
            dto.setDigitalType(d.getDigitalType());
            dto.setPlatform(d.getPlatform());
            dto.setFileSizeMB(d.getFileSizeMB());
            dto.setExpiryDate(d.getExpiryDate());
            dto.setReplacementGuarantee(d.isReplacementGuarantee());
            // deliveryContent KHÔNG copy vào DTO vì bảo mật
        }

        return dto;
    }

    // ─── DTO → Model ─────────────────────────────────────────────────────────

    public static AuctionItem toModel(ItemDTO dto) {
        if (dto == null) return null;
        if ("DIGITAL".equalsIgnoreCase(dto.getItemType())) return toDigitalItem(dto);
        return toPhysicalItem(dto);
    }

    public static PhysicalItem toPhysicalItem(ItemDTO dto) {
        if (dto == null) return null;
        PhysicalItem item = new PhysicalItem();
        applyCommonFields(item, dto);
        item.setCondition(dto.getCondition());
        item.setWeightGrams(dto.getWeightGrams());
        item.setDimensions(dto.getDimensions());
        item.setLocation(dto.getLocation());
        item.setShippingCost(dto.getShippingCost());
        item.setAllowPickup(dto.isAllowPickup());
        return item;
    }

    public static DigitalItem toDigitalItem(ItemDTO dto) {
        if (dto == null) return null;
        DigitalItem item = new DigitalItem();
        applyCommonFields(item, dto);
        item.setDigitalType(dto.getDigitalType());
        item.setPlatform(dto.getPlatform());
        item.setFileSizeMB(dto.getFileSizeMB());
        item.setExpiryDate(dto.getExpiryDate());
        item.setReplacementGuarantee(dto.isReplacementGuarantee());
        return item;
    }

    private static void applyCommonFields(AuctionItem item, ItemDTO dto) {
        item.setId(dto.getId());
        item.setSellerId(dto.getSellerId());
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setCategory(dto.getCategory());
        item.setImageUrl(dto.getImageUrl());
        item.setStartingPrice(dto.getStartingPrice());
        item.setMinBidIncrement(dto.getMinBidIncrement());
        item.setBuyNowPrice(dto.getBuyNowPrice());
        item.setCurrentPrice(dto.getCurrentPrice());
        item.setLeadingBidderId(dto.getLeadingBidderId());
        item.setStatus(dto.getStatus());
        item.setStartTime(dto.getStartTime());
        item.setEndTime(dto.getEndTime());
        item.setCreatedAt(dto.getCreatedAt());
    }
}