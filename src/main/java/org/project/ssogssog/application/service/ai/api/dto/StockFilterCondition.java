package org.project.ssogssog.application.service.ai.api.dto;

import org.project.ssogssog.domain.stockmetric.enums.MetricBasePeriod;

/**
 * 자연어 종목 검색 질문을 LLM이 구조화된 필터 조건으로 변환한 결과.
 * 각 필드는 질문에 언급된 조건만 채우고, 언급되지 않은 조건은 null(제약 없음)이다.
 * 도메인의 {@link org.project.ssogssog.domain.stockmetric.vo.StockMetricScreenerCondition}
 * 스크리너 지원 지표와 1:1로 대응한다.
 */
public record StockFilterCondition(

        Integer minCurrentPrice, // 현재가 (원)
        Integer maxCurrentPrice,

        Long minMarketCap,       // 시가총액 (원)
        Long maxMarketCap,

        Double minPer,           // PER (주가수익비율)
        Double maxPer,

        Double minRoe,           // ROE (자기자본이익률, %)
        Double maxRoe,

        Double minDebtRatio,     // 부채비율 (%)
        Double maxDebtRatio,

        Double minOperatingProfitRatio, // 영업이익률 (%)
        Double maxOperatingProfitRatio,

        Double minSalesGrowthRatio,     // 매출액 성장률 (%)
        Double maxSalesGrowthRatio,
        MetricBasePeriod salesGrowthMetricBasePeriod, // 매출 성장률 기준 기간(직전분기 대비 / 전년동기 대비)

        Double minNetProfitGrowthRatio, // 순이익 성장률 (%)
        Double maxNetProfitGrowthRatio,
        MetricBasePeriod netProfitGrowthMetricBasePeriod, // 순이익 성장률 기준 기간

        Double minDividendYieldRatio,   // 배당수익률 (%)
        Double maxDividendYieldRatio,

        Double minForeignOwnershipRate, // 외국인 보유율 (%)
        Double maxForeignOwnershipRate

) {
}
