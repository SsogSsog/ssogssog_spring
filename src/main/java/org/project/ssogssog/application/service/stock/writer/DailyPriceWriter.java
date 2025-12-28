package org.project.ssogssog.application.service.stock.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.utils.DateUtils;
import org.project.ssogssog.application.service.stock.usecase.dto.HistoricalPriceResponse;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.DailyPriceRepository;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.*;

import static org.project.ssogssog.application.utils.NormalizeUtils.normalizeNumber;
import static org.project.ssogssog.application.utils.ParserUtils.parseIntOrNull;
import static org.project.ssogssog.application.utils.ParserUtils.parseLongOrNull;

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
    public void saveHistoricalPrices(String stockCode, List<HistoricalPriceResponse.DailyItem> items) {

        // 해당 주식 정보 가져오기
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new RuntimeException("종목 없음"));

        List<DailyPrice> priceList = new ArrayList<>();

        // 해당 주식이 갖고 았는 모든 DailyPrice의 날짜 가져오기(중복 제거)
        Set<LocalDate> existDates = dailyPriceRepository.findAllDatesByStock(stock);

        // 만약 복합키에 대해서 Set을 사용해야 할 일이 있다면?..
        // 1. record 사용
        // record는 내부적으로 equals()와 hashCode()를 자동으로 완벽하게 구현해주기에 바로 Set<class> 사용 가능

        // 2. @EqualsAndHashCode 사용
        // equals()와 hashCode() 자동 생성

        // 3. Hacky 문자열 합치기
        // 필드가 복잡하지 않고 단순한 타입(숫자, 날짜)들 뿐이라면, 굳이 클래스를 안 만들고 String Key를 만들기!
        // (ex 20251219_119291)

        for (HistoricalPriceResponse.DailyItem item : items) {
            // 데이터가 비어있는 휴장일 등은 패스
            if (item.getClosePrice() == null || item.getClosePrice().isEmpty()) continue;

            LocalDate date;
            try{
                date = LocalDate.parse(item.getDate(), DateUtils.dateTimeFormatter);
            }catch(DateTimeException e){
                log.error("[Date Error] 종목: {}, 잘못된 날짜 포맷: {}", stockCode, item.getDate());
                continue;
            }

            // 이미 DB에 있는 날짜면 건너뛰기 (중복 저장 방지)
            if (existDates.contains(date)) {
                continue;
            }

            Integer close = parseIntOrNull(normalizeNumber(item.getClosePrice()));
            Integer open  = parseIntOrNull(normalizeNumber(item.getOpenPrice()));
            Integer high  = parseIntOrNull(normalizeNumber(item.getHighPrice()));
            Integer low   = parseIntOrNull(normalizeNumber(item.getLowPrice()));
            Long volume   = parseLongOrNull(normalizeNumber(item.getVolume()));

            // 필수 필드 하나라도 파싱 실패면 스킵
            if (close == null || open == null || high == null || low == null || volume == null) {
                log.warn("[{}] 파싱 실패로 스킵: date={}, close={}, open={}, high={}, low={}, vol={}",
                        stockCode, item.getDate(),
                        item.getClosePrice(), item.getOpenPrice(), item.getHighPrice(), item.getLowPrice(), item.getVolume());
                continue;
            }

            DailyPrice dailyPrice = DailyPrice.builder()
                    .stock(stock)
                    .date(date)
                    .closePrice(close)
                    .openPrice(open)
                    .highPrice(high)
                    .lowPrice(low)
                    .volume(volume)
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
