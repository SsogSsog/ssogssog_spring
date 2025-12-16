package org.project.ssogssog.domain.stock.repository;

import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface StockFinancialRepository extends JpaRepository<StockFinancial, Long> {

    // 특정 종목의 특정 분기 재무제표가 이미 있는지 확인할 때 사용
    Optional<StockFinancial> findByStockIdAndYearAndQuarter(Long stockId, Integer year, String quarter);

    // 특정 종목의 가장 최신 재무제표 1개 조회 (연도 내림차순, 분기 내림차순)
    Optional<StockFinancial> findTopByStockOrderByYearDescQuarterDesc(Stock stock);

}
