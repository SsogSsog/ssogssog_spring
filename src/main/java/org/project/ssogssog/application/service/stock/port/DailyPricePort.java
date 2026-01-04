package org.project.ssogssog.application.service.stock.port;

import com.fasterxml.jackson.databind.JsonNode;
import org.project.ssogssog.application.service.stock.usecase.dto.HistoricalPriceDTO;
/**
 * 일별시세 정보를 담당하는 interface
 */
public interface DailyPricePort {
    // 오늘의 일별시세 정보 가져오기(전체 정보)
    JsonNode getPriceRoot(String stockCode);

    // 시작날~과거날까지의 과거 일별시세 정보 가져오기(정보 제한적)
    HistoricalPriceDTO fetchPastPrices(String stockCode, String strStartDate, String strEndDate);
}
