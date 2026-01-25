package org.project.ssogssog.infrastructure.scheduler.stock;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.project.ssogssog.application.service.stock.usecase.CollectTodayPricesUseCase;
import org.project.ssogssog.application.service.stockmetric.usecase.SyncStockMetricDataUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@ConditionalOnProperty(
        name = "scheduler.today-price.enabled", // 수집 인스턴스에만 해당 값 켜놓기
        havingValue = "true",
        matchIfMissing = false
)
public class TodayPriceCollectScheduler {

    private final CollectTodayPricesUseCase collectTodayPricesUseCase;
    private final SyncStockMetricDataUseCase syncStockMetricDataUseCase;

    // 스케쥴러가 빈에 등록됐는지 여부 검사
    @PostConstruct
    public void init() {
        log.info("[TodayPriceCollectScheduler] 로드 완료!!!");
    }


    // 30분 마다 실행
    @Scheduled(cron = "0 */30 9-17 * * MON-FRI", zone = "Asia/Seoul")
    @SchedulerLock(name = "collectTodayPricesAndRefreshStockMetricData", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void collectTodayPricesAndRefreshStockMetricData() {

        String runId = java.util.UUID.randomUUID().toString().substring(0, 8);
        long start = System.currentTimeMillis();

        // TODO: 외부 API 보호 적용 해보기!! (타임아웃 + 재시도(5xx/timeout) + circuit breaker(401/429 연속 실패 시 fail-fast))
        // TODO: 모의 서버 만들어서 보호가 잘 됐는지 결과까지 내보기!


        // 일별 시세 업데이트
        try {
            log.info("[일별 시세 수집][{}] 시작", runId);

            collectTodayPricesUseCase.updateAllStockPrices();
        } catch (Exception e) {
            log.error("[일별 시세 수집][{}] 실패 msg={}", runId, e.getMessage(), e);
            // TODO: 모니터링 연결 및 에러 로그 추가하기
        }

        // StockMetric 업데이트 (스크리너 계산용)
        try {
            log.info("[주식 통계 업데이트][{}] 시작", runId);

            syncStockMetricDataUseCase.refreshAllMetricsOptimized();
        } catch (Exception e) {
            log.error("[주식 통계 업데이트][{}] 실패 msg={}", runId, e.getMessage(), e);
        }

        log.info("[일별 시세 수집 완료][{}] end {}ms", runId, System.currentTimeMillis() - start);

    }

}
