package org.project.ssogssog.application.service.ai;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

@Component
public class AiTokenMetrics {

    private final MeterRegistry registry;

    public AiTokenMetrics(MeterRegistry registry) {   // ← 스프링이 자동 주입
        this.registry = registry;
    }

    public void record(String endpoint, ChatResponse chatResponse) {
        String model = chatResponse.getMetadata().getModel();
        Usage usage = chatResponse.getMetadata().getUsage();

        count(endpoint, "input",  model, usage.getPromptTokens());
        count(endpoint, "output", model, usage.getCompletionTokens());
        count(endpoint, "total",  model, usage.getTotalTokens());
    }

    private void count(String endpoint, String type, String model, Integer tokens) {
        if (tokens == null) return;   // null 방어 (프로바이더가 안 줄 때)
        Counter.builder("ai.tokens")              // 메트릭 이름 → ai_tokens_total
                .tag("endpoint", endpoint)
                .tag("type", type)
                .tag("model", model)
                .register(registry)
                .increment(tokens.doubleValue());
    }
}