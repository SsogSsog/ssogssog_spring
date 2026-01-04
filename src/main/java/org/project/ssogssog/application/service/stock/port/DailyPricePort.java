package org.project.ssogssog.application.service.stock.port;

import com.fasterxml.jackson.databind.JsonNode;
import org.project.ssogssog.application.service.stock.usecase.dto.HistoricalPriceDTO;

public interface DailyPricePort {
    JsonNode getPriceRoot(String stockCode);

    String fetchSector(String stockCode);

    HistoricalPriceDTO fetchPastPrices(String stockCode, String strStartDate, String strEndDate);
}
