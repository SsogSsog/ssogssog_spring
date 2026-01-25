package org.project.ssogssog.application.service.stockmetric.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stockmetric.reader.StockMetricBulkDataReader.BulkData;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.project.ssogssog.domain.stockmetric.entity.StockMetric;
import org.project.ssogssog.domain.stockmetric.factory.StockMetricCalculator;
import org.project.ssogssog.domain.stockmetric.repository.StockMetricRepository;
import org.project.ssogssog.domain.stockmetric.vo.MetricValues;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * StockMetric Bulk 계산 및 저장 담당
 * - 메모리에 매핑된 데이터로 계산 수행 (DB 조회 없음)
 * - Batch Insert/Update로 한 번에 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockMetricBulkWriter {

    private final StockMetricRepository stockMetricRepository;

    /**
     * Bulk 데이터를 기반으로 모든 종목의 메트릭 계산 및 저장
     *
     * @param bulkData 미리 조회된 데이터
     * @return 처리 결과 (성공/실패 건수)
     */
    @Transactional
    public ProcessResult calculateAndSaveAll(BulkData bulkData) {
        StopWatch stopWatch = new StopWatch("BulkWriter");

        List<Stock> stocks = bulkData.getStocks();
        log.info("[BulkWriter] 메트릭 계산 시작 - 총 {}개 종목", stocks.size());

        // 계산 결과를 담을 리스트
        List<StockMetric> metricsToSave = new ArrayList<>();

        int success = 0;
        int skipped = 0;
        int failed = 0;

        // 계산 단계 (DB 접근 없음, 순수 메모리 연산)
        stopWatch.start("calculateMetrics");
        for (Stock stock : stocks) {
            try {
                StockMetric metric = calculateMetricForStock(stock, bulkData);
                if (metric != null) {
                    metricsToSave.add(metric);
                    success++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                failed++;
                log.error("[BulkWriter] 계산 실패 - 종목: {}({}), 에러: {}",
                        stock.getCorpName(), stock.getStockCode(), e.getMessage());
            }
        }
        stopWatch.stop();
        log.info("[BulkWriter] 계산 완료: 성공={}, 스킵={}, 실패={}, 소요시간={}ms",
                success, skipped, failed, stopWatch.getLastTaskTimeMillis());

        // 저장 단계 (Batch Insert/Update)
        stopWatch.start("saveAll");
        stockMetricRepository.saveAll(metricsToSave);
        stopWatch.stop();
        log.info("[BulkWriter] 저장 완료: {}개, 소요시간={}ms",
                metricsToSave.size(), stopWatch.getLastTaskTimeMillis());

        log.info("[BulkWriter] 전체 처리 완료 - 총 {}ms", stopWatch.getTotalTimeMillis());

        return new ProcessResult(success, skipped, failed, stopWatch.getTotalTimeMillis());
    }

    /**
     * 단일 종목 메트릭 계산 (메모리 데이터만 사용)
     */
    private StockMetric calculateMetricForStock(Stock stock, BulkData bulkData) {
        Long stockId = stock.getId();

        // 1. 최신 DailyPrice 조회 (메모리)
        DailyPrice latestDailyPrice = bulkData.getLatestDailyPriceMap().get(stockId);
        if (latestDailyPrice == null) {
            log.debug("[{}] 스킵 - DailyPrice 없음", stock.getStockCode());
            return null;
        }

        // 2. 최신 StockFinancial 조회 (메모리) - 분기 정보 파악용
        StockFinancial latestFinancial = bulkData.getLatestFinancialMap().get(stockId);
        if (latestFinancial == null) {
            log.debug("[{}] 스킵 - StockFinancial 없음", stock.getStockCode());
            return null;
        }

        final int currentYear = latestFinancial.getYear();
        final String currentQuarter = latestFinancial.getQuarter();
        final int lastYear = currentYear - 1;

        // 3. 올해/작년 분기별 재무 데이터 조회 (메모리)
        Map<String, StockFinancial> currentYearMap = bulkData.getFinancialsByYear(stockId, currentYear);
        Map<String, StockFinancial> lastYearMap = bulkData.getFinancialsByYear(stockId, lastYear);

        // 4. 메트릭 계산 (기존 Calculator 재사용)
        MetricValues metricValues = StockMetricCalculator.calculate(
                stock, latestDailyPrice, currentYearMap, lastYearMap, currentQuarter);

        if (metricValues == null) {
            log.debug("[{}] 스킵 - 계산 결과 null", stock.getStockCode());
            return null;
        }

        // 5. 기존 StockMetric 있으면 업데이트, 없으면 새로 생성
        StockMetric metric = bulkData.getExistingMetricMap().get(stockId);
        if (metric == null) {
            metric = StockMetric.builder().stock(stock).build();
        }

        metric.updateAll(
                metricValues.currentPrice(),
                metricValues.marketCap(),
                metricValues.per(),
                metricValues.roe(),
                metricValues.netProfitMargin(),
                metricValues.debtRatio(),
                metricValues.operatingProfitMargin(),
                metricValues.salesGrowthQoQ(),
                metricValues.salesGrowthYoY(),
                metricValues.netProfitGrowthQoQ(),
                metricValues.netProfitGrowthYoY(),
                metricValues.dividendYield(),
                metricValues.foreignOwnershipRate(),
                metricValues.return3M(),
                metricValues.return6M(),
                metricValues.return12M()
        );

        return metric;
    }

    /**
     * 처리 결과 DTO
     */
    public record ProcessResult(int success, int skipped, int failed, long totalTimeMs) {
        @Override
        public String toString() {
            return String.format("ProcessResult{success=%d, skipped=%d, failed=%d, totalTime=%dms}",
                    success, skipped, failed, totalTimeMs);
        }
    }
}
