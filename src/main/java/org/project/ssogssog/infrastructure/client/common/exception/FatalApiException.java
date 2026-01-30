package org.project.ssogssog.infrastructure.client.common.exception;

/**
 * 시나리오 D: 클라이언트 오류 (4xx, 401/403/429 제외)
 * 전략: 재시도 없이 즉시 실패 처리
 * - 잘못된 요청 파라미터
 * - 존재하지 않는 리소스
 */
public class FatalApiException extends ExternalApiException {

    public FatalApiException(String apiName, int httpStatusCode, String message) {
        super(apiName, httpStatusCode, message);
    }

    public FatalApiException(String apiName, int httpStatusCode, String message, Throwable cause) {
        super(apiName, httpStatusCode, message, cause);
    }
}
