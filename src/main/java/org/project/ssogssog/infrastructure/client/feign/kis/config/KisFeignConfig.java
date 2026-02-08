package org.project.ssogssog.infrastructure.client.feign.kis.config;

import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.project.ssogssog.infrastructure.client.feign.kis.KisTokenManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * KIS Feign Client 격리 설정
 *
 * - 이 설정은 @FeignClient(configuration = KisFeignConfig.class)로만 적용됨
 */
public class KisFeignConfig {

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    /**
     * HTTP 상태 코드 → 커스텀 예외 변환
     */
    @Bean
    public ErrorDecoder kisErrorDecoder() {
        return new KisErrorDecoder();
    }

    /**
     * 토큰 및 공통 헤더 자동 주입
     */
    @Bean
    public RequestInterceptor kisRequestInterceptor(KisTokenManager tokenManager) {
        return new KisRequestInterceptor(tokenManager, appKey, appSecret);
    }

    /**
     * Feign 레벨 재시도 비활성화
     * - 재시도는 Resilience4j @Retry에 위임
     */
    @Bean
    public Retryer kisRetryer() {
        return Retryer.NEVER_RETRY;
    }

    /**
     * 타임아웃 설정
     */
    @Bean
    public Request.Options kisRequestOptions() {
        return new Request.Options(
                10, TimeUnit.SECONDS,  // connectTimeout
                10, TimeUnit.SECONDS,  // readTimeout
                true                    // followRedirects
        );
    }
}
