package org.project.ssogssog.presentation.controller.stockmetric;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.application.service.stockmetric.api.StockMetricService;
import org.project.ssogssog.presentation.common.ApiResponse;
import org.project.ssogssog.application.service.stockmetric.api.dto.StockMetricRequest;
import org.project.ssogssog.application.service.stockmetric.api.dto.StockMetricResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stock-metric")
@Validated
public class StockMetricController {

    private final StockMetricService stockMetricService;

    @Operation(
            summary = "주식 스크리너 조회",
            description = "PER, ROE, 시가총액, 성장률 등 사용자가 선택한 조건으로 종목을 필터링해 반환합니다."
    )
    @PostMapping("/screener")
    public ApiResponse<StockMetricResponse.ScreenerResponseDTO> getScreener(@Valid @RequestBody StockMetricRequest.ScreenerRequestDTO screenerRequestDTO) {

        StockMetricResponse.ScreenerResponseDTO result = stockMetricService.getScreener(screenerRequestDTO);
        return ApiResponse.onSuccess(result);

        //TODO
        // 페이징을 도입하면 page, size, hasNext 등 추가하기
        // 공통 페이지 응답 래퍼 PageResponse<T> 만들기
    }


}
