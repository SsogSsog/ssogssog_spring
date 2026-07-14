package org.project.ssogssog.presentation.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.application.service.ai.rag.DisclosureRagService;
import org.project.ssogssog.application.service.ai.rag.dto.RagAskRequest;
import org.project.ssogssog.application.service.ai.rag.dto.RagAskResult;
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
public class DisclosureRagController {

    private final DisclosureRagService disclosureRagService;

    @PostMapping("/rag-ask")
    @Operation(
            summary = "공시 근거 기반 AI 질의",
            description = """
                    pgvector에서 질문과 의미가 유사한 공시 제목 4건을 검색하고,
                    검색 결과를 근거로 Gemini가 생성한 답변을 반환합니다.
                    운영 프로파일에서는 노출되지 않는 RAG 학습용 엔드포인트입니다.
                    """
    )
    public ApiResponse<RagAskResult> ask(@RequestBody final RagAskRequest request) {
        return ApiResponse.onSuccess(disclosureRagService.ask(request));
    }
}
