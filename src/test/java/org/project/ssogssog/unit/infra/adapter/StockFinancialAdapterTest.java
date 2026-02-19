package org.project.ssogssog.unit.infra.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ssogssog.infrastructure.adapter.stock.StockFinancialAdapter;
import org.project.ssogssog.infrastructure.client.common.exception.FatalApiException;
import org.project.ssogssog.infrastructure.client.common.exception.RetryableApiException;
import org.project.ssogssog.infrastructure.client.feign.opendart.OpenDartFeignClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockFinancialAdapter 단위 테스트")
class StockFinancialAdapterTest {

    @Mock
    private OpenDartFeignClient openDartFeignClient;
    @Mock
    private TimeLimiterRegistry timeLimiterRegistry;
    @Mock
    private TimeLimiter timeLimiter;
    @Mock
    private Executor executor;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private StockFinancialAdapter stockFinancialAdapter;

    @Nested
    @DisplayName("getFinancialInfo - TimeLimiter 테스트")
    class GetFinancialInfoTimeLimiterTest {

        @Test
        @DisplayName("TimeoutException 발생 시 null 반환")
        void whenTimeout_returnsNull() throws Exception {
            // given
            when(timeLimiterRegistry.timeLimiter("opendart-slow-api")).thenReturn(timeLimiter);
            when(timeLimiter.executeFutureSupplier(any())).thenThrow(new TimeoutException());

            // when
            JsonNode result = stockFinancialAdapter.getFinancialInfo("00126380", 2024, "11011");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("FatalApiException 발생 시 그대로 던짐")
        void whenFatalApiException_rethrows() throws Exception {
            // given
            FatalApiException fatalException = new FatalApiException("OPENDART", 401, "API 키 만료");

            when(timeLimiterRegistry.timeLimiter("opendart-slow-api")).thenReturn(timeLimiter);
            when(timeLimiter.executeFutureSupplier(any())).thenThrow(
                    new RuntimeException(fatalException) // CompletableFuture에서 감싸진 예외
            );

            // when & then
            assertThatThrownBy(() -> stockFinancialAdapter.getFinancialInfo("00126380", 2024, "11011"))
                    .isInstanceOf(FatalApiException.class)
                    .hasMessageContaining("API 키 만료");
        }

        @Test
        @DisplayName("일반 Exception 발생 시 RetryableApiException으로 변환")
        void whenGeneralException_throwsRetryable() throws Exception {
            // given
            when(timeLimiterRegistry.timeLimiter("opendart-slow-api")).thenReturn(timeLimiter);
            when(timeLimiter.executeFutureSupplier(any())).thenThrow(
                    new RuntimeException("네트워크 에러")
            );

            // when & then
            assertThatThrownBy(() -> stockFinancialAdapter.getFinancialInfo("00126380", 2024, "11011"))
                    .isInstanceOf(RetryableApiException.class);
        }
    }

}
