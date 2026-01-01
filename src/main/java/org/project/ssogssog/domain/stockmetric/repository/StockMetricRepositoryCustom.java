package org.project.ssogssog.domain.stockmetric.repository;

import org.project.ssogssog.domain.stockmetric.vo.StockMetricScreenerCondition;
import org.project.ssogssog.domain.stockmetric.entity.StockMetric;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface StockMetricRepositoryCustom {
    Slice<StockMetric> getScreener(StockMetricScreenerCondition stockMetricScreenerCondition, Pageable pageable);
}
