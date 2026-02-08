package org.project.ssogssog.infrastructure.client.feign.kis.auth;

import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.project.ssogssog.infrastructure.client.feign.kis.config.KisErrorDecoder;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * KIS 토큰 발급 전용 Feign Client 설정
 */
public class KisTokenFeignConfig {

    @Bean
    public ErrorDecoder kisTokenErrorDecoder() {
        return new KisErrorDecoder();
    }

    /**
     * Feign 레벨 재시도 비활성화 (Resilience4j에 위임)
     */
    @Bean
    public Retryer kisTokenRetryer() {
        return Retryer.NEVER_RETRY;
    }

    /**
     * 타임아웃 설정
     */
    @Bean
    public Request.Options kisTokenRequestOptions() {
        return new Request.Options(
                10, TimeUnit.SECONDS,  // connectTimeout
                10, TimeUnit.SECONDS,  // readTimeout
                true                    // followRedirects
        );
    }
}
