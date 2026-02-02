package org.project.ssogssog.infrastructure.client.feign.opendart.config;

import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.infrastructure.client.common.exception.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * OpenDART API 에러 응답을 커스텀 예외로 변환
 *
 * OpenDART 에러 코드:
 * - "000": 정상
 * - "010": 등록되지 않은 키
 * - "011": 사용기간 만료
 * - "020": 요청 제한 초과
 * - "800": 시스템 점검
 * - "900": 정의되지 않은 오류
 */
@Slf4j
public class OpenDartErrorDecoder implements ErrorDecoder {

    private static final String API_NAME = "OpenDART";
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String body = extractBody(response);

        log.warn("OpenDART API 에러 - method: {}, status: {}, body: {}", methodKey, status, body);

        // HTTP 상태 코드 기반 처리
        if (status == 429 || isRateLimitError(body)) {
            return new RateLimitExceededException(API_NAME, 60L,
                    "OpenDART API 호출 제한 초과: " + body);
        }

        if (status >= 500 || isServerError(body)) {
            return new RetryableApiException(API_NAME, status,
                    "OpenDART 서버 오류: " + body);
        }

        if (status == 401 || status == 403 || isAuthError(body)) {
            // OpenDART는 토큰 갱신이 없으므로 FatalApiException 처리
            return new FatalApiException(API_NAME, status,
                    "OpenDART 인증 오류 (API Key 확인 필요): " + body);
        }

        if (status >= 400) {
            return new FatalApiException(API_NAME, status,
                    "OpenDART API 호출 실패: " + body);
        }

        return defaultDecoder.decode(methodKey, response);
    }

    /**
     * Rate Limit 에러 확인 (status: "020")
     */
    private boolean isRateLimitError(String body) {
        return body != null && body.contains("\"status\":\"020\"");
    }

    /**
     * 서버 오류 확인 (status: "800", "900")
     */
    private boolean isServerError(String body) {
        return body != null &&
                (body.contains("\"status\":\"800\"") || body.contains("\"status\":\"900\""));
    }

    /**
     * 인증 오류 확인 (status: "010", "011")
     */
    private boolean isAuthError(String body) {
        return body != null &&
                (body.contains("\"status\":\"010\"") || body.contains("\"status\":\"011\""));
    }

    private String extractBody(Response response) {
        try {
            if (response.body() != null) {
                return Util.toString(response.body().asReader(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            log.warn("응답 body 읽기 실패", e);
        }
        return "";
    }
}
