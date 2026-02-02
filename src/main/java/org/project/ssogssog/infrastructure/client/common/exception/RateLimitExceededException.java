package org.project.ssogssog.infrastructure.client.common.exception;

import lombok.Getter;

/**
 * 시나리오 B: 호출 제한 초과 (429, Rate Limit)
 */
@Getter
public class RateLimitExceededException extends ExternalApiException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String apiName, long retryAfterSeconds) {
        super(apiName, 429, "Rate limit exceeded. Retry after " + retryAfterSeconds + " seconds");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitExceededException(String apiName, long retryAfterSeconds, String message) {
        super(apiName, 429, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
