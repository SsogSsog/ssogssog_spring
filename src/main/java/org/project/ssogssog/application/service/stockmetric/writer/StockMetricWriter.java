package org.project.ssogssog.application.service.stockmetric.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.project.ssogssog.domain.stock.repository.DailyPriceRepository;
import org.project.ssogssog.domain.stock.repository.StockFinancialRepository;
import org.project.ssogssog.domain.stockmetric.entity.StockMetric;
import org.project.ssogssog.domain.stockmetric.factory.StockMetricCalculator;
import org.project.ssogssog.domain.stockmetric.repository.StockMetricRepository;
import org.project.ssogssog.domain.stockmetric.vo.MetricValues;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockMetricWriter {

    private final StockMetricRepository stockMetricRepository;

    private final StockFinancialRepository stockFinancialRepository;
    private final DailyPriceRepository dailyPriceRepository;

    /**
     * 특정 종목 하나의 지표를 계산해서 저장
     */
    @Transactional
    public void refreshMetricForStock(Stock stock) {

        DailyPrice latestDailyPrice = dailyPriceRepository
                .findTopByStockOrderByDateDesc(stock)
                .orElse(null);

        StockFinancial latestStockFinancial = stockFinancialRepository
                .findTopByStockOrderByYearDescQuarterDesc(stock)
                .orElse(null);

        if (latestDailyPrice == null) {
            log.warn("StockMetric 계산 불가 - 일별시세 정보 부족. stockId={}, code={}",
                    stock.getId(), stock.getStockCode());
            return;
        }

        if (latestStockFinancial == null) {
            log.warn("StockMetric 계산 불가 - 재무제표 정보 부족. stockId={}, code={}",
                    stock.getId(), stock.getStockCode());
            return;
        }

        final int currentYear = latestStockFinancial.getYear();
        final String currentQuarter = latestStockFinancial.getQuarter();
        final boolean isConsolidated = latestStockFinancial.isConsolidated();
        final int lastYear = currentYear - 1;

        // 올해 분기별 재무 데이터 조회 → Map<"1Q", StockFinancial> 형태
        Map<String, StockFinancial> currentYearMap = toQuarterMap(
                stockFinancialRepository.findByStockAndYearAndIsConsolidatedOrderByQuarterAsc(
                        stock, currentYear, isConsolidated)
        );

        // 작년 분기별 재무 데이터 조회 → Map<"1Q"~"4Q", StockFinancial> 형태
        Map<String, StockFinancial> lastYearMap = toQuarterMap(
                stockFinancialRepository.findByStockAndYearAndIsConsolidatedOrderByQuarterAsc(
                        stock, lastYear, isConsolidated)
        );

        log.debug("[{}] 재무 데이터 조회 완료 - 올해({}): {}, 작년({}): {}",
                stock.getStockCode(), currentYear, currentYearMap.keySet(),
                lastYear, lastYearMap.keySet());

        // TTM 기반 메트릭 계산
        MetricValues metricValues = StockMetricCalculator.calculate(
                stock, latestDailyPrice, currentYearMap, lastYearMap, currentQuarter);


        StockMetric metric = stockMetricRepository
                .findByStock(stock)
                .orElseGet(() -> StockMetric.builder().stock(stock).build());

        metric.updateAll(
                metricValues.currentPrice(),
                metricValues.marketCap(),
                metricValues.per(),
                metricValues.pbr(),
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

        stockMetricRepository.save(metric);
    }

    /**
     * List<StockFinancial> → Map<Quarter, StockFinancial> 변환
     */
    private Map<String, StockFinancial> toQuarterMap(List<StockFinancial> financials) {
        return financials.stream()
                .collect(Collectors.toMap(
                        StockFinancial::getQuarter,
                        sf -> sf,
                        (existing, replacement) -> existing  // 중복 시 기존 값 유지
                ));
    }
}
