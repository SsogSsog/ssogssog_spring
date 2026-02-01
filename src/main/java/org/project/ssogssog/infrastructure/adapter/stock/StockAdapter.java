package org.project.ssogssog.infrastructure.adapter.stock;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.port.StockPort;
import org.project.ssogssog.infrastructure.client.common.exception.RetryableApiException;
import org.project.ssogssog.infrastructure.client.common.exception.TokenExpiredException;
import org.project.ssogssog.infrastructure.client.feign.kis.KisFeignClient;
import org.project.ssogssog.infrastructure.client.feign.kis.KisTokenManager;
import org.project.ssogssog.infrastructure.client.feign.kis.dto.KisPriceResponse;
import org.project.ssogssog.infrastructure.client.feign.opendart.OpenDartFeignClient;
import org.project.ssogssog.infrastructure.client.feign.opendart.dto.OpenDartDividendResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

/**
 * 주식 기본 정보 어댑터
 * - KIS + OpenDART Feign Client 사용
 * - Resilience4j 어노테이션으로 회복 탄력성 적용
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StockAdapter implements StockPort {

    private final KisFeignClient kisFeignClient;
    private final KisTokenManager kisTokenManager;
    private final OpenDartFeignClient openDartFeignClient;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final Executor openDartApiExecutor;

    @Value("${opendart.api-key}")
    private String openDartApiKey;

    /**
     * 업종(섹터) 정보 조회
     */
    @Override
    @Retry(name = "kis-retry", fallbackMethod = "fetchSectorFallback")
    @CircuitBreaker(name = "kis-circuit")
    @RateLimiter(name = "kis-rate-limiter")
    public String fetchSector(String stockCode) {
        try {
            KisPriceResponse response = kisFeignClient.getCurrentPrice(
                    "FHKST01010100",  // tr_id: 주식현재가 시세
                    "J",              // 시장구분: 주식
                    stockCode
            );

            if (response != null && response.isSuccess() && response.getOutput() != null) {
                return response.getOutput().getSectorName();
            }

            log.warn("KIS 섹터 조회 실패 - 종목: {}", stockCode);
            return null;

        } catch (TokenExpiredException e) {
            kisTokenManager.invalidateToken();
            throw e;
        }
    }

    public String fetchSectorFallback(String stockCode, Exception e) {
        // 1. 서킷 브레이커가 열려서 실패한 경우
        if (e instanceof CallNotPermittedException) {
            log.warn("[Circuit 차단] 서킷이 열려있어 요청이 거부됨");
        }else{
            log.warn("KIS 섹터 조회 실패 (fallback) - 종목: {}, 원인: {}", stockCode, e.getMessage());
        }
        return null;
    }

    /**
     * 기업코드 ZIP 파일 다운로드
     */
    @Override
    @Retry(name = "opendart-retry", fallbackMethod = "getCorpCodeZipFallback")
    @CircuitBreaker(name = "opendart-circuit")
    @RateLimiter(name = "opendart-rate-limiter")
    public byte[] getCorpCodeZip() {
        return openDartFeignClient.getCorpCodeZip(openDartApiKey);
    }

    public byte[] getCorpCodeZipFallback(Exception e) {
        if (e instanceof CallNotPermittedException) {
            log.warn("[Circuit 차단] 서킷이 열려있어 요청이 거부됨");
        }else{
            log.warn("OpenDART 기업코드 조회 실패 (fallback) - 원인: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 배당금(DPS) 조회
     * - TimeLimiter로 타임아웃 제한 (느린 응답 방지)
     * - CompletableFuture를 사용한 비동기 처리
     */
    @Override
    @Retry(name = "opendart-retry", fallbackMethod = "fetchLastDpsFallback")
    @CircuitBreaker(name = "opendart-circuit")
    @RateLimiter(name = "opendart-rate-limiter")
    public Integer fetchLastDps(String corpCode, String year) {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("opendart-slow-api");

        try {
            // CompletableFuture로 API 호출을 감싸고 TimeLimiter 적용
            return timeLimiter.executeFutureSupplier(
                    () -> CompletableFuture.supplyAsync(() ->
                            fetchLastDpsInternal(corpCode, year), openDartApiExecutor
                    )
            );
        } catch (TimeoutException e) {
            log.warn("[TimeLimiter 타임아웃] OpenDART 배당금 조회 시간 초과 - corpCode: {}, year: {}",
                    corpCode, year);
            return null;
        } catch (Exception e) {
            // 다른 예외는 상위로 전파 (Retry/CircuitBreaker가 처리)
            throw new RetryableApiException("네트워크 에러", 500, e.getMessage());
        }
    }

    /**
     * 실제 배당금 조회 로직 (내부 메서드)
     */
    private Integer fetchLastDpsInternal(String corpCode, String year) {
        OpenDartDividendResponse response = openDartFeignClient.getDividendInfo(
                openDartApiKey,
                corpCode,
                year,
                "11011"  // 사업보고서 (1년치 합산)
        );

        if (response == null || !response.isSuccess() || response.getList() == null) {
            log.warn("배당 정보 조회 실패 or 데이터 없음 (Code: {}, Year: {})", corpCode, year);
            return null;
        }

        // "주당 현금배당금(원)" + "보통주" 항목 찾기
        Optional<OpenDartDividendResponse.DividendItem> targetItem = response.getList().stream()
                .filter(item -> "주당 현금배당금(원)".equals(item.getSe()))
                .filter(item -> "보통주".equals(item.getStockKind()))
                .findFirst();

        if (targetItem.isEmpty()) {
            return null;  // 배당금 항목 없음
        }

        return parseDps(targetItem.get().getThisTerm());
    }

    public Integer fetchLastDpsFallback(String corpCode, String year, Exception e) {
        if (e instanceof CallNotPermittedException) {
            log.warn("[Circuit 차단] 서킷이 열려있어 요청이 거부됨");
        }else{
            log.warn("OpenDART 배당금 조회 실패 (fallback) - corpCode: {}, year: {}, 원인: {}",
                    corpCode, year, e.getMessage());
        }

        return null;
    }

    /**
     * 배당금 문자열 파싱
     */
    private Integer parseDps(String value) {
        if (value == null || value.trim().isEmpty() || "-".equals(value.trim())) {
            return 0;
        }
        try {
            return Integer.parseInt(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
