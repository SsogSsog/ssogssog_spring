package org.project.ssogssog.application.service.cache.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.member.reader.MemberCacheReader;
import org.project.ssogssog.application.service.stock.reader.StockCacheReader;
import org.project.ssogssog.domain.member.event.StockLikeUpdatedEvent;
import org.project.ssogssog.domain.member.event.StrategyUpdatedEvent;
import org.project.ssogssog.domain.stock.event.DailyPriceUpdatedEvent;
import org.project.ssogssog.domain.stock.event.StockUpdatedEvent;
import org.project.ssogssog.infrastructure.config.cache.CacheType;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주식 데이터 업데이트 이벤트를 수신하여 캐시를 무효화하는 리스너
 *
 * @CacheEvict를 통해 선언적으로 캐시를 무효화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockCacheEvictionListener {

    private final MemberCacheReader memberCacheReader;
    private final StockCacheReader stockCacheReader;

    /**
     * 일별시세 업데이트 시 랭킹/테마 통계 캐시 무효화
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(DailyPriceUpdatedEvent event) {
        log.info("[CacheEviction] 랭킹/테마 통계 캐시 무효화 완료");

        stockCacheReader.evictRanking();
        stockCacheReader.evictThemeStats();
    }

    /**
     * 주식 기본 정보 업데이트 시 테마 통계 캐시 무효화
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(StockUpdatedEvent event) {
        log.info("[CacheEviction] 테마 통계 캐시 무효화 완료");

        stockCacheReader.evictRanking();
        stockCacheReader.evictThemeStats();
    }

    /**
     * 좋아요 변경 시 해당 회원의 좋아요 캐시 무효화
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(StockLikeUpdatedEvent event) {
        log.info("[CacheEviction] 회원 좋아요 캐시 무효화 완료 - uuid: {}", event.memberUuid());

        memberCacheReader.evictLikedStocks(event.memberUuid());
    }

    /**
     * 전략 변경 시 해당 회원의 전략 캐시 무효화
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(StrategyUpdatedEvent event) {
        log.info("[CacheEviction] 회원 전략 캐시 무효화 완료 - uuid: {}", event.memberUuid());

        memberCacheReader.evictStrategies(event.memberUuid());
    }
}
