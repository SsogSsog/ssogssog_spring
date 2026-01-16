package org.project.ssogssog.application.service.member.api.converter;

import org.project.ssogssog.application.common.dto.condition.GrowthConditionDTO;
import org.project.ssogssog.application.common.dto.condition.RangeConditionDTO;
import org.project.ssogssog.application.service.member.api.dto.MemberResponse;
import org.project.ssogssog.domain.member.entity.Strategy;
import org.project.ssogssog.domain.member.entity.range.GrowthRangeCondition;
import org.project.ssogssog.domain.member.entity.range.RangeCondition;

public class StrategyConverter {

    public static MemberResponse.StrategyDetailResponse toDetailResponse(Strategy strategy) {
        return MemberResponse.StrategyDetailResponse.builder()
                .strategyId(strategy.getId())
                .strategyName(strategy.getStrategyName())
                .stockPriceRange(strategy.getStockPriceRange())
                .marketCapBucket(strategy.getMarketCapBucket())
                .per(toRangeConditionDTO(strategy.getPer()))
                .roe(toRangeConditionDTO(strategy.getRoe()))
                .debtRatio(toRangeConditionDTO(strategy.getDebtRatio()))
                .operatingProfitMargin(toRangeConditionDTO(strategy.getOperatingProfitMargin()))
                .netProfitMargin(toRangeConditionDTO(strategy.getNetProfitMargin()))
                .salesGrowthQoQ(toGrowthConditionDTO(strategy.getSalesGrowthQoQ()))
                .salesGrowthYoY(toGrowthConditionDTO(strategy.getSalesGrowthYoY()))
                .netProfitGrowthQoQ(toGrowthConditionDTO(strategy.getNetProfitGrowthQoQ()))
                .netProfitGrowthYoY(toGrowthConditionDTO(strategy.getNetProfitGrowthYoY()))
                .dividendYield(toRangeConditionDTO(strategy.getDividendYield()))
                .foreignOwnershipRate(toRangeConditionDTO(strategy.getForeignOwnershipRate()))
                .build();
    }

    private static RangeConditionDTO toRangeConditionDTO(RangeCondition condition) {
        if (condition == null) {
            return null;
        }
        return RangeConditionDTO.builder()
                .min(condition.getMin())
                .max(condition.getMax())
                .build();
    }

    private static GrowthConditionDTO toGrowthConditionDTO(GrowthRangeCondition condition) {
        if (condition == null) {
            return null;
        }
        return GrowthConditionDTO.builder()
                .min(condition.getMin())
                .max(condition.getMax())
                .basePeriod(condition.getBasePeriod())
                .build();
    }
}
