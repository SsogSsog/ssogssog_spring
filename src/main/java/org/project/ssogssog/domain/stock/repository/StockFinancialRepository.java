package org.project.ssogssog.domain.stock.repository;

import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockFinancialRepository extends JpaRepository<StockFinancial, Long> {

    // 특정 종목의 특정 분기 재무제표가 이미 있는지 확인할 때 사용
    Optional<StockFinancial> findByStockIdAndYearAndQuarter(Long stockId, Integer year, String quarter);

    // 특정 종목의 가장 최신 재무제표 1개 조회 (보통 연도 내림차순, 분기 내림차순)
    @Query("SELECT sf FROM StockFinancial sf WHERE sf.stock.stockCode = :stockCode ORDER BY sf.year DESC, sf.quarter DESC LIMIT 1")
    Optional<StockFinancial> findLatestByStockCode(@Param("code") String stockCode);

    Optional<StockFinancial> findTopByStockOrderByYearDescQuarterDesc(Stock stock);

}
