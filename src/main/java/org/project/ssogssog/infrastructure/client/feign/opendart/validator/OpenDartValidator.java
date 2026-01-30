package org.project.ssogssog.infrastructure.client.feign.opendart.validator;

import com.fasterxml.jackson.databind.JsonNode;
import org.project.ssogssog.infrastructure.client.common.exception.FatalApiException;
import org.project.ssogssog.infrastructure.client.common.exception.RateLimitExceededException;
import org.project.ssogssog.infrastructure.client.common.exception.RetryableApiException;

public class OpenDartValidator {

    /**
     * OpenDART 응답 코드를 검사하여 에러 상황 시 적절한 예외를 던짐
     * - 000 (성공), 013 (데이터 없음) -> 통과
     * - 그 외 -> 예외 발생 (Resilience4j 트리거)
     */
    public static void validate(JsonNode root) {
        if (root == null) {
            throw new RetryableApiException("OpenDART", 500, "응답이 비어있습니다.");
        }

        String status = root.path("status").asText();
        String message = root.path("message").asText();

        switch (status) {
            case "000": // 정상
            case "013": // 데이터 없음 (비즈니스 로직에서 null 처리하도록 통과)
                return;

            case "020": // 사용량 제한 초과 -> 재시도 대상 (RateLimitExceededException)
                throw new RateLimitExceededException("OpenDART", 60L, message);

            case "010": // 미등록 키
            case "011": // 키 만료
                throw new FatalApiException("OpenDART", 401, message);

            case "800": // 시스템 점검 -> 재시도 대상
            case "900": // 정의되지 않은 오류
            default:    // 그 외 알 수 없는 코드도 일단 재시도 시도
                throw new RetryableApiException("OpenDART", 500, "서버 오류: " + message);
        }
    }
}