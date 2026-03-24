package org.project.ssogssog.unit.infra.adapter;

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
import org.project.ssogssog.infrastructure.adapter.stock.StockAdapter;
import org.project.ssogssog.infrastructure.client.common.exception.TokenExpiredException;
import org.project.ssogssog.infrastructure.client.feign.kis.KisFeignClient;
import org.project.ssogssog.infrastructure.client.feign.kis.KisTokenManager;
import org.project.ssogssog.infrastructure.client.feign.kis.dto.KisPriceResponse;
import org.project.ssogssog.infrastructure.client.feign.opendart.OpenDartFeignClient;
import org.project.ssogssog.infrastructure.client.feign.opendart.dto.OpenDartDividendResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockAdapter 단위 테스트")
class StockAdapterTest {

    @Mock
    private KisFeignClient kisFeignClient;
    @Mock
    private KisTokenManager kisTokenManager;
    @Mock
    private OpenDartFeignClient openDartFeignClient;
    @Mock
    private TimeLimiterRegistry timeLimiterRegistry;
    @Mock
    private TimeLimiter timeLimiter;

    @Mock
    private Executor executor;

    @InjectMocks
    private StockAdapter stockAdapter;

    @BeforeEach
    void setUp() {
        stockAdapter = new StockAdapter(
                kisFeignClient,
                kisTokenManager,
                openDartFeignClient,
                timeLimiterRegistry,
                executor
        );
        ReflectionTestUtils.setField(stockAdapter, "openDartApiKey", "test-api-key");
    }

    @Nested
    @DisplayName("fetchSector")
    class FetchSectorTest {

        @Test
        @DisplayName("response가 null이면 null 반환")
        void whenResponseNull_returnsNull() {
            // given
            when(kisFeignClient.getCurrentPrice(anyString(), anyString(), anyString()))
                    .thenReturn(null);

            // when
            String result = stockAdapter.fetchSector("005930");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("response.isSuccess()가 false이면 null 반환")
        void whenResponseNotSuccess_returnsNull() {
            // given
            KisPriceResponse response = mock(KisPriceResponse.class);
            when(response.isSuccess()).thenReturn(false);
            when(kisFeignClient.getCurrentPrice(anyString(), anyString(), anyString()))
                    .thenReturn(response);

            // when
            String result = stockAdapter.fetchSector("005930");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("response.getOutput()이 null이면 null 반환")
        void whenOutputNull_returnsNull() {
            // given
            KisPriceResponse response = mock(KisPriceResponse.class);
            when(response.isSuccess()).thenReturn(true);
            when(response.getOutput()).thenReturn(null);
            when(kisFeignClient.getCurrentPrice(anyString(), anyString(), anyString()))
                    .thenReturn(response);

            // when
            String result = stockAdapter.fetchSector("005930");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("TokenExpiredException 발생 시 토큰 무효화 후 재던짐")
        void whenTokenExpired_invalidatesAndRethrows() {
            // given
            when(kisFeignClient.getCurrentPrice(anyString(), anyString(), anyString()))
                    .thenThrow(new TokenExpiredException("KIS 에러", "500"));

            // when & then
            assertThatThrownBy(() -> stockAdapter.fetchSector("005930"))
                    .isInstanceOf(TokenExpiredException.class);

            verify(kisTokenManager).invalidateToken();
        }
    }
}
