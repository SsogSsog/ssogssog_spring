package org.project.ssogssog.application.stockmetric.collect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.project.ssogssog.domain.stock.repository.DailyPriceRepository;
import org.project.ssogssog.domain.stock.repository.StockFinancialRepository;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.domain.stockmetric.entity.StockMetric;
import org.project.ssogssog.domain.stockmetric.factory.StockMetricCalculator;
import org.project.ssogssog.domain.stockmetric.repository.StockMetricRepository;
import org.project.ssogssog.domain.stockmetric.vo.MetricValues;
import org.project.ssogssog.domain.stockmetric.vo.YearQuarter;
import org.project.ssogssog.presentation.controller.stockmetric.dto.StockMetricRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class StockMetricRefreshService {

    private final StockRepository stockRepository;
    private final StockMetricRepository stockMetricRepository;

    private final StockFinancialRepository stockFinancialRepository;
    private final DailyPriceRepository dailyPriceRepository;


    /**
     * 전체 종목의 StockMetric 계산 (종가 이후 하루에 한 번 실행)
     */
    public void refreshAllMetrics(){

        List<Stock> stocks = stockRepository.findAll();
        for (Stock stock : stocks) {
            refreshMetricForStock(stock); // 주의) 현재 같은 클래스이므로 @Transactional이 무시되는 상황이므로 추후 원자적인 처리로 단위 고려하기
        }

    }


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

        if (latestDailyPrice == null || latestStockFinancial == null) {
            log.warn("❌ StockMetric 계산 불가 - 기초 데이터 부족. stockId={}, code={}",
                    stock.getId(), stock.getStockCode());
            return;
        }

        final int latestYear = latestStockFinancial.getYear();          // 최근 년도
        final String latestQuarter = latestStockFinancial.getQuarter(); // 최근 분기
        final YearQuarter prevQuarter = YearQuarter.prevQuarter(latestYear, latestQuarter);   // 직전 분기 YearQuarter
        final YearQuarter prevYear = YearQuarter.prevYear(latestYear, latestQuarter);         // 작년 YearQuarter

        StockFinancial prevQuarterStockFinancial = stockFinancialRepository
                .findByStockIdAndYearAndQuarter(stock.getId(), prevQuarter.year(), prevQuarter.quarter())
                .orElse(null);

        StockFinancial prevYearStockFinancial = stockFinancialRepository
                .findByStockIdAndYearAndQuarter(stock.getId(), prevYear.year(), prevYear.quarter())
                .orElse(null);

        // currentPrice, marketCap
        // per, roe, netProfitMargin, debtRatio,
        // salesGrowthQoQ, salesGrowthYoY, netProfitGrowthQoQ, netProfitGrowthYoY,
        // dividendYield, foreignOwnershipRate, return3M/6M/12M 계산
        MetricValues metricValues = StockMetricCalculator.calculate(
                stock, latestDailyPrice, latestStockFinancial,prevQuarterStockFinancial, prevYearStockFinancial);


        StockMetric metric = stockMetricRepository
                .findByStock(stock)
                .orElseGet(() -> StockMetric.builder().stock(stock).build());

        metric.updateAll(
                metricValues.currentPrice(),
                metricValues.marketCap(),
                metricValues.per(),
                metricValues.roe(),
                metricValues.netProfitMargin(),
                metricValues.debtRatio(),
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
}

