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

}

