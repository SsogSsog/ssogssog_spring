package org.project.ssogssog.infrastructure.client.common.exception;

/**
 * 시나리오 C: 일시적 장애 (5xx, Timeout, IOException)
 * 전략: 지수 백오프(Exponential Backoff)를 적용하여 2~3회 재시도
 */
public class RetryableApiException extends ExternalApiException {

    public RetryableApiException(String apiName, int httpStatusCode, String message) {
        super(apiName, httpStatusCode, message);
    }

    public RetryableApiException(String apiName, int httpStatusCode, String message, Throwable cause) {
        super(apiName, httpStatusCode, message, cause);
    }

    /**
     * 타임아웃, 네트워크 오류 등 HTTP 상태 코드가 없는 경우
     */
    public RetryableApiException(String apiName, String message, Throwable cause) {
        super(apiName, 0, message, cause);
    }
}
