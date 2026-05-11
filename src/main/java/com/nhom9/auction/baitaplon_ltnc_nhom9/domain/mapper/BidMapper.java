package com.nhom9.auction.baitaplon_ltnc_nhom9.domain.mapper;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.dto.BidDTO;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.Bid;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Map between {@link Bid} and {@link BidDTO}.
 */
public class BidMapper {

    private BidMapper() {}

    public static BidDTO toDTO(Bid bid) {
        if (bid == null) return null;

        BidDTO dto = new BidDTO();
        dto.setId(bid.getId());
        dto.setAuctionId(bid.getAuctionId());
        dto.setBuyerId(bid.getBuyerId());
        dto.setBuyerUsername(bid.getBuyerUsername());
        dto.setAmount(bid.getAmount());
        dto.setBidTime(bid.getBidTime());
        dto.setAutoBid(bid.isAutoBid());
        return dto;
    }

    public static List<BidDTO> toDTOList(List<Bid> bids) {
        if (bids == null) return List.of();
        return bids.stream().map(BidMapper::toDTO).collect(Collectors.toList());
    }

    /** List must be sorted by amount descending before call. */
    public static List<BidDTO> toDTOListWithLeading(List<Bid> bids) {
        List<BidDTO> dtos = toDTOList(bids);
        if (!dtos.isEmpty()) dtos.get(0).setLeading(true);
        return dtos;
    }

    public static Bid toModel(BidDTO dto) {
        if (dto == null) return null;

        Bid bid = new Bid();
        bid.setId(dto.getId());
        bid.setAuctionId(dto.getAuctionId());
        bid.setBuyerId(dto.getBuyerId());
        bid.setBuyerUsername(dto.getBuyerUsername());
        bid.setAmount(dto.getAmount());
        bid.setBidTime(dto.getBidTime());
        bid.setAutoBid(dto.isAutoBid());
        return bid;
    }
}
