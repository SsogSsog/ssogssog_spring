package org.project.ssogssog.presentation.controller.stock.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 비즈니스 로직 관리
 */
@Getter
@RequiredArgsConstructor
public enum RankingType {
    RISING("rising"),
    FALLING("falling"),
    VOLUME("volume");

    private final String cacheKey;

}
