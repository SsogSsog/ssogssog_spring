package org.project.ssogssog.application.service.ai.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ssogssog.global.payload.code.status.ErrorStatus;
import org.project.ssogssog.global.payload.exception.GeneralException;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiAskServiceTest {

    private final AiAskService aiAskService = new AiAskService(
            null,
            new ClassPathResource("prompts/stock-assistant-system.st"),
            0.0
    );

    @Test
    @DisplayName("temperature 미지정 시 기본값을 사용한다")
    void resolveTemperature_defaultWhenNull() {
        Double result = aiAskService.resolveTemperature(null);

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("temperature 지정 시 요청값을 사용한다")
    void resolveTemperature_requestedValue() {
        Double result = aiAskService.resolveTemperature(0.7);

        assertThat(result).isEqualTo(0.7);
    }

    @Test
    @DisplayName("temperature 경계값 0.0과 2.0은 허용한다")
    void resolveTemperature_allowsBoundaryValues() {
        assertThat(aiAskService.resolveTemperature(0.0)).isEqualTo(0.0);
        assertThat(aiAskService.resolveTemperature(2.0)).isEqualTo(2.0);
    }

    @Test
    @DisplayName("temperature가 범위를 벗어나면 예외가 발생한다")
    void resolveTemperature_rejectsOutOfRange() {
        assertThatThrownBy(() -> aiAskService.resolveTemperature(-0.1))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorStatus.AI_TEMPERATURE_OUT_OF_RANGE));

        assertThatThrownBy(() -> aiAskService.resolveTemperature(2.1))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorStatus.AI_TEMPERATURE_OUT_OF_RANGE));
    }

    @Test
    @DisplayName("question은 null 또는 blank일 수 없다")
    void validateQuestion_rejectsRequiredValues() {
        assertThatThrownBy(() -> aiAskService.validateQuestion(null))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorStatus.AI_QUESTION_REQUIRED));

        assertThatThrownBy(() -> aiAskService.validateQuestion("  "))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorStatus.AI_QUESTION_REQUIRED));
    }

    @Test
    @DisplayName("question은 1000자까지 허용한다")
    void validateQuestion_allowsUpToMaxLength() {
        assertThatCode(() -> aiAskService.validateQuestion("가".repeat(1000)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("question이 1000자를 초과하면 예외가 발생한다")
    void validateQuestion_rejectsTooLongQuestion() {
        assertThatThrownBy(() -> aiAskService.validateQuestion("가".repeat(1001)))
                .isInstanceOfSatisfying(GeneralException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorStatus.AI_QUESTION_TOO_LONG));
    }

    @Test
    @DisplayName("시스템 프롬프트 리소스를 로드할 수 있다")
    void hasSystemPromptResource() {
        assertThat(aiAskService.hasSystemPromptResource()).isTrue();
    }

}
