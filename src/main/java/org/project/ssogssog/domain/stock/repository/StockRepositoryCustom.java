package org.project.ssogssog.domain.stock.repository;

import org.project.ssogssog.domain.stock.projection.StockItemDTO;
import org.project.ssogssog.domain.stock.projection.ThemeItemDTO;
import org.project.ssogssog.domain.stock.projection.ThemeCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StockRepositoryCustom {
    List<ThemeItemDTO> getThemeStockStats();

    Page<StockItemDTO> getStocksForThemeOrderByClosePrice(String theme, Pageable pageable);

    ThemeCountProjection getThemeCount(String theme);

}
