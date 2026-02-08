package org.project.ssogssog.infrastructure.client.feign.kis.config;

import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.infrastructure.client.common.exception.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * KIS API 에러 응답을 커스텀 예외로 변환
 * 시나리오별 예외 매핑:
 * - A (401, 403): TokenExpiredException
 * - B (429, EGW00201): RateLimitExceededException
 * - C (5xx): RetryableApiException
 * - D (기타 4xx): FatalApiException
 */
@Slf4j
public class KisErrorDecoder implements ErrorDecoder {

    private static final String API_NAME = "KIS";
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String body = extractBody(response);

        log.warn("KIS API 에러 - method: {}, status: {}, body: {}", methodKey, status, body);

        // 시나리오 A: 인증/토큰 만료
        if (status == 401 || status == 403) {
            return new TokenExpiredException(API_NAME, status,
                    "KIS 토큰이 만료되었거나 인증에 실패했습니다: " + body);
        }

        // 시나리오 B: 호출 제한 초과
        if (status == 429) {
            return new RateLimitExceededException(API_NAME, 1L,
                    "KIS API 호출 제한 초과: " + body);
        }

        // 시나리오 C: 서버 오류 (재시도 가능)
        if (status >= 500) {
            return new RetryableApiException(API_NAME, status,
                    "KIS 서버 오류: " + body);
        }

        // 시나리오 D: 클라이언트 오류 (재시도 불가)
        if (status >= 400) {
            return new FatalApiException(API_NAME, status,
                    "KIS API 호출 실패: " + body);
        }

        // 기타 예상치 못한 에러
        return defaultDecoder.decode(methodKey, response);
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
