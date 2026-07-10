package org.project.ssogssog.presentation.controller.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ssogssog.application.service.ai.api.AiAskService;
import org.project.ssogssog.application.service.ai.api.dto.AiRequest;
import org.project.ssogssog.application.service.ai.api.dto.AiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiAskController.class)
@ActiveProfiles("test")
class AiAskControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AiAskService aiAskService;

    @Test
    @DisplayName("POST /ai/ask는 ApiResponse 봉투로 AI 답변과 적용 temperature를 반환한다")
    void ask() throws Exception {
        when(aiAskService.ask(any(AiRequest.AskDTO.class)))
                .thenReturn(new AiResponse.AskDTO("PER은 주가수익비율입니다.", 0.7));

        mockMvc.perform(post("/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "question": "삼성전자 PER 어때?",
                                  "temperature": 0.7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.result.answer").value("PER은 주가수익비율입니다."))
                .andExpect(jsonPath("$.result.temperature").value(0.7));
    }

}
