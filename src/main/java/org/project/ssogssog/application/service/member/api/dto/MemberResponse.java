package org.project.ssogssog.application.service.member.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ssogssog.application.common.dto.condition.GrowthConditionDTO;
import org.project.ssogssog.application.common.dto.condition.RangeConditionDTO;
import org.project.ssogssog.presentation.controller.stockmetric.enums.MarketCapBucket;
import org.project.ssogssog.presentation.controller.stockmetric.enums.StockPriceRange;

import java.util.List;

public class MemberResponse {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RegisterResponse{
        private Long memberId;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StrategyResponse {
        private Long strategyId;
        private String strategyName;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StrategiesResponse {
        private List<StrategyDetailResponse> strategies;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StrategyDetailResponse {
        private Long strategyId;
        private String strategyName;

        private StockPriceRange stockPriceRange;
        private MarketCapBucket marketCapBucket;

        private RangeConditionDTO per;
        private RangeConditionDTO roe;
        private RangeConditionDTO debtRatio;
        private RangeConditionDTO operatingProfitMargin;
        private RangeConditionDTO netProfitMargin;

        private GrowthConditionDTO salesGrowthQoQ;
        private GrowthConditionDTO salesGrowthYoY;
        private GrowthConditionDTO netProfitGrowthQoQ;
        private GrowthConditionDTO netProfitGrowthYoY;

        private RangeConditionDTO dividendYield;
        private RangeConditionDTO foreignOwnershipRate;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class LikeResponse {
        private Long stockId;
        private boolean liked;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class LikedStocksResponse {
        private List<LikedStockDetail> stocks;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class LikedStockDetail {
        private Long stockId;
        private String stockCode;
        private String corpName;
        private String sector;
        private Integer closePrice;
        private Double changeRate;
    }
}
