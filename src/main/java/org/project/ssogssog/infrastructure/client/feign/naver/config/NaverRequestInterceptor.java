package org.project.ssogssog.infrastructure.client.feign.naver.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * 네이버 API 요청 인터셉터
 * - 모든 요청에 Client ID/Secret 헤더 자동 주입
 */
@Slf4j
public class NaverRequestInterceptor implements RequestInterceptor {

    private final String clientId;
    private final String clientSecret;

    public NaverRequestInterceptor(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header("X-Naver-Client-Id", clientId);
        template.header("X-Naver-Client-Secret", clientSecret);

        log.debug("네이버 요청 헤더 주입 완료 - path: {}", template.path());
    }
}
