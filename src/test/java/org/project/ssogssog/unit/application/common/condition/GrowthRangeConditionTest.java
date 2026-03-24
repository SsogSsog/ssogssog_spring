package org.project.ssogssog.unit.application.common.condition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ssogssog.domain.member.entity.range.GrowthRangeCondition;
import org.project.ssogssog.domain.stockmetric.enums.MetricBasePeriod;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GrowthRangeCondition 테스트")
class GrowthRangeConditionTest {

    @Test
    @DisplayName("min, max 둘 다 null이면 isApplied는 false")
    void isApplied_bothNull_returnsFalse() {
        GrowthRangeCondition condition = GrowthRangeCondition.builder()
                .min(null)
                .max(null)
                .basePeriod(MetricBasePeriod.PREV_QUARTER)
                .build();
        assertFalse(condition.isApplied());
    }

    @Test
    @DisplayName("min 또는 max 중 하나라도 있으면 isApplied는 true")
    void isApplied_withMinOrMax_returnsTrue() {
        GrowthRangeCondition withMin = GrowthRangeCondition.builder()
                .min(5.0)
                .basePeriod(MetricBasePeriod.PREV_YEAR)
                .build();
        assertTrue(withMin.isApplied());
    }

    @Test
    @DisplayName("범위 내 값은 matches true")
    void matches_withinRange_returnsTrue() {
        GrowthRangeCondition condition = GrowthRangeCondition.builder()
                .min(-10.0)
                .max(50.0)
                .basePeriod(MetricBasePeriod.PREV_QUARTER)
                .build();
        assertTrue(condition.matches(0.0));
        assertTrue(condition.matches(-10.0)); // 경계값
        assertTrue(condition.matches(50.0));  // 경계값
    }

    @Test
    @DisplayName("범위 밖 값은 matches false")
    void matches_outsideRange_returnsFalse() {
        GrowthRangeCondition condition = GrowthRangeCondition.builder()
                .min(0.0)
                .max(100.0)
                .basePeriod(MetricBasePeriod.PREV_YEAR)
                .build();
        assertFalse(condition.matches(-5.0));
        assertFalse(condition.matches(150.0));
    }

    @Test
    @DisplayName("value가 null이면 matches false")
    void matches_nullValue_returnsFalse() {
        GrowthRangeCondition condition = GrowthRangeCondition.builder()
                .min(0.0)
                .max(100.0)
                .basePeriod(MetricBasePeriod.PREV_QUARTER)
                .build();
        assertFalse(condition.matches(null));
    }
}
