package org.project.ssogssog.application.service.stock.collect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final DailyPriceRepository dailyPriceRepository;

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
                updated = syncDailyPriceChangeInfo(stock);
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

    /**
     * 특정 종목의 일별시세 등락 정보 동기화
     * N+1 문제 방지: 해당 종목의 모든 DailyPrice를 한번에 조회
     */
    @Transactional
    public int syncDailyPriceChangeInfo(Stock stock) {
        // 한번에 모든 DailyPrice 조회 (날짜 오름차순)
        List<DailyPrice> dailyPrices = dailyPriceRepository.findByStockOrderByDateAsc(stock);

        if (dailyPrices.isEmpty()) {
            return 0;
        }

        List<DailyPrice> updatedPrices = calculateChangeInfo(dailyPrices);

        if (!updatedPrices.isEmpty()) {
            dailyPriceRepository.saveAll(updatedPrices);
            log.debug("종목 {} - {}개 데이터 등락 정보 업데이트", stock.getStockCode(), updatedPrices.size());
        }

        return updatedPrices.size();
    }

    /**
     * 연속된 DailyPrice 리스트에서 등락 정보가 없는 데이터를 계산
     * 계산 로직은 DailyPrice 엔티티에 위임
     *
     * @param dailyPrices 날짜순 정렬된 DailyPrice 리스트
     * @return 업데이트된 DailyPrice 리스트
     */
    private List<DailyPrice> calculateChangeInfo(List<DailyPrice> dailyPrices) {
        List<DailyPrice> updated = new ArrayList<>();
        DailyPrice prev = null;

        for (DailyPrice current : dailyPrices) {
            if (current.needsChangeInfoSync() && prev != null) {
                current.syncChangeInfo(prev.getClosePrice());
                updated.add(current);
            }
            prev = current;
        }

        return updated;
    }

}
