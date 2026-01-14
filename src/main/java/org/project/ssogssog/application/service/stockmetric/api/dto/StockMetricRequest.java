package org.project.ssogssog.application.service.stockmetric.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ssogssog.application.common.dto.condition.GrowthConditionDTO;
import org.project.ssogssog.application.common.dto.condition.RangeConditionDTO;
import org.project.ssogssog.presentation.controller.stockmetric.enums.MarketCapBucket;
import org.project.ssogssog.presentation.controller.stockmetric.enums.StockPriceRange;

public class StockMetricRequest {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ScreenerRequestDTO {
        /**
         * 필터링 할 목록
         * 1. 현재가(종가 기준)
         * 2. 시가 총액
         * 3. PER
         * 4. ROE
         * 5. 부채비율
         * 6. 영업이익률
         * 7. 매출액 성장률
         * 8. 순이익 성장률
         * 9. 배당 수익률
         * 10. 외국인 보유률
         * (주가 수익률의 경우는 데이터 부족으로 현재 반영x)
         */

        StockPriceRange stockPriceRange; // 현재가
        MarketCapBucket marketCapBucket; // 시가 총액

        RangeConditionDTO per;                      // PER
        RangeConditionDTO roe;                      // ROE
        RangeConditionDTO debtRatio;                // 부채 비율
        RangeConditionDTO operatingProfitRatio;    // 영업 이익률
        GrowthConditionDTO salesGrowthRatio;       // 매출액 성장률
        GrowthConditionDTO netProfitGrowthRatio;   // 순이익 성장률
        RangeConditionDTO dividendYieldRatio;      // 배당 수익률
        RangeConditionDTO foreignOwnershipRate;    // 외국인 보유률

    }

}
