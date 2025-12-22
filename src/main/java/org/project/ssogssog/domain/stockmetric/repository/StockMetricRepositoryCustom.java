package org.project.ssogssog.domain.stockmetric.repository;

import org.project.ssogssog.domain.stockmetric.vo.StockMetricScreenerCondition;
import org.project.ssogssog.domain.stockmetric.entity.StockMetric;

import java.util.List;

public interface StockMetricRepositoryCustom {
    List<StockMetric> getScreener(StockMetricScreenerCondition stockMetricScreenerCondition);
}
