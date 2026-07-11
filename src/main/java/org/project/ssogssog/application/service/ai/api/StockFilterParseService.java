package org.project.ssogssog.application.service.ai.api;

import org.project.ssogssog.application.service.ai.AiTokenMetrics;
import org.project.ssogssog.application.service.ai.api.dto.AiRequest;
import org.project.ssogssog.application.service.ai.api.dto.AiResponse;
import org.project.ssogssog.application.service.ai.api.dto.StockFilterCondition;
import org.project.ssogssog.global.payload.code.status.ErrorStatus;
import org.project.ssogssog.global.payload.exception.GeneralException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * 자연어 종목 검색 질문을 {@link StockFilterCondition}으로 변환하는 서비스.
 * AiAskService(상담)와 달리 Structured Output(.entity())으로 구조화된 필터 조건을 반환한다.
 */
@Service
public class StockFilterParseService {

    static final int MAX_QUESTION_LENGTH = 1000;
    static final double MIN_TEMPERATURE = 0.0;
    static final double MAX_TEMPERATURE = 2.0;

    private final ChatClient chatClient;
    private final Resource systemPrompt;
    private final Double defaultTemperature;
    private final AiTokenMetrics tokenMetrics;

    public StockFilterParseService(
            ChatClient chatClient,
            @Value("classpath:prompts/stock-filter-parser-system.st") Resource systemPrompt,
            @Value("${ssogssog.ai.default-temperature:0.0}") Double defaultTemperature,
            AiTokenMetrics tokenMetrics
    ) {
        this.chatClient = chatClient;
        this.systemPrompt = systemPrompt;
        this.defaultTemperature = defaultTemperature;
        this.tokenMetrics = tokenMetrics;
    }

    public AiResponse.FilterParseDTO parse(final AiRequest.FilterParseDTO request) {
        validateQuestion(request == null ? null : request.question());
        final Double resolvedTemperature = resolveTemperature(request.temperature());

        final ResponseEntity<ChatResponse, StockFilterCondition> responseEntity = chatClient.prompt()
                .system(systemPrompt)
                .user(request.question())
                .options(ChatOptions.builder()
                        .temperature(resolvedTemperature)
                        .build())
                .call()
                .responseEntity(StockFilterCondition.class);

        tokenMetrics.record("parse-filter", responseEntity.getResponse());

        final StockFilterCondition condition = responseEntity.entity();

        return new AiResponse.FilterParseDTO(condition, resolvedTemperature);
    }

    Double resolveTemperature(final Double requestedTemperature) {
        final Double resolvedTemperature = requestedTemperature == null ? defaultTemperature : requestedTemperature;

        if (resolvedTemperature == null
                || resolvedTemperature < MIN_TEMPERATURE
                || resolvedTemperature > MAX_TEMPERATURE) {
            throw new GeneralException(ErrorStatus.AI_TEMPERATURE_OUT_OF_RANGE);
        }

        return resolvedTemperature;
    }

    void validateQuestion(final String question) {
        if (question == null || question.isBlank()) {
            throw new GeneralException(ErrorStatus.AI_QUESTION_REQUIRED);
        }

        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new GeneralException(ErrorStatus.AI_QUESTION_TOO_LONG);
        }
    }

    boolean hasSystemPromptResource() {
        return systemPrompt.exists();
    }

}
