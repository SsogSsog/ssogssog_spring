package org.project.ssogssog.infrastructure.client.feign.naver.config;

import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.infrastructure.client.common.exception.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 네이버 API 에러 응답을 커스텀 예외로 변환
 */
@Slf4j
public class NaverErrorDecoder implements ErrorDecoder {

    private static final String API_NAME = "Naver";
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String body = extractBody(response);

        log.warn("Naver API 에러 - method: {}, status: {}, body: {}", methodKey, status, body);

        // 인증 오류
        if (status == 401 || status == 403) {
            return new FatalApiException(API_NAME, status,
                    "네이버 API 인증 오류 (Client ID/Secret 확인 필요): " + body);
        }

        // Rate Limit
        if (status == 429) {
            return new RateLimitExceededException(API_NAME, 1L,
                    "네이버 API 호출 제한 초과: " + body);
        }

        // 서버 오류 (재시도 가능)
        if (status >= 500) {
            return new RetryableApiException(API_NAME, status,
                    "네이버 서버 오류: " + body);
        }

        // 클라이언트 오류 (재시도 불가)
        if (status >= 400) {
            return new FatalApiException(API_NAME, status,
                    "네이버 API 호출 실패: " + body);
        }

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
