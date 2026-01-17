package org.project.ssogssog.domain.member.entity.range;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RangeCondition {

    private Double min;
    private Double max;

    // 해당 조건이 존재하는지 검사
    public boolean isApplied() {
        return min != null || max != null;
    }

    // 해당 조건을 만족하는지 검사
    public boolean matches(Double value) {
        if (!isApplied()) return true;
        if (value == null) return false;

        if (min != null && value < min) return false;
        if (max != null && value > max) return false;

        return true;
    }

    public static RangeCondition of(Double min, Double max) {
        return new RangeCondition(min, max);
    }
}
