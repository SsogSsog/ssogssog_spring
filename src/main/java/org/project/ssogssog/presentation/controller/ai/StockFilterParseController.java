package org.project.ssogssog.presentation.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.application.service.ai.api.StockFilterParseService;
import org.project.ssogssog.application.service.ai.api.dto.AiRequest;
import org.project.ssogssog.application.service.ai.api.dto.AiResponse;
import org.project.ssogssog.global.payload.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
@Profile("!prod")
public class StockFilterParseController {

    private final StockFilterParseService stockFilterParseService;

    @PostMapping("/parse-filter")
    @Operation(
            summary = "자연어 → 주식 필터 조건 변환",
            description = """
            자연어 종목 검색 질문을 구조화된 필터 조건(StockFilterCondition)으로 파싱합니다.
            Spring AI Structured Output(.entity())을 사용하며, 운영 프로파일에서는 노출되지 않습니다.
            """
    )
    public ApiResponse<AiResponse.FilterParseDTO> parseFilter(@RequestBody AiRequest.FilterParseDTO request) {
        AiResponse.FilterParseDTO result = stockFilterParseService.parse(request);
        return ApiResponse.onSuccess(result);
    }

}
