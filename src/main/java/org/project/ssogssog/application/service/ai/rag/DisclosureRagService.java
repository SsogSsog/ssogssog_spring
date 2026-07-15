package org.project.ssogssog.application.service.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.ai.AiTokenMetrics;
import org.project.ssogssog.application.service.ai.rag.dto.RagAskRequest;
import org.project.ssogssog.application.service.ai.rag.dto.RagAskResult;
import org.project.ssogssog.global.payload.code.status.ErrorStatus;
import org.project.ssogssog.global.payload.exception.GeneralException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * pgvector의 공시를 검색하고 검색 결과를 Gemini 프롬프트에 주입하는 수동 RAG 서비스.
 *
 * <p>학습 목표를 위해 Spring AI의 QuestionAnswerAdvisor 대신
 * 검색 → 컨텍스트 조립 → 답변 생성을 직접 연결한다.
 */
@Service
@Slf4j
public class DisclosureRagService {

    static final int TOP_K = 4;
    static final int MAX_QUESTION_LENGTH = 1000;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final Resource systemPrompt;
    private final Resource userPrompt;
    private final AiTokenMetrics tokenMetrics;

    public DisclosureRagService(
            VectorStore vectorStore,
            ChatClient chatClient,
            @Value("classpath:prompts/disclosure-rag-system.st") Resource systemPrompt,
            @Value("classpath:prompts/disclosure-rag-user.st") Resource userPrompt,
            AiTokenMetrics tokenMetrics
    ) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
        this.tokenMetrics = tokenMetrics;
    }

    public RagAskResult ask(final RagAskRequest request) {
        validateQuestion(request == null ? null : request.question());

        // 1. 질문을 임베딩하고, pgvector에서 의미가 가까운 공시를 검색한다.
        final List<Document> retrievedDocuments = searchSimilarDisclosures(request.question());

        // 2. 검색된 Document의 text(공시 제목)를 LLM이 읽을 수 있는 문자열로 조립한다.
        final String context = buildContext(retrievedDocuments);

        // 3. 검색 컨텍스트와 사용자 질문을 프롬프트에 주입해 답변을 생성한다.
        final ChatResponse response = chatClient.prompt()
                .system(systemPrompt)
                .user(user -> user
                        .text(userPrompt)
                        .param("context", context)
                        .param("question", request.question()))
                .options(ChatOptions.builder()
                        .temperature(0.0)
                        .build())
                .call()
                .chatResponse();

        tokenMetrics.record("rag-ask", response);

        final String answer = response.getResult().getOutput().getText();
        return new RagAskResult(answer);
    }

    /**
     * filterExpression 없이 질문의 의미만으로 가까운 공시 TOP_K개를 검색한다.
     * 질문 임베딩 생성은 VectorStore가 내부의 EmbeddingModel을 사용해 자동 처리한다.
     */
    private List<Document> searchSimilarDisclosures(final String question) {
        final SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(TOP_K)
                .build();

        final List<Document> found = vectorStore.similaritySearch(searchRequest);
        final List<Document> documents = found == null ? List.of() : found;
        log.info("RAG 공시 검색 완료 - retrieved: {}", documents.stream()
                .map(document -> "%s(score=%s)".formatted(document.getText(), document.getScore()))
                .toList());
        return documents;
    }

    /** 검색된 공시를 [회사 · 날짜] 제목 형태의 번호 붙은 프롬프트 컨텍스트로 변환한다. */
    private String buildContext(final List<Document> documents) {
        if (documents.isEmpty()) {
            return "검색된 공시가 없습니다.";
        }

        return IntStream.range(0, documents.size())
                .mapToObj(index -> {
                    final Document document = documents.get(index);
                    final String submitter = metadataOrDefault(document, "submitter", "회사 미상");
                    final String date = metadataOrDefault(document, "date", "날짜 미상");
                    return "%d. [%s · %s] %s".formatted(index + 1, submitter, date, document.getText());
                })
                .collect(Collectors.joining(System.lineSeparator()));
    }

    /** metadata 값을 문자열로 꺼내되, 없거나 비어 있으면 기본값을 돌려준다. */
    private String metadataOrDefault(final Document document, final String key, final String defaultValue) {
        final Object value = document.getMetadata().get(key);
        if (value == null || value.toString().isBlank()) {
            return defaultValue;
        }
        return value.toString();
    }

    private void validateQuestion(final String question) {
        if (question == null || question.isBlank()) {
            throw new GeneralException(ErrorStatus.AI_QUESTION_REQUIRED);
        }

        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new GeneralException(ErrorStatus.AI_QUESTION_TOO_LONG);
        }
    }
}
