package org.project.ssogssog.domain.stockmetric.repository;

import org.project.ssogssog.domain.stock.projection.StockItemProjection;
import org.project.ssogssog.domain.stockmetric.vo.StockMetricScreenerCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockMetricRepositoryCustom {
    Page<StockItemProjection> getScreener(StockMetricScreenerCondition stockMetricScreenerCondition, Pageable pageable);
}
