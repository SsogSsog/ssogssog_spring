package org.project.ssogssog.application.service.stockmetric.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stockmetric.writer.StockMetricWriter;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class SyncStockMetricDataUseCase {

    private final StockRepository stockRepository;

    private final StockMetricWriter stockMetricWriter;


    /**
     * 전체 종목의 StockMetric 계산 (일별시세 수집 후 이어서 실행)
     */
    public void refreshAllMetrics(){

        List<Stock> stocks = stockRepository.findAll();
        for (Stock stock : stocks) {
            stockMetricWriter.refreshMetricForStock(stock);
        }

    }

}

