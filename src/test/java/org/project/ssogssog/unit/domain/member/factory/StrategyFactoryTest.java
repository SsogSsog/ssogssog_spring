package org.project.ssogssog.unit.domain.member.factory;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ssogssog.application.common.dto.condition.GrowthConditionDTO;
import org.project.ssogssog.application.common.dto.condition.RangeConditionDTO;
import org.project.ssogssog.application.service.member.api.dto.MemberRequest;
import org.project.ssogssog.domain.member.entity.Member;
import org.project.ssogssog.domain.member.entity.Strategy;
import org.project.ssogssog.domain.member.factory.StrategyFactory;
import org.project.ssogssog.domain.stockmetric.enums.MetricBasePeriod;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StrategyFactory 단위 테스트
 */
@DisplayName("StrategyFactory 테스트")
class StrategyFactoryTest {

    @Test
    @DisplayName("RangeConditionDTO가 null이면 RangeCondition null")
    void rangeCondition_null_when_dto_is_null() {
        // Given
        MemberRequest.StrategyRequest request = MemberRequest.StrategyRequest.builder()
                .per(null)
                .roe(null)
                .build();

        // When
        Strategy result = StrategyFactory.createFrom(createMember(), "테스트", request);

        // Then
        assertNull(result.getPer());
        assertNull(result.getRoe());
    }


    @Test
    @DisplayName("GrowthConditionDTO null 또는 basePeriod 불일치면 null")
    void growthCondition_null_when_dto_null_or_period_mismatch() {
        // Given
        MemberRequest.StrategyRequest request = MemberRequest.StrategyRequest.builder()
                .salesGrowthRatio(null)  // null
                .netProfitGrowthRatio(GrowthConditionDTO.builder()
                        .min(10.0)
                        .max(50.0)
                        .basePeriod(MetricBasePeriod.PREV_QUARTER)  // QoQ인데 YoY 기대
                        .build())
                .build();

        // When
        Strategy result = StrategyFactory.createFrom(createMember(), "테스트", request);

        // Then
        // salesGrowthRatio가 null이므로 둘 다 null
        assertNull(result.getSalesGrowthQoQ());
        assertNull(result.getSalesGrowthYoY());

        // netProfitGrowthRatio는 있지만
        assertNotNull(result.getNetProfitGrowthQoQ());  // QoQ는 basePeriod 일치
        assertNull(result.getNetProfitGrowthYoY());     // YoY는 basePeriod 불일치
    }


    // 헬퍼
    private Member createMember() {
        return Member.builder()
                .build();
    }
}
