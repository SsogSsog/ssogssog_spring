package org.project.ssogssog.domain.stock.repository;

import org.project.ssogssog.application.service.stock.api.dto.ThemeItemDTO;

import java.util.List;

public interface StockRepositoryCustom {
    List<ThemeItemDTO> getThemeStockStats();
}
