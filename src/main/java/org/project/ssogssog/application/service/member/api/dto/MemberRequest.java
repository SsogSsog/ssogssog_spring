package org.project.ssogssog.application.service.member.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ssogssog.application.common.dto.condition.GrowthConditionDTO;
import org.project.ssogssog.application.common.dto.condition.RangeConditionDTO;
import org.project.ssogssog.presentation.controller.stockmetric.enums.MarketCapBucket;
import org.project.ssogssog.presentation.controller.stockmetric.enums.StockPriceRange;

public class MemberRequest {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotBlank(message = "uuid는 필수입니다.")
        @Size(max = 36, message = "uuid는 최대 36자까지 가능합니다.") // TODO 추후 @Pattern 방식으로 변경하기
        private String uuid;
        private String fcm;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StrategyRequest {

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
