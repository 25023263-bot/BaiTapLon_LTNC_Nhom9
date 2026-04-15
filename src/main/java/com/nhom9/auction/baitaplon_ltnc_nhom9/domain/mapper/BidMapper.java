package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.mapper;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.BidDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Chuyển đổi giữa Bid model và BidDTO.
 */
public class BidMapper {

    private BidMapper() {}

    // ─── Model → DTO ─────────────────────────────────────────────────────────

    public static BidDTO toDTO(Bid bid) {
        if (bid == null) return null;

        BidDTO dto = new BidDTO();
        dto.setId(bid.getId());
        dto.setItemId(bid.getItemId());
        dto.setBidderId(bid.getBidderId());
        dto.setBidderUsername(bid.getBidderUsername());
        dto.setAmount(bid.getAmount());
        dto.setBidTime(bid.getBidTime());
        dto.setAutoBid(bid.isAutoBid());
        return dto;
    }

    public static List<BidDTO> toDTOList(List<Bid> bids) {
        if (bids == null) return List.of();
        return bids.stream().map(BidMapper::toDTO).collect(Collectors.toList());
    }

    /**
     * Chuyển list và đánh dấu bid đầu tiên (cao nhất) là leading.
     * List phải được sort descending theo amount trước khi gọi.
     */
    public static List<BidDTO> toDTOListWithLeading(List<Bid> bids) {
        List<BidDTO> dtos = toDTOList(bids);
        if (!dtos.isEmpty()) dtos.get(0).setLeading(true);
        return dtos;
    }

    // ─── DTO → Model ─────────────────────────────────────────────────────────

    public static Bid toModel(BidDTO dto) {
        if (dto == null) return null;

        Bid bid = new Bid();
        bid.setId(dto.getId());
        bid.setItemId(dto.getItemId());
        bid.setBidderId(dto.getBidderId());
        bid.setBidderUsername(dto.getBidderUsername());
        bid.setAmount(dto.getAmount());
        bid.setBidTime(dto.getBidTime());
        bid.setAutoBid(dto.isAutoBid());
        return bid;
    }
}