package org.project.ssogssog.infrastructure.config.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 캐시 설정을 관리하는 enum
 */
@Getter
@RequiredArgsConstructor
public enum CacheType {

    /**
     * 급상승/급하락/거래량 TOP5 랭킹
     * - TTL: 1시간 (실시간성 중요)
     * - 크기: 100개 (rising, falling, volume 정도만)
     */
    STOCK_RANKING(
            Names.STOCK_RANKING,
            1, TimeUnit.HOURS,
            50,
            "주식 랭킹 캐시"
    ),

    /**
     * 종목별 뉴스
     * - TTL: 8시간 (외부 API 제한 고려)
     * - 크기: 1,000개 (종목코드 + 페이지 조합)
     * - 키 형식: {stockCode}:{page}
     */
    STOCK_NEWS(
            Names.STOCK_NEWS,
            8, TimeUnit.HOURS,
            300_000,
            "주식 뉴스 캐시"
    ),

    /**
     * 종목별 공시
     * - TTL: 8시간 (외부 API 제한 고려)
     * - 크기: 1,000개 (종목코드 + 페이지 조합)
     * - 키 형식: {stockCode}:{page}
     */
    STOCK_DISCLOSURES(
            Names.STOCK_DISCLOSURES,
            8, TimeUnit.HOURS,
            300_000,
            "주식 공시 캐시"
    );

    private final String cacheName;
    private final long duration;
    private final TimeUnit timeUnit;
    private final long maxSize;
    private final String description;

    /**
     * ✨ @Cacheable의 value에서 사용할 상수들
     * 컴파일 타임 상수이므로 어노테이션에서 사용 가능!
     */
    public static class Names {
        public static final String STOCK_RANKING = "stockRanking";
        public static final String STOCK_NEWS = "stockNews";
        public static final String STOCK_DISCLOSURES = "stockDisclosures";

        private Names() {} // 인스턴스화 방지
    }

    /**
     * 복합 키 생성 헬퍼
     * 예: generateKey("005930", 1) -> "005930:1"
     */
    public String generateKey(Object... params) { // 가변인자 사용
        if (params.length == 0) {
            return cacheName;
        }

        // Object들을 String으로 변환하여 리스트에 담기
        List<String> parts = new ArrayList<>();
        for (Object param : params) {
            parts.add(String.valueOf(param));
        }

        // ":" 로 연결
        return String.join(":", parts);
    }
}