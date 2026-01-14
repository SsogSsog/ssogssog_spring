package org.project.ssogssog.application.common.dto.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RangeConditionDTO {

    private Double min;
    private Double max;

}
