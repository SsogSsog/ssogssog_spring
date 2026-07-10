package org.project.ssogssog.application.service.ai.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자연어 종목 검색 파싱 응답")
public record StockFilterParseResponse(
        @Schema(description = "자연어 질문에서 추출한 주식 필터 조건")
        StockFilterCondition condition,

        @Schema(description = "실제 AI 호출에 적용된 temperature 값", example = "0.0")
        Double temperature
) {
}
