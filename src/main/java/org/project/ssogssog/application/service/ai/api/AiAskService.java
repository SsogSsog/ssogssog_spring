package org.project.ssogssog.application.service.ai.api;

import org.project.ssogssog.application.service.ai.AiTokenMetrics;
import org.project.ssogssog.application.service.ai.api.dto.AiRequest;
import org.project.ssogssog.application.service.ai.api.dto.AiResponse;
import org.project.ssogssog.global.payload.code.status.ErrorStatus;
import org.project.ssogssog.global.payload.exception.GeneralException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class AiAskService {

    static final int MAX_QUESTION_LENGTH = 1000;
    static final double MIN_TEMPERATURE = 0.0;
    static final double MAX_TEMPERATURE = 2.0;

    private final ChatClient chatClient;
    private final Resource systemPrompt;
    private final Double defaultTemperature;
    private final AiTokenMetrics tokenMetrics;

    public AiAskService(
            ChatClient chatClient,
            @Value("classpath:prompts/stock-assistant-system.st") Resource systemPrompt,
            @Value("${ssogssog.ai.default-temperature:0.0}") Double defaultTemperature,
            AiTokenMetrics tokenMetrics
    ) {
        this.chatClient = chatClient;
        this.systemPrompt = systemPrompt;
        this.defaultTemperature = defaultTemperature;
        this.tokenMetrics = tokenMetrics;
    }

    public AiResponse.AskDTO ask(final AiRequest.AskDTO request) {
        validateQuestion(request == null ? null : request.question());
        final Double resolvedTemperature = resolveTemperature(request.temperature());

        final ChatResponse response = chatClient.prompt()
                .system(systemPrompt)
                .user(request.question())
                .options(ChatOptions.builder()
                        .temperature(resolvedTemperature)
                        .build())
                .call()
                .chatResponse();

        final String answer = response.getResult().getOutput().getText();

        tokenMetrics.record("ask", response);

        return new AiResponse.AskDTO(answer, resolvedTemperature);
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
