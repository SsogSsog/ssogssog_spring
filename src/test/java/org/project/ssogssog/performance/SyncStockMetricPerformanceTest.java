package org.project.ssogssog.performance;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ssogssog.application.service.stockmetric.collect.SyncStockMetricDataUseCase;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.project.ssogssog.domain.stock.repository.DailyPriceRepository;
import org.project.ssogssog.domain.stock.repository.StockFinancialRepository;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.domain.stockmetric.repository.StockMetricRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SyncStockMetricDataUseCase 성능 측정 테스트
 *
 * [측정 목표]
 * 1. refreshAllMetrics() 전체 실행 시간
 * 2. 종목 수 대비 소요 시간 (종목당 평균 시간)
 * 3. 각 쿼리별 누적 시간 (병목 구간 파악)
 *
 * [포트폴리오용 Before/After 비교 데이터 수집]
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class SyncStockMetricPerformanceTest {

    @Autowired
    private SyncStockMetricDataUseCase syncStockMetricDataUseCase;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private DailyPriceRepository dailyPriceRepository;

    @Autowired
    private StockFinancialRepository stockFinancialRepository;

    @Autowired
    private StockMetricRepository stockMetricRepository;

    @Autowired
    private MockMvc mockMvc; // 웹 요청 흉내내는 도구

    @Test
    @DisplayName("[성능 측정] refreshAllMetrics - 최적화 전 baseline")
    void measureRefreshAllMetrics_before_optimization() {
        // given
        long stockCount = stockRepository.count();
        log.info("========================================");
        log.info("[성능 측정 시작] 총 종목 수: {}", stockCount);
        log.info("========================================");

        // when
        StopWatch stopWatch = new StopWatch("refreshAllMetrics 성능 측정");

        stopWatch.start("refreshAllMetrics 전체 실행");
        syncStockMetricDataUseCase.refreshAllMetrics();
        try {
            mockMvc.perform(post("/admin/stock-metrics/refresh"))
                    .andExpect(status().isOk());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        stopWatch.stop();

        // then
        long totalTimeMs = stopWatch.getTotalTimeMillis();
        double totalTimeSec = totalTimeMs / 1000.0;
        double avgTimePerStock = stockCount > 0 ? (double) totalTimeMs / stockCount : 0;

        log.info("========================================");
        log.info("[성능 측정 결과 - 최적화 전]");
        log.info("- 총 종목 수: {}", stockCount);
        log.info("- 전체 소요 시간: {}ms ({}초)", totalTimeMs, String.format("%.2f", totalTimeSec));
        log.info("- 종목당 평균 소요 시간: {}ms", String.format("%.2f", avgTimePerStock));
        log.info("- 예상 쿼리 수: {} (종목당 6개 쿼리 가정)", stockCount * 6);
        log.info("========================================");
        log.info(stopWatch.prettyPrint());
        log.info("========================================");
    }

    @Test
    @DisplayName("[상세 분석] 각 쿼리별 누적 시간 측정 - 병목 구간 파악")
    void measureEachQueryTime() {
        // given
        List<Stock> stocks = stockRepository.findAll();
        int stockCount = stocks.size();

        log.info("========================================");
        log.info("[쿼리별 시간 측정 시작] 총 종목 수: {}", stockCount);
        log.info("========================================");

        // 각 쿼리별 누적 시간 (나노초)
        AtomicLong totalFindLatestDailyPrice = new AtomicLong(0);
        AtomicLong totalFindLatestFinancial = new AtomicLong(0);
        AtomicLong totalFindCurrentYearFinancials = new AtomicLong(0);
        AtomicLong totalFindLastYearFinancials = new AtomicLong(0);
        AtomicLong totalFindStockMetric = new AtomicLong(0);

        int processedCount = 0;

        for (Stock stock : stocks) {
            long start, end;

            // 쿼리 1: 최신 DailyPrice 조회
            start = System.nanoTime();
            var latestDailyPrice = dailyPriceRepository.findTopByStockOrderByDateDesc(stock);
            end = System.nanoTime();
            totalFindLatestDailyPrice.addAndGet(end - start);

            // 쿼리 2: 최신 StockFinancial 조회
            start = System.nanoTime();
            var latestFinancial = stockFinancialRepository.findTopByStockOrderByYearDescQuarterDesc(stock);
            end = System.nanoTime();
            totalFindLatestFinancial.addAndGet(end - start);

            if (latestFinancial.isEmpty()) continue;

            StockFinancial sf = latestFinancial.get();
            int currentYear = sf.getYear();
            int lastYear = currentYear - 1;
            boolean isConsolidated = sf.isConsolidated();

            // 쿼리 3: 올해 분기별 데이터 조회
            start = System.nanoTime();
            stockFinancialRepository.findByStockAndYearAndIsConsolidatedOrderByQuarterAsc(
                    stock, currentYear, isConsolidated);
            end = System.nanoTime();
            totalFindCurrentYearFinancials.addAndGet(end - start);

            // 쿼리 4: 작년 분기별 데이터 조회
            start = System.nanoTime();
            stockFinancialRepository.findByStockAndYearAndIsConsolidatedOrderByQuarterAsc(
                    stock, lastYear, isConsolidated);
            end = System.nanoTime();
            totalFindLastYearFinancials.addAndGet(end - start);

            // 쿼리 5: StockMetric 조회
            start = System.nanoTime();
            stockMetricRepository.findByStock(stock);
            end = System.nanoTime();
            totalFindStockMetric.addAndGet(end - start);

            processedCount++;
        }

        // 결과 출력 (나노초 → 밀리초 변환)
        long q1Ms = totalFindLatestDailyPrice.get() / 1_000_000;
        long q2Ms = totalFindLatestFinancial.get() / 1_000_000;
        long q3Ms = totalFindCurrentYearFinancials.get() / 1_000_000;
        long q4Ms = totalFindLastYearFinancials.get() / 1_000_000;
        long q5Ms = totalFindStockMetric.get() / 1_000_000;
        long totalMs = q1Ms + q2Ms + q3Ms + q4Ms + q5Ms;

        double q1Pct = totalMs > 0 ? (q1Ms * 100.0 / totalMs) : 0;
        double q2Pct = totalMs > 0 ? (q2Ms * 100.0 / totalMs) : 0;
        double q3Pct = totalMs > 0 ? (q3Ms * 100.0 / totalMs) : 0;
        double q4Pct = totalMs > 0 ? (q4Ms * 100.0 / totalMs) : 0;
        double q5Pct = totalMs > 0 ? (q5Ms * 100.0 / totalMs) : 0;

        double q1Avg = processedCount > 0 ? (double) q1Ms / processedCount : 0;
        double q2Avg = processedCount > 0 ? (double) q2Ms / processedCount : 0;
        double q3Avg = processedCount > 0 ? (double) q3Ms / processedCount : 0;
        double q4Avg = processedCount > 0 ? (double) q4Ms / processedCount : 0;
        double q5Avg = processedCount > 0 ? (double) q5Ms / processedCount : 0;

        log.info("========================================");
        log.info("[쿼리별 누적 시간 분석 결과]");
        log.info("처리된 종목 수: {}", processedCount);
        log.info("----------------------------------------");
        log.info("쿼리 1) 최신 DailyPrice 조회:      {}ms ({}%) - 평균 {}ms/종목",
                q1Ms, String.format("%.1f", q1Pct), String.format("%.2f", q1Avg));
        log.info("쿼리 2) 최신 StockFinancial 조회:  {}ms ({}%) - 평균 {}ms/종목",
                q2Ms, String.format("%.1f", q2Pct), String.format("%.2f", q2Avg));
        log.info("쿼리 3) 올해 분기별 데이터 조회:   {}ms ({}%) - 평균 {}ms/종목",
                q3Ms, String.format("%.1f", q3Pct), String.format("%.2f", q3Avg));
        log.info("쿼리 4) 작년 분기별 데이터 조회:   {}ms ({}%) - 평균 {}ms/종목",
                q4Ms, String.format("%.1f", q4Pct), String.format("%.2f", q4Avg));
        log.info("쿼리 5) StockMetric 조회:          {}ms ({}%) - 평균 {}ms/종목",
                q5Ms, String.format("%.1f", q5Pct), String.format("%.2f", q5Avg));
        log.info("----------------------------------------");
        log.info("총 쿼리 시간 (save 제외):          {}ms", totalMs);
        log.info("========================================");
    }

    @Test
    @DisplayName("[데이터 확인] 현재 DB 상태 확인")
    void checkDatabaseStatus() {
        long stockCount = stockRepository.count();
        long dailyPriceCount = dailyPriceRepository.count();
        long stockFinancialCount = stockFinancialRepository.count();
        long stockMetricCount = stockMetricRepository.count();

        log.info("========================================");
        log.info("[DB 상태 확인]");
        log.info("- Stock 테이블:          {} rows", stockCount);
        log.info("- DailyPrice 테이블:     {} rows", dailyPriceCount);
        log.info("- StockFinancial 테이블: {} rows", stockFinancialCount);
        log.info("- StockMetric 테이블:    {} rows", stockMetricCount);
        log.info("----------------------------------------");
        if (stockCount > 0) {
            log.info("- DailyPrice 평균:       {} rows/종목", String.format("%.1f", (double) dailyPriceCount / stockCount));
            log.info("- StockFinancial 평균:   {} rows/종목", String.format("%.1f", (double) stockFinancialCount / stockCount));
        }
        log.info("========================================");
    }

    // =========================================================================
    // 최적화 버전 성능 측정
    // =========================================================================

    @Test
    @DisplayName("[성능 측정] refreshAllMetricsOptimized - 최적화 후")
    void measureRefreshAllMetrics_after_optimization() {
        // given
        long stockCount = stockRepository.count();
        log.info("========================================");
        log.info("[성능 측정 시작 - 최적화 버전] 총 종목 수: {}", stockCount);
        log.info("========================================");

        // when
        StopWatch stopWatch = new StopWatch("refreshAllMetricsOptimized 성능 측정");

        stopWatch.start("refreshAllMetricsOptimized 전체 실행");
        syncStockMetricDataUseCase.refreshAllMetricsOptimized();
        stopWatch.stop();

        // then
        long totalTimeMs = stopWatch.getTotalTimeMillis();
        double totalTimeSec = totalTimeMs / 1000.0;
        double avgTimePerStock = stockCount > 0 ? (double) totalTimeMs / stockCount : 0;

        log.info("========================================");
        log.info("[성능 측정 결과 - 최적화 후]");
        log.info("- 총 종목 수: {}", stockCount);
        log.info("- 전체 소요 시간: {}ms ({}초)", totalTimeMs, String.format("%.2f", totalTimeSec));
        log.info("- 종목당 평균 소요 시간: {}ms", String.format("%.2f", avgTimePerStock));
        log.info("- 예상 쿼리 수: 약 6개 (Bulk 조회)");
        log.info("========================================");
        log.info(stopWatch.prettyPrint());
        log.info("========================================");
    }

    @Test
    @DisplayName("[Before/After 비교] 최적화 전후 성능 비교")
    void compareBeforeAfterOptimization() {
        long stockCount = stockRepository.count();

        log.info("========================================");
        log.info("[Before/After 성능 비교 테스트]");
        log.info("총 종목 수: {}", stockCount);
        log.info("========================================");

        // 1. 최적화 전 (기존 방식)
        log.info("\n[BEFORE] 기존 방식 실행 시작...");
        StopWatch beforeWatch = new StopWatch("BEFORE");
        beforeWatch.start("refreshAllMetrics");
        syncStockMetricDataUseCase.refreshAllMetrics();
        beforeWatch.stop();
        long beforeTimeMs = beforeWatch.getTotalTimeMillis();
        log.info("[BEFORE] 완료 - {}ms ({}초)", beforeTimeMs, String.format("%.2f", beforeTimeMs / 1000.0));

        // 2. 최적화 후 (Bulk 방식)
        log.info("\n[AFTER] 최적화 방식 실행 시작...");
        StopWatch afterWatch = new StopWatch("AFTER");
        afterWatch.start("refreshAllMetricsOptimized");
        syncStockMetricDataUseCase.refreshAllMetricsOptimized();
        afterWatch.stop();
        long afterTimeMs = afterWatch.getTotalTimeMillis();
        log.info("[AFTER] 완료 - {}ms ({}초)", afterTimeMs, String.format("%.2f", afterTimeMs / 1000.0));

        // 3. 비교 결과
        double improvement = beforeTimeMs > 0 ? ((double) (beforeTimeMs - afterTimeMs) / beforeTimeMs) * 100 : 0;
        double speedup = afterTimeMs > 0 ? (double) beforeTimeMs / afterTimeMs : 0;

        log.info("\n========================================");
        log.info("[성능 비교 결과]");
        log.info("========================================");
        log.info("| 항목              | BEFORE        | AFTER         |");
        log.info("|-------------------|---------------|---------------|");
        log.info("| 총 소요 시간      | {}ms | {}ms |",
                String.format("%10d", beforeTimeMs), String.format("%10d", afterTimeMs));
        log.info("| 종목당 평균       | {}ms | {}ms |",
                String.format("%10.2f", stockCount > 0 ? (double) beforeTimeMs / stockCount : 0),
                String.format("%10.2f", stockCount > 0 ? (double) afterTimeMs / stockCount : 0));
        log.info("| 예상 쿼리 수      | {} | {} |",
                String.format("%10d", stockCount * 6), String.format("%10d", 6));
        log.info("========================================");
        log.info("성능 개선율: {}%", String.format("%.1f", improvement));
        log.info("속도 향상: {}배", String.format("%.1f", speedup));
        log.info("========================================");
    }
}
