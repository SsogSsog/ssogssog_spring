package org.project.ssogssog.domain.stockmetric.vo;

import org.project.ssogssog.presentation.controller.stockmetric.enums.MetricBasePeriod;

public record StockMetricScreenerCondition(

        Integer minCurrentPrice, // 현재가
        Integer maxCurrentPrice,

        Long minMarketCap,       // 시가총액
        Long maxMarketCap,

        Double minPer,           // PER
        Double maxPer,

        Double minRoe,           // ROE
        Double maxRoe,

        Double minDebtRatio,     // 부채 비율
        Double maxDebtRatio,

        Double minSalesGrowthRatio,              // 매출액 성장률
        Double maxSalesGrowthRatio,
        MetricBasePeriod salesGrowthMetricBasePeriod,

        Double minNetProfitGrowthRatio,          // 순이익 성장률
        Double maxNetProfitGrowthRatio,
        MetricBasePeriod netProfitGrowthMetricBasePeriod,

        Double minDividendYieldRatio,            // 배당 수익률
        Double maxDividendYieldRatio,

        Double minForeignOwnershipRate,          // 외국인 보유률
        Double maxForeignOwnershipRate

) {
}
