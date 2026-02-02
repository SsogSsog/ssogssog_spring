package org.project.ssogssog.infrastructure.client.common.exception;

import lombok.Getter;

/**
 * 외부 API 호출 시 발생하는 예외의 기본 클래스
 * 모든 외부 API 관련 예외는 이 클래스를 상속받아야 함
 */
@Getter
public abstract class ExternalApiException extends RuntimeException {

    private final String apiName;
    private final int httpStatusCode;

    protected ExternalApiException(String apiName, int httpStatusCode, String message) {
        super(message);
        this.apiName = apiName;
        this.httpStatusCode = httpStatusCode;
    }

    protected ExternalApiException(String apiName, int httpStatusCode, String message, Throwable cause) {
        super(message, cause);
        this.apiName = apiName;
        this.httpStatusCode = httpStatusCode;
    }
}
