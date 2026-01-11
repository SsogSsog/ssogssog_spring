package org.project.ssogssog.presentation.controller.stock.enums;

import lombok.Getter;

@Getter
public enum RankingType {
    RISING("rising"),
    FALLING("falling"),
    VOLUME("volume");

    private final String cacheKey;

    RankingType(String cacheKey) {
        this.cacheKey = cacheKey;
    }
}
