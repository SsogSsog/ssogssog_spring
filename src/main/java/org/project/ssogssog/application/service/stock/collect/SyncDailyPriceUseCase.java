package org.project.ssogssog.application.service.stock.collect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.writer.DailyPriceWriter;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.DailyPriceRepository;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 일별 시세 등락 정보(changeRate, changePrice) 동기화 UseCase
 *
 * 대량 수집된 DailyPrice 중 등락 정보가 비어있는 데이터를
 * 전일 종가를 기반으로 계산하여 채워주는 역할
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncDailyPriceUseCase {

    private final StockRepository stockRepository;
    private final DailyPriceWriter dailyPriceWriter;

    /**
     * 전 종목의 등락 정보 동기화
     */
    public void syncDailyPriceChangeAll() {
        List<Stock> stocks = stockRepository.findAll();
        log.info("등락 정보 동기화 시작 - 총 {}개 종목", stocks.size());

        int totalUpdated = 0;
        int processedStocks = 0;

        for (Stock stock : stocks) {
            int updated = 0;

            try {
                updated = dailyPriceWriter.syncDailyPriceChangeInfo(stock);
                totalUpdated += updated;
            } catch (Exception e) {
                log.error("등락 정보 동기화 실패 - 종목: {}({}), 에러: {}",
                        stock.getCorpName(), stock.getStockCode(), e.getMessage());
            }
            processedStocks++;

            if (processedStocks % 100 == 0) {
                log.info("진행 중... {}/{} 종목 처리 완료", processedStocks, stocks.size());
            }
        }

        log.info("등락 정보 동기화 완료 - 총 {}개 종목, {}개 데이터 업데이트", stocks.size(), totalUpdated);
    }

}
