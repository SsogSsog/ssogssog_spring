package org.project.ssogssog.domain.stock.repository;

import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.springframework.data.domain.Pageable;
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
     * 분기 실적 조회 (최근 N분기)
     * 연결재무제표 우선 (isConsolidated DESC)
     */
    @Query("""
           SELECT sf FROM StockFinancial sf
           WHERE sf.stock = :stock
             AND sf.isConsolidated = :isConsolidated
           ORDER BY sf.year DESC, sf.quarter DESC
           """)
    List<StockFinancial> findQuarterlyByStockAndConsolidated(
            @Param("stock") Stock stock,
            @Param("isConsolidated") boolean isConsolidated,
            Pageable pageable);

    /**
     * 가장 최신 재무제표 1건 조회 (재무 안정성 지표용)
     * 연결재무제표 우선
     */
    Optional<StockFinancial> findTopByStockAndIsConsolidatedOrderByYearDescQuarterDesc(
            Stock stock, boolean isConsolidated);

    /**
     * 특정 종목의 특정 연도 모든 분기 데이터 조회 (TTM 계산용)
     */
    List<StockFinancial> findByStockAndYearAndIsConsolidatedOrderByQuarterAsc(
            Stock stock, Integer year, boolean isConsolidated);



    /**
     * 모든 종목의 최신 StockFinancial 한 번에 조회
     * ROW_NUMBER로 각 종목별 최신 1건만 선택
     *
     * Bulk 조회 쿼리
     */
    @Query(value = """
        SELECT *
        FROM (
            SELECT sf.*,
                   ROW_NUMBER() OVER (
                       PARTITION BY sf.stock_id
                       ORDER BY sf.year DESC, sf.quarter DESC, sf.is_consolidated DESC
                   ) as rn
            FROM stock_financial sf
        ) t
        WHERE t.rn = 1
        """, nativeQuery = true)
    List<StockFinancial> findAllLatestByStock();


    /**
     * 특정 연도 목록의 모든 재무 데이터 조회 (올해 + 작년 한 번에)
     *
     * Bulk 조회 쿼리
     */
    @Query("SELECT sf FROM StockFinancial sf WHERE sf.year IN :years")
    List<StockFinancial> findByYearIn(@Param("years") List<Integer> years);
}
