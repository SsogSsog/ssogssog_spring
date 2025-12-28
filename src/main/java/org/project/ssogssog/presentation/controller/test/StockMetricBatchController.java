package org.project.ssogssog.presentation.controller.test;

import lombok.RequiredArgsConstructor;
import org.project.ssogssog.application.service.stockmetric.usecase.SyncStockMetricDataUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/stock-metrics")
@RequiredArgsConstructor
public class StockMetricBatchController {

    private final SyncStockMetricDataUseCase syncStockMetricDataUseCase;

    /**
     * 전체 종목 StockMetric 배치 실행 (테스트용)
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshAll() {
        syncStockMetricDataUseCase.refreshAllMetrics();
        return ResponseEntity.ok().build();
    }
}