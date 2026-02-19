package org.project.ssogssog.unit.application.common.condition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ssogssog.domain.member.entity.range.RangeCondition;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RangeCondition 테스트")
class RangeConditionTest {

    @Test
    @DisplayName("min, max 둘 다 null이면 isApplied는 false")
    void isApplied_bothNull_returnsFalse() {
        RangeCondition condition = RangeCondition.of(null, null);
        assertFalse(condition.isApplied());
    }

    @Test
    @DisplayName("min 또는 max 중 하나라도 있으면 isApplied는 true")
    void isApplied_minOnly_returnsTrue() {
        RangeCondition condition = RangeCondition.of(10.0, null);
        assertTrue(condition.isApplied());
    }

    @Test
    @DisplayName("범위 내 값은 matches true")
    void matches_withinRange_returnsTrue() {
        RangeCondition condition = RangeCondition.of(10.0, 20.0);
        assertTrue(condition.matches(15.0));
        assertTrue(condition.matches(10.0)); // 경계값
        assertTrue(condition.matches(20.0)); // 경계값
    }

    @Test
    @DisplayName("범위 밖 값은 matches false")
    void matches_outsideRange_returnsFalse() {
        RangeCondition condition = RangeCondition.of(10.0, 20.0);
        assertFalse(condition.matches(5.0));  // min 미만
        assertFalse(condition.matches(25.0)); // max 초과
    }

    @Test
    @DisplayName("조건 미적용 시 항상 true")
    void matches_notApplied_alwaysTrue() {
        RangeCondition condition = RangeCondition.of(null, null);
        assertTrue(condition.matches(999.0));
        assertTrue(condition.matches(null));
    }
}
