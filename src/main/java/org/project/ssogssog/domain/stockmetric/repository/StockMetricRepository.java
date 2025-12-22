
package org.project.ssogssog.domain.stockmetric.repository;

import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stockmetric.entity.StockMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockMetricRepository extends JpaRepository<StockMetric, Long>, StockMetricRepositoryCustom {
    Optional<StockMetric> findByStock(Stock stock);
}

