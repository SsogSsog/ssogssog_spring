package org.project.ssogssog.presentation.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.application.service.ai.api.AiPingService;
import org.project.ssogssog.global.payload.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiPingController {

    private final AiPingService aiPingService;

    @GetMapping("/ping")
    @Operation(
            summary = "Spring AI 연동 확인용 핑",
            description = """
            고정 프롬프트를 Gemini(OpenAI 호환 엔드포인트)로 보내 응답 텍스트를 반환합니다.
            서버 경유로 AI 응답이 정상적으로 오는지 확인하기 위한 테스트 엔드포인트입니다.
            """
    )
    public ApiResponse<String> ping() {
        String result = aiPingService.ping();
        return ApiResponse.onSuccess(result);
    }

}
