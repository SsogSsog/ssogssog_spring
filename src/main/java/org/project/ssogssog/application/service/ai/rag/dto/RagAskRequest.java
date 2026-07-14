package org.project.ssogssog.application.service.ai.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공시 RAG 질의 요청")
public record RagAskRequest(
        @Schema(
                description = "pgvector에 적재된 공시를 근거로 답변할 질문. 최대 1000자입니다.",
                example = "삼성전자 배당 관련 공시가 있어?"
        )
        String question
) {
}
