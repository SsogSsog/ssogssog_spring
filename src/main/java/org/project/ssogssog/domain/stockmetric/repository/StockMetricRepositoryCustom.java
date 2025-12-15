package org.project.ssogssog.domain.stockmetric.repository;

import org.project.ssogssog.application.stockmetric.api.dto.StockMetricScreenerCondition;
import org.project.ssogssog.domain.stockmetric.entity.StockMetric;

import java.util.List;

public interface StockMetricRepositoryCustom {
    List<StockMetric> getScreener(StockMetricScreenerCondition stockMetricScreenerCondition);
}
