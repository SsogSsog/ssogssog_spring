package org.project.ssogssog.presentation.controller.stock;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.application.service.stock.api.StockService;
import org.project.ssogssog.presentation.common.ApiResponse;
import org.project.ssogssog.presentation.controller.stock.dto.StockResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stock")
@Validated
public class StockController {

    private final StockService stockService;

    @GetMapping
    @Operation(
            summary = "테마별 주식 개수 + 변동률 평균 목록 조회",
            description = """
            테마 단위로 주식을 그룹핑하여 최신 변동률(changeRate)의 평균을 계산한 목록을 반환합니다.
            응답의 totalCount는 해당 테마의 주식 개수입니다.
            """
    )
    public ApiResponse<StockResponse.ThemeResponseDTO> getThemeStockStats(){

        StockResponse.ThemeResponseDTO result = stockService.getThemeStockStats();
        return ApiResponse.onSuccess(result);

    }

}