package org.project.ssogssog.application.service.stockmetric.reader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.project.ssogssog.domain.stock.repository.DailyPriceRepository;
import org.project.ssogssog.domain.stock.repository.StockFinancialRepository;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.domain.stockmetric.entity.StockMetric;
import org.project.ssogssog.domain.stockmetric.repository.StockMetricRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * StockMetric 계산을 위한 데이터 Bulk 조회 담당
 * - N+1 문제 해결: 개별 쿼리 대신 한 번에 모든 데이터 조회
 * - 메모리 매핑: 조회한 데이터를 Map으로 변환하여 O(1) 접근
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockMetricBulkDataReader {

    private final StockRepository stockRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final StockFinancialRepository stockFinancialRepository;
    private final StockMetricRepository stockMetricRepository;

    /**
     * 모든 StockMetric 계산에 필요한 데이터를 한 번에 조회
     *
     * @return BulkData - 메모리 매핑된 데이터 객체
     */
    public BulkData fetchAll() {
        // 1. 모든 Stock 조회
        List<Stock> stocks = stockRepository.findAll();
        log.info("[Bulk] Stock 조회 완료: {}개", stocks.size());

        // 2. 모든 종목의 최신 DailyPrice 조회
        List<DailyPrice> latestDailyPrices = dailyPriceRepository.findAllLatestByStock();
        log.info("[Bulk] 최신 DailyPrice 조회 완료: {}개", latestDailyPrices.size());

        // 3. 모든 종목의 최신 StockFinancial 조회 (최신 분기 정보 파악용)
        List<StockFinancial> latestFinancials = stockFinancialRepository.findAllLatestByStock();
        log.info("[Bulk] 최신 StockFinancial 조회 완료: {}개", latestFinancials.size());

        // 4. 최신 분기 정보에서 필요한 연도 추출 (올해 + 작년)
        Set<Integer> requiredYears = extractRequiredYears(latestFinancials);
        log.info("[Bulk] 필요한 연도: {}", requiredYears);

        // 5. 필요한 연도의 모든 재무 데이터 한 번에 조회
        List<StockFinancial> allFinancials = stockFinancialRepository.findByYearIn(new ArrayList<>(requiredYears));
        log.info("[Bulk] 전체 StockFinancial 조회 완료: {}개", allFinancials.size());

        // 6. 기존 StockMetric 조회 (있으면 update, 없으면 insert)
        List<StockMetric> existingMetrics = stockMetricRepository.findAll();
        log.info("[Bulk] 기존 StockMetric 조회 완료: {}개", existingMetrics.size());

        // 7. 메모리 매핑
        BulkData bulkData = mapToMemory(stocks, latestDailyPrices, latestFinancials, allFinancials, existingMetrics);

        log.info("[Bulk] 전체 데이터 조회 완료");

        return bulkData;
    }

    /**
     * 최신 재무 데이터에서 필요한 연도 추출
     * (각 종목의 currentYear와 lastYear)
     */
    private Set<Integer> extractRequiredYears(List<StockFinancial> latestFinancials) {
        Set<Integer> years = new HashSet<>();
        for (StockFinancial sf : latestFinancials) {
            int currentYear = sf.getYear();
            years.add(currentYear);
            years.add(currentYear - 1);
        }
        return years;
    }

    /**
     * 조회한 데이터를 Map으로 변환
     */
    private BulkData mapToMemory(
            List<Stock> stocks,
            List<DailyPrice> latestDailyPrices,
            List<StockFinancial> latestFinancials,
            List<StockFinancial> allFinancials,
            List<StockMetric> existingMetrics
    ) {
        // Stock ID → Stock
        Map<Long, Stock> stockMap = stocks.stream()
                .collect(Collectors.toMap(Stock::getId, s -> s));

        // Stock ID → 최신 DailyPrice
        Map<Long, DailyPrice> latestDailyPriceMap = latestDailyPrices.stream()
                .collect(Collectors.toMap(
                        dp -> dp.getStock().getId(),
                        dp -> dp,
                        (existing, replacement) -> existing  // 중복 시 기존 유지
                ));

        // Stock ID → 최신 StockFinancial (분기 정보 파악용)
        Map<Long, StockFinancial> latestFinancialMap = latestFinancials.stream()
                .collect(Collectors.toMap(
                        sf -> sf.getStock().getId(),
                        sf -> sf,
                        (existing, replacement) -> existing
                ));

        // Stock ID → Year → Quarter → StockFinancial
        // 3중 중첩 Map으로 구성
        Map<Long, Map<Integer, Map<String, StockFinancial>>> financialMap = new HashMap<>();
        for (StockFinancial sf : allFinancials) {
            Long stockId = sf.getStock().getId();
            int year = sf.getYear();
            String quarter = sf.getQuarter();

            financialMap
                    .computeIfAbsent(stockId, k -> new HashMap<>())
                    .computeIfAbsent(year, k -> new HashMap<>())
                    .put(quarter, sf);
        }

        // Stock ID → 기존 StockMetric (없으면 null)
        Map<Long, StockMetric> existingMetricMap = existingMetrics.stream()
                .collect(Collectors.toMap(
                        sm -> sm.getStock().getId(),
                        sm -> sm,
                        (existing, replacement) -> existing
                ));

        return new BulkData(
                stocks,
                stockMap,
                latestDailyPriceMap,
                latestFinancialMap,
                financialMap,
                existingMetricMap
        );
    }

    /**
     * Bulk 조회 결과를 담는 DTO
     */
    @Getter
    @RequiredArgsConstructor
    public static class BulkData {
        private final List<Stock> stocks;
        private final Map<Long, Stock> stockMap;
        private final Map<Long, DailyPrice> latestDailyPriceMap;
        private final Map<Long, StockFinancial> latestFinancialMap;
        private final Map<Long, Map<Integer, Map<String, StockFinancial>>> financialMap;
        private final Map<Long, StockMetric> existingMetricMap;

        /**
         * 특정 종목의 특정 연도 분기별 재무 데이터 조회
         *
         * @param stockId 종목 ID
         * @param year    연도
         * @return Map<Quarter, StockFinancial> (없으면 빈 Map)
         */
        public Map<String, StockFinancial> getFinancialsByYear(Long stockId, int year) {
            return financialMap
                    .getOrDefault(stockId, Collections.emptyMap())
                    .getOrDefault(year, Collections.emptyMap());
        }
    }
}
