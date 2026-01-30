package org.project.ssogssog.infrastructure.client.feign.opendart.config;

import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * OpenDART Feign Client 격리 설정
 *
 * 주의: @Configuration 사용 금지!
 * - @Configuration을 사용하면 전역 Bean으로 등록되어 다른 Feign Client에 영향을 줌
 * - 이 설정은 @FeignClient(configuration = OpenDartFeignConfig.class)로만 적용됨
 */
public class OpenDartFeignConfig {

    /**
     * HTTP 상태 코드 → 커스텀 예외 변환
     */
    @Bean
    public ErrorDecoder openDartErrorDecoder() {
        return new OpenDartErrorDecoder();
    }

    /**
     * Feign 레벨 재시도 비활성화
     * - 재시도는 Resilience4j @Retry에 위임
     */
    @Bean
    public Retryer openDartRetryer() {
        return Retryer.NEVER_RETRY;
    }

    /**
     * 타임아웃 설정
     * - OpenDART는 재무정보 조회 시 시간이 오래 걸릴 수 있음
     */
    @Bean
    public Request.Options openDartRequestOptions() {
        return new Request.Options(
                10, TimeUnit.SECONDS,  // connectTimeout
                30, TimeUnit.SECONDS,  // readTimeout (재무정보 조회용)
                true                    // followRedirects
        );
    }
}
