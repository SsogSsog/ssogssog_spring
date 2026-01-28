package org.project.ssogssog.unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ssogssog.application.service.stock.port.DailyPricePort;
import org.project.ssogssog.application.service.stock.collect.CollectTodayPricesUseCase;
import org.project.ssogssog.application.service.stock.writer.DailyPriceWriter;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.StockRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectTodayPricesUseCaseTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private DailyPriceWriter dailyPriceWriter;

    @Mock
    private DailyPricePort dailyPricePort; // KISClient 대신 Port 사용

    @InjectMocks
    private CollectTodayPricesUseCase collectTodayPricesUseCase;

    private final ObjectMapper om = new ObjectMapper();

    @Test
    @DisplayName("장 열린 날: 정상적으로 시세를 가져와 저장한다")
    void updateAllStockPrices_success() throws Exception {
        // given
        // 휴장일 아님 설정
        when(dailyPricePort.isMarketOpen(any())).thenReturn(true);

        Stock stock = Stock.builder()
                .corpName("삼성전자")
                .stockCode("005930")
                .build();
        when(stockRepository.findAll()).thenReturn(List.of(stock));

        // API 응답 Mocking (파싱 로직에 필요한 필드 포함)
        String json = """
                {
                  "rt_cd": "0",
                  "msg_cd": "0",
                  "output": {
                    "stck_bsop_date": "20240118",
                    "stck_prpr": "1000",
                    "stck_oprc": "900",
                    "stck_hgpr": "1100",
                    "stck_lwpr": "800",
                    "acml_vol": "12345",
                    "hts_avls": "9999",
                    "lstn_stcn": "1000000",
                    "frgn_hldn_qty": "200000",
                    "prdy_vrss": "10",
                    "prdy_ctrt": "1.00",
                    "stck_sdpr": "990",
                    "w52_hgpr": "1500",
                    "w52_lwpr": "700",
                    "pbr": "1.5"
                  }
                }
                """;
        JsonNode root = om.readTree(json);
        when(dailyPricePort.getPriceRoot("005930")).thenReturn(root);

        // when
        collectTodayPricesUseCase.updateAllStockPrices();

        // then
        // 저장 로직(saveDailyPrice)이 1번 호출되었는지 검증
        verify(dailyPriceWriter, times(1)).saveDailyPrice(any());
    }

    @Test
    @DisplayName("휴장일인 경우: 아무 작업도 하지 않고 종료한다")
    void updateAllStockPrices_marketClosed() {
        // given
        // 휴장일 설정
        when(dailyPricePort.isMarketOpen(any())).thenReturn(false);

        // when
        collectTodayPricesUseCase.updateAllStockPrices();

        // then
        // 종목 조회나 저장이 호출되지 않아야 함
        verify(stockRepository, never()).findAll();
        verify(dailyPriceWriter, never()).saveDailyPrice(any());
    }

    @Test
    @DisplayName("API 응답이 비어있거나 에러인 경우: 저장을 시도하지 않는다")
    void updateAllStockPrices_apiError() {
        // given
        when(dailyPricePort.isMarketOpen(any())).thenReturn(true);

        Stock stock = Stock.builder().corpName("에러종목").stockCode("999999").build();
        when(stockRepository.findAll()).thenReturn(List.of(stock));

        // API가 null을 리턴한다고 가정
        when(dailyPricePort.getPriceRoot("999999")).thenReturn(null);

        // when
        collectTodayPricesUseCase.updateAllStockPrices();

        // then
        verify(dailyPriceWriter, never()).saveDailyPrice(any());
    }
}