package org.project.ssogssog.application.service.stock.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.collect.dto.KisHistoricalPriceResponse;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.DailyPriceRepository;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class DailyPriceWriter {

    private final DailyPriceRepository dailyPriceRepository;
    private final StockRepository stockRepository;

    @Transactional
    // --- 저장 (중복 방지) ---
    public void saveDailyPrice(DailyPrice newPrice) {
        Optional<DailyPrice> existing = dailyPriceRepository.findByStockIdAndDate(
                newPrice.getStock().getId(), newPrice.getDate());

        if (existing.isEmpty()) {
            dailyPriceRepository.save(newPrice);
        } else {
            // 이미 있으면 업데이트 로직 (필요시 구현)
        }
    }

    @Transactional
    public void saveHistoricalPrices(String stockCode, List<KisHistoricalPriceResponse.DailyItem> items) {

        // 해당 주식 정보 가져오기
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new RuntimeException("종목 없음"));

        List<DailyPrice> priceList = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        // 해당 주식이 갖고 았는 모든 DailyPrice의 날짜 가져오기(중복 제거)
        Set<LocalDate> existDates;
        List<DailyPrice> dailyPrices = dailyPriceRepository.findDailyPricesByStock(stock);

        existDates = dailyPrices.stream()
                .map(DailyPrice::getDate)
                .collect(Collectors.toSet());

        for (KisHistoricalPriceResponse.DailyItem item : items) {
            // 데이터가 비어있는 휴장일 등은 패스
            if (item.getClosePrice() == null || item.getClosePrice().isEmpty()) continue;
            LocalDate date = LocalDate.parse(item.getDate(), dateFormatter);

            // 이미 DB에 있는 날짜면 건너뛰기 (중복 저장 방지)
            if (existDates.contains(date)) {
                continue;
            }

            DailyPrice dailyPrice = DailyPrice.builder()
                    .stock(stock)
                    .date(date)
                    .closePrice(Integer.parseInt(item.getClosePrice()))
                    .openPrice(Integer.parseInt(item.getOpenPrice()))
                    .highPrice(Integer.parseInt(item.getHighPrice()))
                    .lowPrice(Integer.parseInt(item.getLowPrice()))
                    .volume(Long.parseLong(item.getVolume()))
                    // 시가총액은 이 API에서 안 주므로 null 혹은 별도 계산
                    .build();

            priceList.add(dailyPrice);
        }

        if (!priceList.isEmpty()) {
            dailyPriceRepository.saveAll(priceList);
            log.info("[{}] 과거 데이터 {}건 저장 완료!", stock.getCorpName(), priceList.size());
        } else {
            log.info("[{}] 저장할 새로운 데이터가 없습니다.", stock.getCorpName());
        }

    }
}
