package org.project.ssogssog.presentation.controller.stockmetric.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ssogssog.presentation.controller.stockmetric.enums.MarketCapBucket;
import org.project.ssogssog.presentation.controller.stockmetric.enums.MetricBasePeriod;
import org.project.ssogssog.presentation.controller.stockmetric.enums.StockPriceRange;

public class StockMetricRequest {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ScreenerRequestDTO{
        /**
         * 필터링 할 목록
         * 1. 현재가(종가 기준)
         * 2. 시가 총액
         * 3. PER
         * 4. ROE
         * 5. 부채비율
         * 6. 매출액 성장률
         * 7. 순이익 성장률
         * 8. 외국인 보유률
         * (배당 수익률, 주가 수익률의 경우는 데이터 부족으로 현재 반영x)
         */

        StockPriceRange stockPriceRange; // 현재가
        MarketCapBucket marketCapBucket; // 시가 총액

        @DecimalMin(value = "0.0", message = "PER 최소값은 0 이상이어야 합니다.")
        Double minPer; // PER

        @DecimalMin(value = "0.0", message = "PER 최대값은 0 이상이어야 합니다.")
        Double maxPer;

        Double minRoe; // ROE
        Double maxRoe;

        Double minDebtRatio; // 부채 비율
        Double maxDebtRatio;

        Double minSalesGrowthRatio; // 매출액 성장률
        Double maxSalesGrowthRatio;
        MetricBasePeriod salesGrowthMetricBasePeriod;

        @DecimalMin(value = "0.0", message = "순이익 성장률 최소값은 0 이상이어야 합니다.")
        Double minNetProfitGrowthRatio; // 순이익 성장률

        @DecimalMin(value = "0.0", message = "순이익 성장률 최대값은 0 이상이어야 합니다.")
        Double maxNetProfitGrowthRatio;
        MetricBasePeriod netProfitGrowthMetricBasePeriod;

        Double minDividendYieldRatio; // 배당 수익률
        Double maxDividendYieldRatio;

        Double minForeignOwnershipRate; // 외국인 보유률
        Double maxForeignOwnershipRate;


    }


}
