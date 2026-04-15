package com.nhom9.auction.baitaplon_ltnc_nhom9.service.search;

import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.common.FilterCriteria;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.common.Page;
import com.nhom9.auction.baitaplon_ltnc_nhom9.domain.model.item.AuctionItem;

import java.util.List;

/**
 * Hợp đồng tìm kiếm và lọc vật phẩm đấu giá.
 */
public interface Searchable {

    /**
     * Tìm kiếm phân trang theo FilterCriteria.
     */
    Page<AuctionItem> search(FilterCriteria criteria, int pageNumber, int pageSize) throws Exception;

    /**
     * Gợi ý từ khoá tìm kiếm (autocomplete).
     * @param prefix tiền tố người dùng đang gõ
     * @return tối đa 10 gợi ý
     */
    List<String> suggest(String prefix) throws Exception;

    /**
     * Lấy danh sách category hiện có (có ít nhất 1 item ACTIVE).
     */
    List<String> getActiveCategories() throws Exception;
}