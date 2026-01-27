package org.project.ssogssog.domain.stockmetric.vo;

/**
 * StockMetricCalculator의 계산 결과를 담는 값 객체(VO).
 * - 불변(immutable)
 * - 값 기반 equals/hashCode/toString 제공
 */
public record MetricValues(
        Integer currentPrice,
        Long marketCap,

        Double per,
        Double pbr,
        Double roe,
        Double netProfitMargin,
        Double debtRatio,
        Double operatingProfitMargin,

        Double salesGrowthQoQ,
        Double salesGrowthYoY,
        Double netProfitGrowthQoQ,
        Double netProfitGrowthYoY,

        Double dividendYield,
        Double foreignOwnershipRate,

        Double return3M,
        Double return6M,
        Double return12M
) {
}