package org.project.ssogssog.presentation.controller.stockmetric;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.application.service.stockmetric.api.StockMetricService;
import org.project.ssogssog.global.paging.SliceDTO;
import org.project.ssogssog.global.payload.ApiResponse;
import org.project.ssogssog.application.service.stockmetric.api.dto.StockMetricRequest;
import org.project.ssogssog.application.service.stockmetric.api.dto.StockMetricResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static org.springframework.data.domain.Sort.Direction.DESC;

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
    public ApiResponse<SliceDTO<StockMetricResponse.ScreenerItemDTO>> getScreener(
            @Valid @RequestBody StockMetricRequest.ScreenerRequestDTO screenerRequestDTO,
            @PageableDefault(size=10, sort="currentPrice", direction=DESC) Pageable pageable) {

        SliceDTO<StockMetricResponse.ScreenerItemDTO> result = stockMetricService.getScreener(screenerRequestDTO, pageable);
        return ApiResponse.onSuccess(result);
    }


}
