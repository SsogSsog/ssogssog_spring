package org.project.ssogssog.presentation.controller.stock.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RankingType {
    RISING("rising"),
    FALLING("falling"),
    VOLUME("volume");

    private final String cacheKey;

    // 캐시 이름 (모든 랭킹이 공유)
    public static final String CACHE_NAME = "stockRanking";

}
