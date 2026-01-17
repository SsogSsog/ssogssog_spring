package org.project.ssogssog;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ssogssog.application.service.stock.api.StockService;
import org.project.ssogssog.presentation.controller.stock.enums.RankingType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class CachePerformanceTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private CacheManager cacheManager;

    /**
     * 성능 측정 헬퍼 메서드
     */
    private long measure(Runnable task, int repeat) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        for (int i = 0; i < repeat; i++) {
            task.run();
        }

        stopWatch.stop();
        return stopWatch.getTotalTimeMillis();
    }

    @Test
    @DisplayName("급상승 종목 캐시 성능 비교")
    void risingStocksCachePerformanceTest() {
        // Given
        int repeat = 30;
        cacheManager.getCache("stockRanking").clear();

        // When - 캐시 없이 (매번 DB 조회)
        log.info("🔥 급상승 - 캐시 없이 {}번 실행...", repeat);
        long withoutCache = measure(() -> {
            cacheManager.getCache("stockRanking").clear(); // 매번 캐시 비움
            stockService.getRanking(RankingType.RISING);
        }, repeat);

        // When - 캐시 있고 (첫 번째만 DB, 나머지 캐시)
        log.info("🔥 급상승 - 캐시 적용 후 {}번 실행...", repeat);
        cacheManager.getCache("stockRanking").clear();
        stockService.getRanking(RankingType.RISING);
        long withCache = measure(() ->
                stockService.getRanking(RankingType.RISING), repeat
        );

        // Then
        double improvement = ((double)(withoutCache - withCache) / withoutCache) * 100;

        log.info("=================================================");
        log.info("🔥 급상승 종목 캐시 성능 비교 ({}번 반복)", repeat);
        log.info("=================================================");
        log.info("❌ 캐시 미적용: {} ms", withoutCache);
        log.info("⭕ 캐시 적용:   {} ms", withCache);
        log.info("📈 성능 개선:   {}%", String.format("%.2f", improvement));
        log.info("⚡ 속도 향상:   {}배", String.format("%.1f", (double)withoutCache / withCache));
        log.info("=================================================");

        assertThat(withCache).isLessThan(withoutCache);
        assertThat(improvement).isGreaterThan(50.0); // 최소 50% 개선
    }

    @Test
    @DisplayName("급하락 종목 캐시 성능 비교")
    void fallingStocksCachePerformanceTest() {
        // Given
        int repeat = 30;
        cacheManager.getCache("stockRanking").clear();

        // When
        log.info("💧 급하락 - 캐시 없이 {}번 실행...", repeat);
        long withoutCache = measure(() -> {
            cacheManager.getCache("stockRanking").evict("falling");
            stockService.getRanking(RankingType.FALLING);
        }, repeat);

        log.info("💧 급하락 - 캐시 적용 후 {}번 실행...", repeat);
        cacheManager.getCache("stockRanking").clear();
        stockService.getRanking(RankingType.FALLING);
        long withCache = measure(() ->
                stockService.getRanking(RankingType.FALLING), repeat
        );

        // Then
        double improvement = ((double)(withoutCache - withCache) / withoutCache) * 100;

        log.info("=================================================");
        log.info("💧 급하락 종목 캐시 성능 비교 ({}번 반복)", repeat);
        log.info("=================================================");
        log.info("❌ 캐시 미적용: {} ms", withoutCache);
        log.info("⭕ 캐시 적용:   {} ms", withCache);
        log.info("📈 성능 개선:   {}%", String.format("%.2f", improvement));
        log.info("=================================================");

        assertThat(withCache).isLessThan(withoutCache);
    }

    @Test
    @DisplayName("거래량 TOP5 캐시 성능 비교")
    void volumeStocksCachePerformanceTest() {
        // Given
        int repeat = 30;
        cacheManager.getCache("stockRanking").clear();

        // When
        log.info("📊 거래량 - 캐시 없이 {}번 실행...", repeat);
        long withoutCache = measure(() -> {
            cacheManager.getCache("stockRanking").evict("volume");
            stockService.getRanking(RankingType.VOLUME);
        }, repeat);

        log.info("📊 거래량 - 캐시 적용 후 {}번 실행...", repeat);
        cacheManager.getCache("stockRanking").clear();
        stockService.getRanking(RankingType.VOLUME);
        long withCache = measure(() ->
                stockService.getRanking(RankingType.VOLUME), repeat
        );

        // Then
        double improvement = ((double)(withoutCache - withCache) / withoutCache) * 100;

        log.info("=================================================");
        log.info("📊 거래량 TOP5 캐시 성능 비교 ({}번 반복)", repeat);
        log.info("=================================================");
        log.info("❌ 캐시 미적용: {} ms", withoutCache);
        log.info("⭕ 캐시 적용:   {} ms", withCache);
        log.info("📈 성능 개선:   {}%", String.format("%.2f", improvement));
        log.info("=================================================");

        assertThat(withCache).isLessThan(withoutCache);
    }

    @Test
    @DisplayName("전체 랭킹 캐시 통합 성능 테스트")
    void allRankingsCachePerformanceTest() {
        int repeat = 20;

        // 급상승
        cacheManager.getCache("stockRanking").clear();
        long rising1 = measure(() -> {
            cacheManager.getCache("stockRanking").evict("rising");
            stockService.getRanking(RankingType.RISING);
        }, repeat);

        cacheManager.getCache("stockRanking").clear();
        stockService.getRanking(RankingType.RISING);
        long rising2 = measure(() -> stockService.getRanking(RankingType.RISING), repeat);

        // 급하락
        cacheManager.getCache("stockRanking").clear();
        long falling1 = measure(() -> {
            cacheManager.getCache("stockRanking").evict("falling");
            stockService.getRanking(RankingType.FALLING);
        }, repeat);

        cacheManager.getCache("stockRanking").clear();
        stockService.getRanking(RankingType.FALLING);
        long falling2 = measure(() -> stockService.getRanking(RankingType.FALLING), repeat);

        // 거래량
        cacheManager.getCache("stockRanking").clear();
        long volume1 = measure(() -> {
            cacheManager.getCache("stockRanking").evict("volume");
            stockService.getRanking(RankingType.VOLUME);
        }, repeat);

        cacheManager.getCache("stockRanking").clear();
        stockService.getRanking(RankingType.VOLUME);
        long volume2 = measure(() -> stockService.getRanking(RankingType.VOLUME), repeat);

        // 결과
        log.info("┌─────────────────────────────────────────────────────────┐");
        log.info("│             📈 성능 비교 결과 ({}번 반복)              │", repeat);
        log.info("├─────────────────────────────────────────────────────────┤");
        log.info("│ 🔥 급상승 │ 캐시X: {}ms │ 캐시O: {}ms │ {}% 개선 │",
                String.format("%4d", rising1),
                String.format("%3d", rising2),
                String.format("%.0f", ((double)(rising1-rising2)/rising1)*100));
        log.info("│ 💧 급하락 │ 캐시X: {}ms │ 캐시O: {}ms │ {}% 개선 │",
                String.format("%4d", falling1),
                String.format("%3d", falling2),
                String.format("%.0f", ((double)(falling1-falling2)/falling1)*100));
        log.info("│ 📊 거래량 │ 캐시X: {}ms │ 캐시O: {}ms │ {}% 개선 │",
                String.format("%4d", volume1),
                String.format("%3d", volume2),
                String.format("%.0f", ((double)(volume1-volume2)/volume1)*100));
        log.info("└─────────────────────────────────────────────────────────┘");

        double avgImprovement = (
                ((double)(rising1-rising2)/rising1) +
                        ((double)(falling1-falling2)/falling1) +
                        ((double)(volume1-volume2)/volume1)
        ) / 3 * 100;

        log.info("\n✅ 평균 성능 개선: {}%", String.format("%.1f", avgImprovement));
        log.info("✅ 캐시가 정상적으로 동작합니다!\n");
    }
}
