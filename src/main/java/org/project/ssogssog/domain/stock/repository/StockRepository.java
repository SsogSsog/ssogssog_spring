package org.project.ssogssog.domain.stock.repository;

import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.presentation.controller.stock.dto.StockResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long>, StockRepositoryCustom{
    Optional<Stock> findByStockCode(String stockCode);

    List<Stock> findBySectorIsNull();

}
