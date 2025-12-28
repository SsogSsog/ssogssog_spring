package org.project.ssogssog.unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ssogssog.application.service.stock.usecase.CollectTodayPricesUseCase;
import org.project.ssogssog.application.service.stock.writer.DailyPriceWriter;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.infrastructure.client.ksi.KSIClient;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectTodayPricesUseCaseTest {

    @Mock private StockRepository stockRepository;
    @Mock private DailyPriceWriter dailyPriceWriter;
    @Mock private KSIClient ksiClient;

    @InjectMocks private CollectTodayPricesUseCase collectTodayPricesUseCase;

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void updateAllStockPrices_success_callsWriterOncePerStock() throws Exception {
        // given
        Stock stock = mock(Stock.class);
        when(stock.getCorpName()).thenReturn("삼성전자");
        when(stock.getStockCode()).thenReturn("005930");
        when(stockRepository.findAll()).thenReturn(List.of(stock));

        when(ksiClient.getAccessToken()).thenReturn("token");

        String json =
                """
                {
                  "rt_cd": "0",
                  "msg_cd": "0",
                  "output": {
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
                    "stck_sdpr": "990"
                  }
                }
                """;
        JsonNode root = om.readTree(json);

        when(ksiClient.getPriceRoot(anyString(), eq("005930"))).thenReturn(root);

        // when
        collectTodayPricesUseCase.updateAllStockPrices();

        // then
        verify(dailyPriceWriter, times(1)).saveDailyPrice(any());
    }
}
