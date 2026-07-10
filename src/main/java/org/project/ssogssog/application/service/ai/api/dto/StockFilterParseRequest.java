package org.project.ssogssog.application.service.ai.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자연어 종목 검색 파싱 요청")
public record StockFilterParseRequest(
        @Schema(description = "필터 조건으로 변환할 자연어 질문. 최대 1000자입니다.",
                example = "PER 10 이하 배당수익률 3% 이상 종목")
        String question,

        @Schema(description = "AI 응답 다양성 조절값. 0.0 이상 2.0 이하이며, 미지정 시 기본값을 사용합니다. 파싱은 보통 낮은 값이 유리합니다.",
                example = "0.0")
        Double temperature
) {
}
