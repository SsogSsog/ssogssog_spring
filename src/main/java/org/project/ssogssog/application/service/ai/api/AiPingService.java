package org.project.ssogssog.application.service.ai.api;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Spring AI 연동 확인용 서비스.
 * 고정 프롬프트를 Gemini(OpenAI 호환 엔드포인트)로 보내 응답 텍스트를 반환한다.
 */
@Service
@RequiredArgsConstructor
public class AiPingService {

    private static final String PING_PROMPT = "안녕! 너와 연결됐는지 확인 중이야. 한 문장으로 짧게 인사해줘.";

    private final ChatClient chatClient;

    public String ping() {
        return chatClient.prompt()
                .user(PING_PROMPT)
                .call()
                .content();
    }

}
