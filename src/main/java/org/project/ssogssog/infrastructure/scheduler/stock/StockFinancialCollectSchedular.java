package org.project.ssogssog.infrastructure.scheduler.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.usecase.CollectFinancialsUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@ConditionalOnProperty(
        name = "scheduler.today-price.enabled", // 수집 인스턴스에만 해당 값 켜놓기
        havingValue = "true",
        matchIfMissing = false
)
public class StockFinancialCollectSchedular {

    private final CollectFinancialsUseCase collectFinancialsUseCase;

    // TODO 하루에 한 번 새로운 분기의 재무제표 확인 및 업데이트 하기
}
