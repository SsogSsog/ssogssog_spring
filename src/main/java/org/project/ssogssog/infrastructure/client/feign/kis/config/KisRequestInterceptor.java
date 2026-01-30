package org.project.ssogssog.infrastructure.client.feign.kis.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.infrastructure.client.feign.kis.KisTokenManager;
import org.springframework.beans.factory.annotation.Value;

/**
 * KIS API 요청 인터셉터
 * - 모든 요청에 인증 토큰 및 필수 헤더 자동 주입
 * - 토큰 발급 API(/oauth2/tokenP)는 제외
 */
@Slf4j
public class KisRequestInterceptor implements RequestInterceptor {

    private final KisTokenManager tokenManager;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    public KisRequestInterceptor(KisTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public void apply(RequestTemplate template) {
        // 토큰 발급 API는 인터셉터 제외 (토큰이 필요 없음)
        if (template.path().contains("/oauth2/tokenP")) {
            log.debug("토큰 발급 API - 인터셉터 스킵");
            return;
        }

        String accessToken = tokenManager.getValidToken();

        if (accessToken == null) {
            log.warn("KIS 토큰을 가져올 수 없습니다");
            return;
        }

        // 공통 헤더 설정
        template.header("Content-Type", "application/json; charset=utf-8");
        template.header("authorization", "Bearer " + accessToken);
        template.header("appkey", appKey);
        template.header("appsecret", appSecret);

        log.debug("KIS 요청 헤더 주입 완료 - path: {}", template.path());
    }
}
