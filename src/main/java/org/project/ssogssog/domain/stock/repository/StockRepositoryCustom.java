package org.project.ssogssog.domain.stock.repository;

import org.project.ssogssog.domain.stock.projection.StockItemProjection;
import org.project.ssogssog.domain.stock.projection.ThemeItemProjection;
import org.project.ssogssog.domain.stock.projection.ThemeCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StockRepositoryCustom {
    List<ThemeItemProjection> getThemeStockStats();

    Page<StockItemProjection> getStocksForThemeOrderByClosePrice(String theme, Pageable pageable);

    ThemeCountProjection getThemeCount(String theme);

    // 자동완성용 검색
    List<StockItemProjection> searchAutocomplete(String keyword, int limit);

    // 전체 검색
    Page<StockItemProjection> search(String keyword, Pageable pageable);

}
