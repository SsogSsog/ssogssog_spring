package org.project.ssogssog.infrastructure.adapter.stock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.port.StockFinancialPort;
import org.project.ssogssog.infrastructure.client.common.exception.RetryableApiException;
import org.project.ssogssog.infrastructure.client.feign.opendart.OpenDartFeignClient;
import org.project.ssogssog.infrastructure.client.feign.opendart.validator.OpenDartValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

/**
 * 재무정보 어댑터
 * - OpenDART Feign Client를 사용하여 외부 API 호출
 * - Resilience4j 어노테이션으로 회복 탄력성 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockFinancialAdapter implements StockFinancialPort {

    private final ObjectMapper objectMapper;
    private final OpenDartFeignClient openDartFeignClient;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final Executor openDartApiExecutor;

    @Value("${opendart.api-key}")
    private String openDartApiKey;
    /**
     * 재무정보 조회
     * - TimeLimiter로 타임아웃 제한 (느린 응답 방지)
     * - CompletableFuture를 사용한 비동기 처리
     */
    @Override
    @Retry(name = "opendart-retry", fallbackMethod = "getFinancialInfoFallback")
    @CircuitBreaker(name = "opendart-circuit")
    @RateLimiter(name = "opendart-rate-limiter")
    public JsonNode getFinancialInfo(String corpCode, Integer year, String reportCode) {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("opendart-slow-api");

        try {
            // CompletableFuture로 API 호출을 감싸고 TimeLimiter 적용
            return timeLimiter.executeFutureSupplier(
                    () -> CompletableFuture.supplyAsync(() ->
                            fetchFinancialInfoInternal(corpCode, year, reportCode), openDartApiExecutor
                    )
            );
        } catch (TimeoutException e) {
            log.warn("[TimeLimiter 타임아웃] OpenDART 재무정보 조회 시간 초과 - corpCode: {}, year: {}, reportCode: {}",
                    corpCode, year, reportCode);
            return null;
        } catch (Exception e) {
            // 다른 예외는 상위로 전파 (Retry/CircuitBreaker가 처리)
            throw new RetryableApiException("네트워크 에러", 500, e.getMessage());
        }
    }

    /**
     * 실제 API 호출 로직 (내부 메서드)
     */
    private JsonNode fetchFinancialInfoInternal(String corpCode, Integer year, String reportCode) {
        try {
            String response = openDartFeignClient.getFinancialInfo(
                    openDartApiKey,
                    corpCode,
                    year,
                    reportCode
            );

            JsonNode root = objectMapper.readTree(response);

            // [★ 적용] 공통 검증기 호출 (여기서 020, 800 등은 예외가 터져서 재시도됨)
            OpenDartValidator.validate(root);

            // 검증 통과 후, "013(데이터 없음)"인 경우는 null 리턴
            if ("013".equals(root.path("status").asText())) {
                log.info("OpenDART 데이터 없음 (013) - corpCode: {}", corpCode);
                return null;
            }
            return root;

        } catch (JsonProcessingException e) {
            log.warn("OpenDART JSON 파싱 실패 corpCode={}, year={}, reportCode={}",
                    corpCode, year, reportCode, e);
            return null;
        }
    }

    public JsonNode getFinancialInfoFallback(String corpCode, Integer year, String reportCode, Exception e) {
        if (e instanceof CallNotPermittedException) {
            log.warn("[Circuit 차단] 서킷이 열려있어 요청이 거부됨");
        }else{
            log.warn("OpenDART 재무정보 조회 실패 (fallback) - corpCode: {}, year: {}, reportCode: {}, 원인: {}",
                    corpCode, year, reportCode, e.getMessage());
        }
        return null;
    }
}
