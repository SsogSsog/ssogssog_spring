package org.project.ssogssog.application.service.ai.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공시 RAG 질의 응답")
public record RagAskResult(
        @Schema(description = "검색된 공시 제목을 근거로 생성한 AI 답변")
        String answer
) {
}
