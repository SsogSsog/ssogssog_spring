package org.project.ssogssog.domain.stock.repository;

import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface StockFinancialRepository extends JpaRepository<StockFinancial, Long> {

    // 특정 종목의 특정 분기 재무제표가 이미 있는지 확인할 때 사용
    Optional<StockFinancial> findByStockIdAndYearAndQuarter(Long stockId, Integer year, String quarter);

    // 특정 종목의 가장 최신 재무제표 1개 조회 (연도 내림차순, 분기 내림차순)
    Optional<StockFinancial> findTopByStockOrderByYearDescQuarterDesc(Stock stock);

    // 특정 년도 + 특정 분기에 존재하는 재무제표 가져오기
    @Query("""
           select distinct sf.stock.id
           from StockFinancial sf
           where sf.year = :year
             and sf.quarter = :quarter
           """)
    List<Long> findStockIdsByYearAndQuarter(@Param("year") Integer year,
                                            @Param("quarter") String quarter);

    /**
     * 연간 실적 조회 (4Q 기준, 최근 N년)
     * 연결재무제표 우선 (isConsolidated DESC)
     */
    @Query("""
           SELECT sf FROM StockFinancial sf
           WHERE sf.stock = :stock
             AND sf.quarter = '4Q'
             AND sf.isConsolidated = :isConsolidated
           ORDER BY sf.year DESC
           LIMIT :limit
           """)
    List<StockFinancial> findAnnualByStockAndConsolidated(
            @Param("stock") Stock stock,
            @Param("isConsolidated") boolean isConsolidated,
            @Param("limit") int limit);

    /**
     * 분기 실적 조회 (최근 N분기)
     * 연결재무제표 우선 (isConsolidated DESC)
     */
    @Query("""
           SELECT sf FROM StockFinancial sf
           WHERE sf.stock = :stock
             AND sf.isConsolidated = :isConsolidated
           ORDER BY sf.year DESC, sf.quarter DESC
           LIMIT :limit
           """)
    List<StockFinancial> findQuarterlyByStockAndConsolidated(
            @Param("stock") Stock stock,
            @Param("isConsolidated") boolean isConsolidated,
            @Param("limit") int limit);

    /**
     * 가장 최신 재무제표 1건 조회 (재무 안정성 지표용)
     * 연결재무제표 우선
     */
    Optional<StockFinancial> findTopByStockAndIsConsolidatedOrderByYearDescQuarterDesc(
            Stock stock, boolean isConsolidated);
}
