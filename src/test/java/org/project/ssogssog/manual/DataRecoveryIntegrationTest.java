package org.project.ssogssog.manual;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ssogssog.application.service.stock.collect.CollectFinancialsUseCase;
import org.project.ssogssog.application.service.stock.collect.CollectHistoricalPricesUseCase;
import org.project.ssogssog.application.service.stock.collect.CollectTodayPricesUseCase;
import org.project.ssogssog.application.service.stock.collect.SyncDailyPriceUseCase;
import org.project.ssogssog.application.service.stock.collect.SyncStockDataUseCase;
import org.project.ssogssog.application.service.stockmetric.collect.SyncStockMetricDataUseCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 누락된 데이터를 일괄 업데이트하기 위한 통합 테스트
 *
 * 장기간 서비스 중단 후 재배포 시 사용:
 * 1. 과거 일별시세 수집
 * 2. 등락률 동기화
 * 3. 재무제표 수집 (최근 분기)
 * 4. 투자 지표 재계산
 */
@Slf4j
@SpringBootTest
@Disabled("수동 실행 전용 - CI/CD에서 실행 방지. IDE에서 개별 메서드 클릭하면 실행됨")
class DataRecoveryIntegrationTest {

    @Autowired
    private CollectHistoricalPricesUseCase collectHistoricalPricesUseCase;

    @Autowired
    private CollectTodayPricesUseCase collectTodayPricesUseCase;

    @Autowired
    private SyncDailyPriceUseCase syncDailyPriceUseCase;

    @Autowired
    private CollectFinancialsUseCase collectFinancialsUseCase;

    @Autowired
    private SyncStockDataUseCase syncStockDataUseCase;

    @Autowired
    private SyncStockMetricDataUseCase syncStockMetricDataUseCase;

    // ==================== 개별 실행용 테스트 ====================

    @Test
    @DisplayName("1. 과거 N개월 일별시세 수집 (KIS API)")
    void step1_collectHistoricalPrices() {
        // 과거부터 현재까지 빠진 데이터 수집
        int months = 4; // 수집할 월 정보 입력
        log.info("=== 과거 {}개월 일별시세 수집 시작 ===", months);
        collectHistoricalPricesUseCase.fetchAndSavePastPrices(months);
        log.info("=== 과거 일별시세 수집 완료 ===");
    }

    @Test
    @DisplayName("2. 오늘 시세 수집 (당일 데이터)")
    void step2_collectTodayPrices() {
        log.info("=== 오늘 시세 수집 시작 ===");
        collectTodayPricesUseCase.updateAllStockPrices();
        log.info("=== 오늘 시세 수집 완료 ===");
    }

    @Test
    @DisplayName("3. 등락률 동기화 (changeRate, changePrice 계산)")
    void step3_syncDailyPriceChange() {
        log.info("=== 등락률 동기화 시작 ===");
        syncDailyPriceUseCase.syncDailyPriceChangeAll();
        log.info("=== 등락률 동기화 완료 ===");
    }

    @Test
    @DisplayName("4. 누락된 재무제표 수집")
    void step4collectFinancials() {
        log.info("=== 2026년 1분기 재무제표 수집 시작 ===");
        // 1분기: 11013
        collectFinancialsUseCase.updateAllFinancials(2026, "11013");
        log.info("=== 2026년 1분기 재무제표 수집 완료 ===");
    }


    @Test
    @DisplayName("5. 섹터/CorpCode 누락 데이터 보완")
    void step5_syncStockData() {
        log.info("=== 섹터 정보 업데이트 시작 ===");
        syncStockDataUseCase.updateMissingSectors();
        log.info("=== 섹터 정보 업데이트 완료 ===");

        log.info("=== CorpCode 업데이트 시작 ===");
        syncStockDataUseCase.fillCorpCodes();
        log.info("=== CorpCode 업데이트 완료 ===");
    }

    @Test
    @DisplayName("6. 투자 지표(StockMetric) 전체 재계산")
    void step6_refreshAllMetrics() {
        log.info("=== 투자 지표 재계산 시작 ===");
        syncStockMetricDataUseCase.refreshAllMetricsOptimized();
        log.info("=== 투자 지표 재계산 완료 ===");
    }

    // ==================== 전체 순차 실행 ====================

    @Test
    @DisplayName("전체 데이터 복구 (순차 실행 - 주의: 시간이 오래 걸림)")
    void fullDataRecovery() {
        log.info("========================================");
        log.info("전체 데이터 복구 프로세스 시작");
        log.info("========================================");

        // Step 1: 과거 일별시세 수집 (과거~현재)
        log.info("\n[Step 1/6] 과거 일별시세 수집...");
        collectHistoricalPricesUseCase.fetchAndSavePastPrices(4);

        // Step 2: 오늘 시세 수집
        log.info("\n[Step 2/6] 오늘 시세 수집...");
        collectTodayPricesUseCase.updateAllStockPrices();

        // Step 3: 등락률 동기화
        log.info("\n[Step 3/6] 등락률 동기화...");
        syncDailyPriceUseCase.syncDailyPriceChangeAll();

        // Step 4: 재무제표 수집 (필요한 분기들)
        log.info("\n[Step 4/6] 재무제표 수집...");
        collectFinancialsUseCase.updateAllFinancials(2026, "11013"); // 2024 4분기
        //collectFinancialsUseCase.refillMissingFinancials(2024, "11011");

        // Step 5: 섹터/CorpCode 보완
        log.info("\n[Step 5/6] 섹터/CorpCode 보완...");
        syncStockDataUseCase.updateMissingSectors();
        syncStockDataUseCase.fillCorpCodes();

        // TODO 선행 단계 검증 후 메서드 실행 체크(#85)
        // Step 6: 투자 지표 재계산
        log.info("\n[Step 6/6] 투자 지표 재계산...");
        syncStockMetricDataUseCase.refreshAllMetricsOptimized();

        log.info("========================================");
        log.info("전체 데이터 복구 프로세스 완료!");
        log.info("========================================");
    }

    // ==================== 유틸리티 ====================

    @Test
    @DisplayName("특정 종목 재무제표 디버깅")
    void debugFinancialData() {
        // stockId를 변경하여 특정 종목 확인
        Long stockId = 1L;
        collectFinancialsUseCase.debugOpenDartAccountNames(stockId, 2024, "11011");
    }
}
