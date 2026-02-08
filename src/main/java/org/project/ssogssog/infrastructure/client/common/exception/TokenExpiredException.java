package org.project.ssogssog.infrastructure.client.common.exception;

/**
 * 시나리오 A: 인증/토큰 만료 (401, 403)
 */
public class TokenExpiredException extends ExternalApiException {

    public TokenExpiredException(String apiName, String message) {
        super(apiName, 401, message);
    }

    public TokenExpiredException(String apiName, int httpStatusCode, String message) {
        super(apiName, httpStatusCode, message);
    }
}
