package org.project.ssogssog.domain.stock.repository;

import org.project.ssogssog.presentation.controller.stock.dto.StockResponse;

import java.util.List;

public interface StockRepositoryCustom {
    List<StockResponse.ThemeItemDTO> findStocksGroupedPerSector();
}
