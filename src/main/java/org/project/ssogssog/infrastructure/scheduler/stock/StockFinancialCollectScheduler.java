package org.project.ssogssog.infrastructure.scheduler.stock;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.collect.CollectFinancialsUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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

    // 스케쥴러가 빈에 등록됐는지 여부 검사
    @PostConstruct
    public void init() {
        log.info("[StockFinancialCollectScheduler] 로드 완료");
    }

    // TODO 하루에 한 번 새로운 분기의 재무제표 확인 및 업데이트 하기
}
