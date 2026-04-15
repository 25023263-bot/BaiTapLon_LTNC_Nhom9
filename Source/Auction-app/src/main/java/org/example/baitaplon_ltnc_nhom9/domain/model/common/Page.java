package org.example.baitaplon_ltnc_nhom9.domain.model.common;

import java.util.Collections;
import java.util.List;

/**
 * Kết quả phân trang tổng quát.
 * Sử dụng Generic để dùng cho mọi loại entity.
 *
 * @param <T> loại đối tượng trong danh sách
 */
public class Page<T> {

    private final List<T> content;     // Danh sách kết quả trang hiện tại
    private final int pageNumber;      // Trang hiện tại (bắt đầu từ 0)
    private final int pageSize;        // Số item mỗi trang
    private final int totalElements;   // Tổng số item
    private final int totalPages;      // Tổng số trang

    // ─── Constructor ────────────────────────────────────────────────────────

    public Page(List<T> content, int pageNumber, int pageSize, int totalElements) {
        this.content       = content != null ? content : Collections.emptyList();
        this.pageNumber    = pageNumber;
        this.pageSize      = pageSize > 0 ? pageSize : 10;
        this.totalElements = totalElements;
        this.totalPages    = pageSize > 0
                ? (int) Math.ceil((double) totalElements / pageSize)
                : 0;
    }

    /** Trang trống tiện lợi */
    public static <T> Page<T> empty(int pageSize) {
        return new Page<>(Collections.emptyList(), 0, pageSize, 0);
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    public boolean hasContent()       { return !content.isEmpty(); }
    public boolean isFirstPage()      { return pageNumber == 0; }
    public boolean isLastPage()       { return pageNumber >= totalPages - 1; }
    public boolean hasNextPage()      { return pageNumber < totalPages - 1; }
    public boolean hasPreviousPage()  { return pageNumber > 0; }

    /** Offset dùng trong SQL: pageNumber * pageSize */
    public int getOffset()            { return pageNumber * pageSize; }

    // ─── Getters ─────────────────────────────────────────────────────────────

    public List<T> getContent()       { return content; }
    public int getPageNumber()        { return pageNumber; }
    public int getPageSize()          { return pageSize; }
    public int getTotalElements()     { return totalElements; }
    public int getTotalPages()        { return totalPages; }

    @Override
    public String toString() {
        return String.format("Page{page=%d/%d, size=%d, total=%d, items=%d}",
                pageNumber + 1, totalPages, pageSize, totalElements, content.size());
    }
}