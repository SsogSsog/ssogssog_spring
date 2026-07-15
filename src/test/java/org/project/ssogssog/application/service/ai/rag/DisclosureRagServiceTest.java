package org.project.ssogssog.application.service.ai.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.project.ssogssog.application.service.ai.AiTokenMetrics;
import org.project.ssogssog.application.service.ai.rag.dto.RagAskRequest;
import org.project.ssogssog.application.service.ai.rag.dto.RagAskResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DisclosureRagServiceTest {

    @Test
    @DisplayName("유사 공시를 검색해 프롬프트 컨텍스트로 주입하고 답변을 반환한다")
    @SuppressWarnings("unchecked")
    void ask_retrievesContextAndCallsChatClient() {
        final VectorStore vectorStore = mock(VectorStore.class);
        final ChatClient chatClient = mock(ChatClient.class);
        final ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        final ChatClient.PromptUserSpec userSpec = mock(ChatClient.PromptUserSpec.class);
        final ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        final ChatResponse chatResponse = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        final AiTokenMetrics tokenMetrics = mock(AiTokenMetrics.class);
        final Resource systemPrompt = new ClassPathResource("prompts/disclosure-rag-system.st");
        final Resource userPrompt = new ClassPathResource("prompts/disclosure-rag-user.st");

        final List<Document> foundDocuments = List.of(
                Document.builder()
                        .text("현금·현물배당 결정")
                        .metadata("submitter", "삼성전자")
                        .metadata("date", "2026-07-10")
                        .build(),
                // metadata 없는 문서 → "회사 미상 · 날짜 미상"으로 대체되는지 함께 검증
                Document.builder().text("자기주식취득 결정").build()
        );
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(foundDocuments);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(systemPrompt)).thenReturn(requestSpec);
        when(requestSpec.user(any(Consumer.class))).thenAnswer(invocation -> {
            final Consumer<ChatClient.PromptUserSpec> userCustomizer = invocation.getArgument(0);
            userCustomizer.accept(userSpec);
            return requestSpec;
        });
        when(userSpec.text(userPrompt)).thenReturn(userSpec);
        when(userSpec.param(anyString(), any())).thenReturn(userSpec);
        when(requestSpec.options(any(ChatOptions.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenReturn(chatResponse);
        when(chatResponse.getResult().getOutput().getText()).thenReturn("배당과 자기주식 관련 공시가 있습니다.");

        final DisclosureRagService service = new DisclosureRagService(
                vectorStore,
                chatClient,
                systemPrompt,
                userPrompt,
                tokenMetrics
        );

        final String question = "삼성전자 배당 관련 공시가 있어?";
        final RagAskResult result = service.ask(new RagAskRequest(question));

        final ArgumentCaptor<SearchRequest> searchRequestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(searchRequestCaptor.capture());
        final SearchRequest searchRequest = searchRequestCaptor.getValue();
        assertThat(searchRequest.getQuery()).isEqualTo(question);
        assertThat(searchRequest.getTopK()).isEqualTo(4);
        assertThat(searchRequest.hasFilterExpression()).isFalse();

        verify(userSpec).text(userPrompt);
        verify(userSpec).param("context", "1. [삼성전자 · 2026-07-10] 현금·현물배당 결정" + System.lineSeparator()
                + "2. [회사 미상 · 날짜 미상] 자기주식취득 결정");
        verify(userSpec).param("question", question);
        verify(tokenMetrics).record("rag-ask", chatResponse);
        assertThat(result.answer()).isEqualTo("배당과 자기주식 관련 공시가 있습니다.");
    }
}
