package org.project.ssogssog.infrastructure.client.feign.naver.config;

import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * 네이버 Feign Client 격리 설정
 *
 * 주의: @Configuration 사용 금지!
 * - @Configuration을 사용하면 전역 Bean으로 등록되어 다른 Feign Client에 영향을 줌
 * - 이 설정은 @FeignClient(configuration = NaverFeignConfig.class)로만 적용됨
 */
public class NaverFeignConfig {

    /**
     * HTTP 상태 코드 → 커스텀 예외 변환
     */
    @Bean
    public ErrorDecoder naverErrorDecoder() {
        return new NaverErrorDecoder();
    }

    /**
     * Client ID/Secret 헤더 자동 주입
     */
    @Bean
    public RequestInterceptor naverRequestInterceptor() {
        return new NaverRequestInterceptor();
    }

    /**
     * Feign 레벨 재시도 비활성화
     * - 재시도는 Resilience4j @Retry에 위임
     */
    @Bean
    public Retryer naverRetryer() {
        return Retryer.NEVER_RETRY;
    }

    /**
     * 타임아웃 설정
     */
    @Bean
    public Request.Options naverRequestOptions() {
        return new Request.Options(
                5, TimeUnit.SECONDS,   // connectTimeout
                10, TimeUnit.SECONDS,  // readTimeout
                true                    // followRedirects
        );
    }
}
