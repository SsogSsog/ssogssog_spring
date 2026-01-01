package org.project.ssogssog.domain.stock.repository;

import org.project.ssogssog.domain.stock.vo.StockItemDTO;
import org.project.ssogssog.domain.stock.vo.ThemeItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StockRepositoryCustom {
    List<ThemeItemDTO> getThemeStockStats();

    Page<StockItemDTO> getStocksForThemeOrderByClosePrice(String theme, Pageable pageable);

}
