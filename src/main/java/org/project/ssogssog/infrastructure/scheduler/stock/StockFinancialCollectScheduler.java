package org.project.ssogssog.infrastructure.scheduler.stock;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.project.ssogssog.application.service.stock.collect.CollectFinancialsUseCase;
import org.project.ssogssog.application.service.stock.collect.SyncDailyPriceUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@RequiredArgsConstructor
@Component
@Slf4j
@ConditionalOnProperty(
        name = "scheduler.financial.enabled", // 수집 인스턴스에만 해당 값 켜놓기
        havingValue = "true",
        matchIfMissing = false
)
public class StockFinancialCollectScheduler {

    private final CollectFinancialsUseCase collectFinancialsUseCase;
    private final SyncDailyPriceUseCase syncDailyPriceUseCase;

    // 스케쥴러가 빈에 등록됐는지 여부 검사
    @PostConstruct
    public void init() {
        log.info("[StockFinancialCollectScheduler] 로드 완료");
    }

    // TODO 하루에 한 번 새로운 분기의 재무제표 확인 및 업데이트 하기

    /**
     * 재무제표 누락분 재수집
     * 매일 새벽 3시에 실행 - 최근 분기의 누락된 재무제표를 수집
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "refillMissingFinancials", lockAtMostFor = "PT2H", lockAtLeastFor = "PT10M")
    public void refillMissingFinancials() {
        String runId = java.util.UUID.randomUUID().toString().substring(0, 8);
        long start = System.currentTimeMillis();

        try {
            log.info("[재무제표 누락 재수집][{}] 시작", runId);

            LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
            int year = today.getYear();
            int month = today.getMonthValue();
            int day = today.getDayOfMonth();

            String reprtCode = determineLatestReportCode(month, day);

            // 공시 기간이 아니면 스킵
            if (reprtCode == null) {
                log.info("[재무제표 누락 재수집][{}] 현재 공시 기간 아님 ({}월 {}일) - 스킵",
                        runId, month, day);
                return;
            }

            int targetYear = determineTargetYear(year, month, reprtCode);

            // TODO 예외에 대한 로그 수집 방향성 검토해보기 #85

            log.info("[재무제표 누락 재수집][{}] 대상: {}년 {}",
                    runId, targetYear, getReportName(reprtCode));

            collectFinancialsUseCase.refillMissingFinancials(targetYear, reprtCode);

            log.info("[재무제표 누락 재수집][{}] 완료 - 소요시간: {}ms",
                    runId, System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("[재무제표 누락 재수집][{}] 실패 msg={}", runId, e.getMessage(), e);
        }
    }

    /**
     * 등락률 동기화
     * 매일 새벽 4시에 실행 - 과거 데이터 중 등락률이 누락된 데이터 보완
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "syncDailyPriceChange", lockAtMostFor = "PT1H", lockAtLeastFor = "PT5M")
    public void syncDailyPriceChange() {
        String runId = java.util.UUID.randomUUID().toString().substring(0, 8);
        long start = System.currentTimeMillis();

        try {
            log.info("[등락률 동기화][{}] 시작", runId);

            syncDailyPriceUseCase.syncDailyPriceChangeAll();

            log.info("[등락률 동기화][{}] 완료 - 소요시간: {}ms",
                    runId, System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("[등락률 동기화][{}] 실패 msg={}", runId, e.getMessage(), e);
        }
    }

    /**
     * 공시 기간 기준으로 수집할 보고서 코드 결정
     * - 1/1~3/31: 사업보고서 공시 기간
     * - 4/1~5/15: 1분기 공시 기간
     * - 5/16~8/14: 반기 공시 기간
     * - 8/15~11/16: 3분기 공시 기간
     * - 11/17~12/31: 공시 없음 (null 반환)
     */
    private String determineLatestReportCode(int month, int day) {
        if (month <= 3) {
            return "11011"; // 사업보고서 (4분기) 공시 기간
        } else if (month <= 5 && (month < 5 || day <= 15)) {
            return "11013"; // 1분기 공시 기간
        } else if (month <= 8 && (month < 8 || day <= 14)) {
            return "11012"; // 반기 공시 기간
        } else if (month <= 11 && (month < 11 || day <= 16)) {
            return "11014"; // 3분기 공시 기간
        } else {
            return "11014"; // 11/17~12/31 기간에도 3분기 데이터 수집
        }
    }

    private int determineTargetYear(int currentYear, int month, String reprtCode) {
        // 사업보고서(11011)만 전년도 데이터
        if ("11011".equals(reprtCode)) {
            return currentYear - 1;
        }
        return currentYear;
    }

    private String getReportName(String reprtCode) {
        return switch (reprtCode) {
            case "11013" -> "1분기";
            case "11012" -> "반기";
            case "11014" -> "3분기";
            case "11011" -> "사업보고서(4분기)";
            default -> reprtCode;
        };
    }
}
