package org.project.ssogssog.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ssogssog.application.service.stock.api.StockService;
import org.project.ssogssog.domain.stock.vo.ThemeItemDTO;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.presentation.controller.stock.dto.StockResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    StockRepository stockRepository;

    @InjectMocks
    StockService stockService;

    @Test
    @DisplayName("섹터별 그룹핑 후 평균 계산 + themeName 오름차순 정렬 + totalCount는 그룹 개수")
    void getThemeStockStats_groupAndAverageAndSort() {
        // given
        List<ThemeItemDTO> items = List.of(
                new ThemeItemDTO("IT", 1.0),
                new ThemeItemDTO("IT", 3.0),
                new ThemeItemDTO("BIO", 10.0),
                new ThemeItemDTO("BIO", 20.0),
                new ThemeItemDTO("BIO", 30.0)
        );

        when(stockRepository.getThemeStockStats()).thenReturn(items);

        // when
        StockResponse.ThemeResponseDTO result = stockService.getThemeStockStats();

        // then
        verify(stockRepository, times(1)).getThemeStockStats();

        assertThat(result).isNotNull();
        assertThat(result.getTotalCount()).isEqualTo(2); // BIO, IT 두 그룹

        List<StockResponse.ThemeCollectedItemDTO> collected = result.getItems();
        assertThat(collected).hasSize(2);

        // 정렬: themeName 오름차순 (BIO, IT)
        assertThat(collected.get(0).getThemeName()).isEqualTo("BIO");
        assertThat(collected.get(1).getThemeName()).isEqualTo("IT");

        // BIO 평균 = (10+20+30)/3 = 20
        StockResponse.ThemeCollectedItemDTO bio = collected.get(0);
        assertThat(bio.getTotalCount()).isEqualTo(3);
        assertThat(bio.getSum()).isEqualTo(60.0);
        assertThat(bio.getChangeRateAverage()).isEqualTo(20.0);

        // IT 평균 = (1+3)/2 = 2
        StockResponse.ThemeCollectedItemDTO it = collected.get(1);
        assertThat(it.getTotalCount()).isEqualTo(2);
        assertThat(it.getSum()).isEqualTo(4.0);
        assertThat(it.getChangeRateAverage()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("themeName null/빈값 또는 changeRate null 은 필터링되어 결과에서 제외된다")
    void getThemeStockStats_filtersInvalidItems() {
        // given
        List<ThemeItemDTO> items = List.of(
                new ThemeItemDTO(null, 1.0),     // 제외
                new ThemeItemDTO("", 2.0),       // 제외
                new ThemeItemDTO("  ", 3.0),     // 제외
                new ThemeItemDTO("IT", null),    // 제외
                new ThemeItemDTO("IT", 5.0)      // 포함
        );

        when(stockRepository.getThemeStockStats()).thenReturn(items);

        // when
        StockResponse.ThemeResponseDTO result = stockService.getThemeStockStats();

        // then
        verify(stockRepository, times(1)).getThemeStockStats();

        assertThat(result).isNotNull();
        
        assertThat(result.getItems())
                .anyMatch(dto -> "IT".equals(dto.getThemeName()));

        StockResponse.ThemeCollectedItemDTO it = result.getItems().stream()
                .filter(dto -> "IT".equals(dto.getThemeName()))
                .findFirst()
                .orElseThrow();

        assertThat(it.getTotalCount()).isEqualTo(1);
        assertThat(it.getSum()).isEqualTo(5.0);
        assertThat(it.getChangeRateAverage()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("유효한 데이터가 하나도 없으면 빈 리스트와 totalCount=0을 반환한다")
    void getThemeStockStats_emptyWhenAllInvalid() {
        // given
        List<ThemeItemDTO> items = List.of(
                new ThemeItemDTO(null, 1.0),
                new ThemeItemDTO("", 2.0),
                new ThemeItemDTO("IT", null)
        );

        when(stockRepository.getThemeStockStats()).thenReturn(items);

        // when
        StockResponse.ThemeResponseDTO result = stockService.getThemeStockStats();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalCount()).isEqualTo(0);
    }
}
