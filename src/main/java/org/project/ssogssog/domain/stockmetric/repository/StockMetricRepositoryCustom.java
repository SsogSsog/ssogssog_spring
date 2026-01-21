package org.project.ssogssog.domain.stockmetric.repository;

import org.project.ssogssog.domain.stock.projection.StockItemProjection;
import org.project.ssogssog.domain.stockmetric.vo.StockMetricScreenerCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface StockMetricRepositoryCustom {
    Slice<StockItemProjection> getScreener(StockMetricScreenerCondition stockMetricScreenerCondition, Pageable pageable);
}
