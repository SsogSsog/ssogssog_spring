package org.project.ssogssog.application.service.ai.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 종목 분석 질문 응답")
public record AiAskResponse(
        @Schema(description = "AI 답변 텍스트")
        String answer,

        @Schema(description = "실제 AI 호출에 적용된 temperature 값", example = "0.7")
        Double temperature
) {
}
