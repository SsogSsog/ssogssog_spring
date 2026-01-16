package org.project.ssogssog.domain.stockmetric.vo;

import org.project.ssogssog.domain.stockmetric.enums.MetricBasePeriod;

// 내부 구현은 자유도를 높이기 위해 굳이 공통 Class로 묶지 않음, 대신 필요할 때 리팩토링 고려
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

        Double minOperatingProfitRatio,
        Double maxOperatingProfitRatio,

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
