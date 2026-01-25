package org.project.ssogssog.application.service.stockmetric.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stockmetric.reader.StockMetricBulkDataReader;
import org.project.ssogssog.application.service.stockmetric.reader.StockMetricBulkDataReader.BulkData;
import org.project.ssogssog.application.service.stockmetric.writer.StockMetricBulkWriter;
import org.project.ssogssog.application.service.stockmetric.writer.StockMetricBulkWriter.ProcessResult;
import org.project.ssogssog.application.service.stockmetric.writer.StockMetricWriter;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class SyncStockMetricDataUseCase {

    private final StockRepository stockRepository;

    private final StockMetricWriter stockMetricWriter;

    private final StockMetricBulkDataReader bulkDataFetcher;
    private final StockMetricBulkWriter bulkWriter;


    /**
     * 전체 종목의 StockMetric 계산 (일별시세 수집 후 이어서 실행)
     * 종목별 독립적인 업데이트를 의도했어서 해당 메서드에 @Transactional을 걸지 않음
     */
    public void refreshAllMetrics(){

        List<Stock> stocks = stockRepository.findAll();

        log.info("총 {}개 종목 메트릭 갱신 시작...", stocks.size());
        int success = 0;
        int failed = 0;

        for (Stock stock : stocks) {
            try {
                stockMetricWriter.refreshMetricForStock(stock);
                success++;
            } catch (Exception e) {
                failed++;
                log.error("메트릭 갱신 실패 - 종목: {}({}), 에러: {}",
                        stock.getCorpName(), stock.getStockCode(), e.getMessage());
                }
        }

        log.info("✅ 메트릭 갱신 완료. 성공: {}, 실패: {}, 전체: {}", success, failed, stocks.size());


    }

    /**
     * [최적화 버전] 전체 종목의 StockMetric 계산
     *
     * 기존 방식 vs 최적화 방식:
     * - 기존: 종목당 6개 쿼리 × 3000종목 = 18,000+ 쿼리
     * - 최적화: 6개 Bulk 쿼리 + 메모리 매핑 + 1개 Batch Insert
     *
     * 구조:
     * 1. BulkDataReader: 모든 데이터를 한 번에 조회 → Map으로 매핑
     * 2. BulkWriter: 메모리 데이터로 계산 → Batch 저장
     */
    public void refreshAllMetricsOptimized() {
        StopWatch stopWatch = new StopWatch("refreshAllMetricsOptimized");

        // 1. Bulk 데이터 조회 (6개 쿼리로 모든 데이터 수집)
        stopWatch.start("bulkDataFetch");
        BulkData bulkData = bulkDataFetcher.fetchAll();
        stopWatch.stop();
        log.info("[Phase 1] 데이터 조회 완료 - {}ms", stopWatch.getLastTaskTimeMillis());

        // 2. 계산 및 Batch 저장
        stopWatch.start("calculateAndSave");
        ProcessResult result = bulkWriter.calculateAndSaveAll(bulkData);
        stopWatch.stop();
        log.info("[Phase 2] 계산 및 저장 완료 - {}ms", stopWatch.getLastTaskTimeMillis());

        // 결과 출력
        log.info("========================================");
        log.info("메트릭 갱신 완료");
        log.info("- 성공: {}, 스킵: {}, 실패: {}", result.success(), result.skipped(), result.failed());
        log.info("- 총 소요 시간: {}ms ({}초)", stopWatch.getTotalTimeMillis(),
                String.format("%.2f", stopWatch.getTotalTimeMillis() / 1000.0));
        log.info("========================================");
    }

}

