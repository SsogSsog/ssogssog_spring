
package org.project.ssogssog.domain.stockmetric.repository;

import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stockmetric.entity.StockMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StockMetricRepository extends JpaRepository<StockMetric, Long>, StockMetricRepositoryCustom {
    Optional<StockMetric> findByStock(Stock stock);

    /**
     * 모든 StockMetric을 Stock과 함께 조회 (FETCH JOIN)
     *
     * Bulk 조회 쿼리
     */
    @Query("SELECT sm FROM StockMetric sm JOIN FETCH sm.stock")
    List<StockMetric> findAllWithStock();
}

