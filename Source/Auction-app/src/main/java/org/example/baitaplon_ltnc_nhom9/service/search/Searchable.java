package org.example.baitaplon_ltnc_nhom9.service.search;

import org.example.baitaplon_ltnc_nhom9.model.FilterCriteria;
import org.example.baitaplon_ltnc_nhom9.model.Page;
import java.util.List;

public interface Searchable<T> {
    List<T> search(String keyword);
    List<T> filter(FilterCriteria criteria);
    Page<T> search(String keyword, int page, int pageSize);
}