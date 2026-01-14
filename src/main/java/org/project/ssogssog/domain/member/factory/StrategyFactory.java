package org.project.ssogssog.domain.member.factory;

import org.project.ssogssog.application.common.dto.condition.GrowthConditionDTO;
import org.project.ssogssog.application.common.dto.condition.RangeConditionDTO;
import org.project.ssogssog.application.service.member.api.dto.MemberRequest;
import org.project.ssogssog.domain.member.entity.Member;
import org.project.ssogssog.domain.member.entity.Strategy;
import org.project.ssogssog.domain.member.entity.range.GrowthRangeCondition;
import org.project.ssogssog.domain.member.entity.range.RangeCondition;
import org.project.ssogssog.domain.stockmetric.enums.MetricBasePeriod;

public class StrategyFactory {

    public static Strategy createFrom(Member member, String strategyName, MemberRequest.StrategyRequest request) {
        return Strategy.builder()
                .member(member)
                .strategyName(strategyName)
                .stockPriceRange(request.getStockPriceRange())
                .marketCapBucket(request.getMarketCapBucket())
                .per(toRangeCondition(request.getPer()))
                .roe(toRangeCondition(request.getRoe()))
                .debtRatio(toRangeCondition(request.getDebtRatio()))
                .operatingProfitMargin(toRangeCondition(request.getOperatingProfitRatio()))
                .salesGrowthQoQ(toGrowthRangeCondition(request.getSalesGrowthRatio(), MetricBasePeriod.PREV_QUARTER))
                .salesGrowthYoY(toGrowthRangeCondition(request.getSalesGrowthRatio(), MetricBasePeriod.PREV_YEAR))
                .netProfitGrowthQoQ(toGrowthRangeCondition(request.getNetProfitGrowthRatio(), MetricBasePeriod.PREV_YEAR))
                .netProfitGrowthYoY(toGrowthRangeCondition(request.getNetProfitGrowthRatio(), MetricBasePeriod.PREV_YEAR))
                .dividendYield(toRangeCondition(request.getDividendYieldRatio()))
                .foreignOwnershipRate(toRangeCondition(request.getForeignOwnershipRate()))
                .build();
    }

    private static RangeCondition toRangeCondition(RangeConditionDTO dto) {
        if (dto == null) {
            return null;
        }
        return RangeCondition.of(dto.getMin(), dto.getMax());
    }

    private static GrowthRangeCondition toGrowthRangeCondition(
            GrowthConditionDTO dto,
            MetricBasePeriod expectedPeriod
    ) {
        if (dto == null || dto.getBasePeriod() != expectedPeriod) {
            return null;
        }
        return GrowthRangeCondition.builder()
                .min(dto.getMin())
                .max(dto.getMax())
                .basePeriod(dto.getBasePeriod())
                .build();
    }
}
