package org.project.ssogssog.application.common.dto.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.project.ssogssog.domain.stockmetric.enums.MetricBasePeriod;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrowthConditionDTO {

    private Double min;
    private Double max;
    private MetricBasePeriod basePeriod;

}
