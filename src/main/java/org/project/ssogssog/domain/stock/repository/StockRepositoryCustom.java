package org.project.ssogssog.domain.stock.repository;

import org.project.ssogssog.domain.stock.vo.ThemeItemDTO;

import java.util.List;

public interface StockRepositoryCustom {
    List<ThemeItemDTO> getThemeStockStats();
}
