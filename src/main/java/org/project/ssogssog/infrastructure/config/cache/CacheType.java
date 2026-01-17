package org.project.ssogssog.infrastructure.config.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
            Values.STOCK_RANKING,
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
            Values.STOCK_NEWS,
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
            Values.STOCK_DISCLOSURES,
            8, TimeUnit.HOURS,
            300_000,
            "주식 공시 캐시"
    ),

    /**
     * 회원별 좋아요한 주식 목록
     * - TTL: 24시간 (변경 시 CacheEvict로 무효화)
     * - 키 형식: {uuid}
     */
    MEMBER_LIKED_STOCKS(
            Values.MEMBER_LIKED_STOCKS,
            1, TimeUnit.HOURS,
            10_000,
            "회원 좋아요 주식 캐시"
    ),

    /**
     * 회원별 저장한 전략 목록
     * - TTL: 24시간 (변경 시 CacheEvict로 무효화)
     * - 키 형식: {uuid}
     */
    MEMBER_STRATEGIES(
            Values.MEMBER_STRATEGIES,
            1, TimeUnit.HOURS,
            100,
            "회원 전략 캐시"
    );

    private final String cacheName;
    private final long duration;
    private final TimeUnit timeUnit;
    private final long maxSize;
    private final String description;

    /**
     * ✨ @Cacheable의 value에서 사용할 상수들
     */
    public static class Values {
        public static final String STOCK_RANKING = "stockRanking";
        public static final String STOCK_NEWS = "stockNews";
        public static final String STOCK_DISCLOSURES = "stockDisclosures";
        public static final String MEMBER_LIKED_STOCKS = "memberLikedStocks";
        public static final String MEMBER_STRATEGIES = "memberStrategies";

        private Values() {} // 인스턴스화 방지
    }

    /**
     * ✨ @Cacheable의 key에서 사용할 상수들
     */
    public static class Keys{
        public static final String STOCK_RANKING = "#type.cacheKey";
        public static final String STOCK_NEWS = "#stockCode + ':' + #page";
        public static final String STOCK_DISCLOSURE = "#stockCode + ':' + #page";
        public static final String MEMBER_UUID = "#uuid";
    }
}