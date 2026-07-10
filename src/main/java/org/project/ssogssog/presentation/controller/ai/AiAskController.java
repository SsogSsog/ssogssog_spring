package org.project.ssogssog.presentation.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.application.service.ai.api.AiAskService;
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
public class AiAskController {

    private final AiAskService aiAskService;

    @PostMapping("/ask")
    @Operation(
            summary = "AI 종목 분석 질문",
            description = """
            주식쏙쏙 도메인 시스템 프롬프트와 선택 temperature 값을 적용해 AI 답변을 반환합니다.
            운영 프로파일에서는 노출되지 않는 실험용 엔드포인트입니다.
            """
    )
    public ApiResponse<AiResponse.AskDTO> ask(@RequestBody AiRequest.AskDTO request) {
        AiResponse.AskDTO result = aiAskService.ask(request);
        return ApiResponse.onSuccess(result);
    }

}
