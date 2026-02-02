package org.project.ssogssog.infrastructure.adapter.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.port.DailyPricePort;
import org.project.ssogssog.application.service.stock.collect.dto.HistoricalPriceDTO;
import org.project.ssogssog.infrastructure.client.common.exception.RateLimitExceededException;
import org.project.ssogssog.infrastructure.client.common.exception.TokenExpiredException;
import org.project.ssogssog.infrastructure.client.feign.kis.KisFeignClient;
import org.project.ssogssog.infrastructure.client.feign.kis.KisTokenManager;
import org.project.ssogssog.infrastructure.client.feign.kis.dto.KisHistoricalPriceResponse;
import org.project.ssogssog.infrastructure.client.feign.kis.dto.KisHolidayResponse;
import org.project.ssogssog.infrastructure.client.feign.kis.dto.KisPriceResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 일별 시세 어댑터
 * - KIS Feign Client를 사용하여 외부 API 호출
 * - Resilience4j 어노테이션으로 회복 탄력성 적용
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DailyPriceAdapter implements DailyPricePort {

    private final KisFeignClient kisFeignClient;
    private final KisTokenManager kisTokenManager;
    private final ObjectMapper objectMapper;

    /**
     * 오늘의 일별시세 정보 제공
     */
    @Override
    @Retry(name = "kis-token-retry", fallbackMethod = "getPriceRootFallback")
    @CircuitBreaker(name = "kis-circuit", fallbackMethod = "getPriceRootFallback")
    @RateLimiter(name = "kis-rate-limiter")
    public JsonNode getPriceRoot(String stockCode) {
        try {
            KisPriceResponse response = kisFeignClient.getCurrentPrice(
                    "FHKST01010100",  // tr_id: 주식현재가 시세
                    "J",              // 시장구분: 주식
                    stockCode
            );

            // KIS API는 200 OK여도 rt_cd로 에러를 알림
            if (!response.isSuccess()) {
                if (response.isRateLimitError()) {
                    throw new RateLimitExceededException("KIS", 1L, response.getMsg1());
                }
                log.warn("KIS API 응답 오류 - 종목: {}, 코드: {}, 메시지: {}",
                        stockCode, response.getMsgCd(), response.getMsg1());
                return null;
            }

            return objectMapper.valueToTree(response);

        } catch (TokenExpiredException e) {
            // 토큰 만료 시 무효화 후 재시도 (Retry가 처리)
            kisTokenManager.invalidateToken();
            throw e;
        }
    }

    /**
     * Circuit Open 또는 최종 실패 시 fallback
     */
    public JsonNode getPriceRootFallback(String stockCode, Exception e) {
        log.warn("KIS API 호출 실패 (fallback) - 종목: {}, 원인: {}", stockCode, e.getMessage());
        return null;
    }

    /**
     * 기간별 과거 시세 조회
     */
    @Override
    @Retry(name = "kis-token-retry", fallbackMethod = "fetchPastPricesFallback")
    @CircuitBreaker(name = "kis-circuit", fallbackMethod = "fetchPastPricesFallback")
    @RateLimiter(name = "kis-rate-limiter")
    public HistoricalPriceDTO fetchPastPrices(String stockCode, String strStartDate, String strEndDate) {
        try {
            KisHistoricalPriceResponse response = kisFeignClient.getHistoricalPrices(
                    "FHKST03010100",  // tr_id: 기간별 시세
                    "J",              // 시장구분: 주식
                    stockCode,
                    strStartDate,
                    strEndDate,
                    "D",              // 기간구분: 일봉
                    "1"               // 수정주가 반영
            );

            if (!response.isSuccess()) {
                if (response.isRateLimitError()) {
                    throw new RateLimitExceededException("KIS", 1L, response.getMsg1());
                }
                log.warn("KIS 기간별 시세 조회 오류 - 종목: {}, 메시지: {}", stockCode, response.getMsg1());
                return null;
            }

            return convertToHistoricalPriceDTO(response);

        } catch (TokenExpiredException e) {
            kisTokenManager.invalidateToken();
            throw e;
        }
    }

    /**
     * Circuit Open 또는 최종 실패 시 fallback
     */
    public HistoricalPriceDTO fetchPastPricesFallback(String stockCode, String strStartDate, String strEndDate, Exception e) {
        log.warn("KIS 기간별 시세 조회 실패 (fallback) - 종목: {}, 원인: {}", stockCode, e.getMessage());
        return null;
    }

    /**
     * 시장 개장 여부 확인
     */
    @Override
    @Retry(name = "kis-retry", fallbackMethod = "isMarketOpenFallback")
    @CircuitBreaker(name = "kis-circuit", fallbackMethod = "isMarketOpenFallback")
    @RateLimiter(name = "kis-rate-limiter")
    public boolean isMarketOpen(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        try{
            KisHolidayResponse response = kisFeignClient.checkHoliday(
                    "CTCA0903R",  // tr_id: 휴장일 조회
                    "",           // tr_cont
                    "P",          // custtype: 개인
                    dateStr,
                    "",           // CTX_AREA_NK
                    ""            // CTX_AREA_FK
            );

            if (response == null || response.getOutput() == null || response.getOutput().isEmpty()) {
                log.error("휴장일 정보를 가져오지 못했습니다. 안전하게 '휴장'으로 처리합니다.");
                return false;
            }

            // TODO 날짜 파싱 로직 검토하 + 통합 테스트 코드 만들기
            // 요청한 날짜의 개장 여부 확인
            return response.getOutput().stream()
                    .filter(info -> info.getBaseDate().equals(dateStr))
                    .findFirst()
                    .map(KisHolidayResponse.HolidayInfo::isMarketOpen)
                    .orElse(response.getOutput().get(0).isMarketOpen());
        }catch (TokenExpiredException e) {
            kisTokenManager.invalidateToken();
            throw e;
        }

    }

    public boolean isMarketOpenFallback(LocalDate date, Exception e) {
        log.warn("휴장일 확인 실패 (fallback) - 날짜: {}, 원인: {}. 보수적으로 '휴장'으로 처리", date, e.getMessage());
        return false;
    }

    /**
     * KIS 응답 → HistoricalPriceDTO 변환
     */
    private HistoricalPriceDTO convertToHistoricalPriceDTO(KisHistoricalPriceResponse response) {
        if (response == null || response.getOutput2() == null || response.getOutput2().isEmpty()) {
            return null;
        }

        HistoricalPriceDTO dto = new HistoricalPriceDTO();
        dto.setReturnCode(response.getRtCd());
        dto.setMessage(response.getMsg1());

        // Output1 변환
        if (response.getOutput1() != null) {
            HistoricalPriceDTO.Output1 output1 = new HistoricalPriceDTO.Output1();
            output1.setPriceChange(response.getOutput1().getPriceChange());
            dto.setOutput1(output1);
        }

        // Output2 (일별 데이터) 변환
        List<HistoricalPriceDTO.DailyItem> items = new ArrayList<>();
        for (KisHistoricalPriceResponse.DailyPrice daily : response.getOutput2()) {
            HistoricalPriceDTO.DailyItem item = new HistoricalPriceDTO.DailyItem();
            item.setDate(daily.getDate());
            item.setClosePrice(daily.getClosePrice());
            item.setOpenPrice(daily.getOpenPrice());
            item.setHighPrice(daily.getHighPrice());
            item.setLowPrice(daily.getLowPrice());
            item.setVolume(daily.getVolume());
            items.add(item);
        }
        dto.setDailyItems(items);

        return dto;
    }
}
