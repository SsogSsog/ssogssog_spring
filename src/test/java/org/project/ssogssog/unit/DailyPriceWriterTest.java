package org.project.ssogssog.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ssogssog.application.service.stock.collect.dto.KisHistoricalPriceResponse;
import org.project.ssogssog.application.service.stock.writer.DailyPriceWriter;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.DailyPriceRepository;
import org.project.ssogssog.domain.stock.repository.StockRepository;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyPriceWriterTest {

    @Mock private DailyPriceRepository dailyPriceRepository;
    @Mock private StockRepository stockRepository;

    @InjectMocks private DailyPriceWriter dailyPriceWriter;

    private Stock stock;

    @BeforeEach
    void setUp() {
        stock = mock(Stock.class);
        when(stock.getCorpName()).thenReturn("테스트회사");
    }

    @Test
    void saveHistoricalPrices_skipsExistingDates_andSavesOnlyNewOnes() {
        String stockCode = "005930";
        when(stockRepository.findByStockCode(stockCode)).thenReturn(Optional.of(stock));

        // 기존 날짜 1개가 이미 DB에 있음
        LocalDate existDate = LocalDate.of(2025, 12, 20);
        when(dailyPriceRepository.findAllDatesByStock(stock)).thenReturn(Set.of(existDate));

        // items: 기존 날짜 1개 + 신규 날짜 1개
        KisHistoricalPriceResponse.DailyItem oldItem = new KisHistoricalPriceResponse.DailyItem();
        oldItem.setDate("20251220");
        oldItem.setClosePrice("1,000");
        oldItem.setOpenPrice("900");
        oldItem.setHighPrice("1100");
        oldItem.setLowPrice("800");
        oldItem.setVolume("10");

        KisHistoricalPriceResponse.DailyItem newItem = new KisHistoricalPriceResponse.DailyItem();
        newItem.setDate("20251221");
        newItem.setClosePrice("2,000");
        newItem.setOpenPrice("1900");
        newItem.setHighPrice("2100");
        newItem.setLowPrice("1800");
        newItem.setVolume("20");

        List<KisHistoricalPriceResponse.DailyItem> items = List.of(oldItem, newItem);

        // when
        dailyPriceWriter.saveHistoricalPrices(stockCode, items);

        // then: saveAll 호출되고, 신규 1건만 저장되어야 함
        ArgumentCaptor<List<DailyPrice>> captor = ArgumentCaptor.forClass(List.class);
        verify(dailyPriceRepository, times(1)).saveAll(captor.capture());

        List<DailyPrice> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertEquals(LocalDate.of(2025, 12, 21), saved.get(0).getDate());
        assertEquals(2000, saved.get(0).getClosePrice());
        assertEquals(1900, saved.get(0).getOpenPrice());
    }

    @Test
    void saveHistoricalPrices_skipsIfParsingFails() {
        String stockCode = "005930";
        when(stockRepository.findByStockCode(stockCode)).thenReturn(Optional.of(stock));
        when(dailyPriceRepository.findAllDatesByStock(stock)).thenReturn(Collections.emptySet());

        // closePrice가 숫자가 아니라 파싱 실패 → 스킵되어 saveAll이 호출되지 않아야 함
        KisHistoricalPriceResponse.DailyItem badItem = new KisHistoricalPriceResponse.DailyItem();
        badItem.setDate("20251221");
        badItem.setClosePrice("ABC"); // 파싱 실패
        badItem.setOpenPrice("1900");
        badItem.setHighPrice("2100");
        badItem.setLowPrice("1800");
        badItem.setVolume("20");

        dailyPriceWriter.saveHistoricalPrices(stockCode, List.of(badItem));

        verify(dailyPriceRepository, never()).saveAll(any());
    }
}

