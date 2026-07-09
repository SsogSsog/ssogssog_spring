package org.project.ssogssog.application.service.ai.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 종목 분석 질문 요청")
public record AiAskRequest(
        @Schema(description = "AI에게 전달할 질문. 최대 1000자입니다.", example = "삼성전자 PER 어때?")
        String question,

        @Schema(description = "AI 응답 다양성 조절값. 0.0 이상 2.0 이하이며, 미지정 시 기본값을 사용합니다.", example = "0.7")
        Double temperature
) {
}
