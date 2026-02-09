package org.project.ssogssog.unit.infra.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ssogssog.application.service.stock.collect.dto.HistoricalPriceDTO;
import org.project.ssogssog.infrastructure.adapter.stock.DailyPriceAdapter;
import org.project.ssogssog.infrastructure.client.common.exception.RateLimitExceededException;
import org.project.ssogssog.infrastructure.client.common.exception.TokenExpiredException;
import org.project.ssogssog.infrastructure.client.feign.kis.KisFeignClient;
import org.project.ssogssog.infrastructure.client.feign.kis.KisTokenManager;
import org.project.ssogssog.infrastructure.client.feign.kis.dto.KisHistoricalPriceResponse;
import org.project.ssogssog.infrastructure.client.feign.kis.dto.KisHolidayResponse;
import org.project.ssogssog.infrastructure.client.feign.kis.dto.KisPriceResponse;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyPriceAdapter 단위 테스트")
class DailyPriceAdapterTest {

    @Mock
    private KisFeignClient kisFeignClient;

    @Mock
    private KisTokenManager kisTokenManager;

    @InjectMocks
    private DailyPriceAdapter dailyPriceAdapter;

    @Nested
    @DisplayName("getPriceRoot")
    class GetPriceRootTest {

        @Test
        @DisplayName("response가 null이면 null 반환")
        void whenResponseNull_returnsNull() {
            // given
            when(kisFeignClient.getCurrentPrice(anyString(), anyString(), anyString()))
                    .thenReturn(null);

            // when
            JsonNode result = dailyPriceAdapter.getPriceRoot("005930");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("response.isSuccess()가 false이고 rate limit 에러이면 RateLimitExceededException 발생")
        void whenRateLimitError_throwsException() {
            // given
            KisPriceResponse response = mock(KisPriceResponse.class);
            when(response.isSuccess()).thenReturn(false);
            when(response.isRateLimitError()).thenReturn(true);
            when(response.getMsg1()).thenReturn("초당 거래건수를 초과하였습니다.");
            when(kisFeignClient.getCurrentPrice(anyString(), anyString(), anyString()))
                    .thenReturn(response);

            // when & then
            assertThatThrownBy(() -> dailyPriceAdapter.getPriceRoot("005930"))
                    .isInstanceOf(RateLimitExceededException.class);
        }

        @Test
        @DisplayName("response.isSuccess()가 false이고 일반 에러이면 null 반환")
        void whenGeneralError_returnsNull() {
            // given
            KisPriceResponse response = mock(KisPriceResponse.class);
            when(response.isSuccess()).thenReturn(false);
            when(response.isRateLimitError()).thenReturn(false);
            when(kisFeignClient.getCurrentPrice(anyString(), anyString(), anyString()))
                    .thenReturn(response);

            // when
            JsonNode result = dailyPriceAdapter.getPriceRoot("005930");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("TokenExpiredException 발생 시 토큰 무효화 후 재던짐")
        void whenTokenExpired_invalidatesAndRethrows() {
            // given
            when(kisFeignClient.getCurrentPrice(anyString(), anyString(), anyString()))
                    .thenThrow(new TokenExpiredException("KIS 에러", "401"));

            // when & then
            assertThatThrownBy(() -> dailyPriceAdapter.getPriceRoot("005930"))
                    .isInstanceOf(TokenExpiredException.class);

            verify(kisTokenManager).invalidateToken();
        }
    }

    @Nested
    @DisplayName("fetchPastPrices")
    class FetchPastPricesTest {

        @Test
        @DisplayName("response가 null이면 null 반환")
        void whenResponseNull_returnsNull() {
            // given
            when(kisFeignClient.getHistoricalPrices(anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(null);

            // when
            HistoricalPriceDTO result = dailyPriceAdapter.fetchPastPrices("005930", "20240101", "20240131");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("response.isSuccess()가 false이고 rate limit 에러이면 RateLimitExceededException 발생")
        void whenRateLimitError_throwsException() {
            // given
            KisHistoricalPriceResponse response = mock(KisHistoricalPriceResponse.class);
            when(response.isSuccess()).thenReturn(false);
            when(response.isRateLimitError()).thenReturn(true);
            when(response.getMsg1()).thenReturn("초당 거래건수를 초과하였습니다.");
            when(kisFeignClient.getHistoricalPrices(anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(response);

            // when & then
            assertThatThrownBy(() -> dailyPriceAdapter.fetchPastPrices("005930", "20240101", "20240131"))
                    .isInstanceOf(RateLimitExceededException.class);
        }

        @Test
        @DisplayName("response.isSuccess()가 false이고 일반 에러이면 null 반환")
        void whenGeneralError_returnsNull() {
            // given
            KisHistoricalPriceResponse response = mock(KisHistoricalPriceResponse.class);
            when(response.isSuccess()).thenReturn(false);
            when(response.isRateLimitError()).thenReturn(false);
            when(kisFeignClient.getHistoricalPrices(anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(response);

            // when
            HistoricalPriceDTO result = dailyPriceAdapter.fetchPastPrices("005930", "20240101", "20240131");

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("TokenExpiredException 발생 시 토큰 무효화 후 재던짐")
        void whenTokenExpired_invalidatesAndRethrows() {
            // given
            when(kisFeignClient.getHistoricalPrices(anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(), anyString()))
                    .thenThrow(new TokenExpiredException("KIS 에러", "401"));

            // when & then
            assertThatThrownBy(() -> dailyPriceAdapter.fetchPastPrices("005930", "20240101", "20240131"))
                    .isInstanceOf(TokenExpiredException.class);

            verify(kisTokenManager).invalidateToken();
        }
    }

    @Nested
    @DisplayName("isMarketOpen")
    class IsMarketOpenTest {

        @Test
        @DisplayName("response가 null이면 false 반환")
        void whenResponseNull_returnsFalse() {
            // given
            when(kisFeignClient.checkHoliday(anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString()))
                    .thenReturn(null);

            // when
            boolean result = dailyPriceAdapter.isMarketOpen(LocalDate.of(2024, 1, 2));

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("response.getOutput()이 null이면 false 반환")
        void whenOutputNull_returnsFalse() {
            // given
            KisHolidayResponse response = mock(KisHolidayResponse.class);
            when(response.getOutput()).thenReturn(null);
            when(kisFeignClient.checkHoliday(anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString()))
                    .thenReturn(response);

            // when
            boolean result = dailyPriceAdapter.isMarketOpen(LocalDate.of(2024, 1, 2));

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("response.getOutput()이 빈 리스트이면 false 반환")
        void whenOutputEmpty_returnsFalse() {
            // given
            KisHolidayResponse response = mock(KisHolidayResponse.class);
            when(response.getOutput()).thenReturn(Collections.emptyList());
            when(kisFeignClient.checkHoliday(anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString()))
                    .thenReturn(response);

            // when
            boolean result = dailyPriceAdapter.isMarketOpen(LocalDate.of(2024, 1, 2));

            // then
            assertThat(result).isFalse();
        }
    }
}
