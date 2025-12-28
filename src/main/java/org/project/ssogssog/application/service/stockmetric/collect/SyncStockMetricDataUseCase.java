package org.project.ssogssog.application.service.stockmetric.collect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stockmetric.writer.StockMetricWriter;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class SyncStockMetricDataUseCase {

    private final StockRepository stockRepository;

    private final StockMetricWriter stockMetricWriter;


    /**
     * 전체 종목의 StockMetric 계산 (종가 이후 하루에 한 번 실행)
     */
    public void refreshAllMetrics(){

        List<Stock> stocks = stockRepository.findAll();
        for (Stock stock : stocks) {
            stockMetricWriter.refreshMetricForStock(stock); // 주의) 현재 같은 클래스이므로 @Transactional이 무시되는 상황이므로 추후 원자적인 처리로 단위 고려하기
        }

    }

}

