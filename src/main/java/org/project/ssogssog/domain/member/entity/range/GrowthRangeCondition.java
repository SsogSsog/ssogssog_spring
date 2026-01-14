package org.project.ssogssog.domain.member.entity.range;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ssogssog.domain.stockmetric.enums.MetricBasePeriod;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrowthRangeCondition {

    private Double min;
    private Double max;

    @Enumerated(EnumType.STRING)
    private MetricBasePeriod basePeriod;

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
}
