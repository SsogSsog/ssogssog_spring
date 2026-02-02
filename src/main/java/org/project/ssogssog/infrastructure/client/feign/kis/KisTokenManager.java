package org.project.ssogssog.infrastructure.client.feign.kis;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.infrastructure.client.feign.kis.auth.KisTokenFeignClient;
import org.project.ssogssog.infrastructure.client.feign.kis.dto.KisTokenRequest;
import org.project.ssogssog.infrastructure.client.feign.kis.dto.KisTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * KIS 토큰 관리자
 * - Caffeine 캐시를 사용하여 토큰을 12시간 동안 캐싱
 * - 동시 요청 시 토큰 발급 중복 방지를 위한 동기화 처리
 */
@Slf4j
@Component
public class KisTokenManager {

    private static final String CACHE_KEY = "KIS_ACCESS_TOKEN";

    private final Cache<String, String> kisTokenCache;
    private final KisTokenFeignClient tokenClient;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    public KisTokenManager(Cache<String, String> kisTokenCache, KisTokenFeignClient tokenClient) {
        this.kisTokenCache = kisTokenCache;
        this.tokenClient = tokenClient;
    }

    /**
     * 유효한 토큰 반환
     * - 캐시에 토큰이 있으면 반환
     * - 없으면 새로 발급하여 캐시에 저장 후 반환
     */
    public String getValidToken() {
        String token = kisTokenCache.getIfPresent(CACHE_KEY);
        if (token != null) {
            return token;
        }

        // 동시 토큰 발급 요청 방지
        synchronized (this) {
            token = kisTokenCache.getIfPresent(CACHE_KEY);
            if (token != null) {
                return token;
            }

            String newToken = issueNewToken();
            if (newToken != null) {
                kisTokenCache.put(CACHE_KEY, newToken);
                log.info("KIS 토큰 발급 완료");
            }
            return newToken;
        }
    }

    /**
     * 토큰 무효화 (만료 시 호출)
     */
    public void invalidateToken() {
        kisTokenCache.invalidate(CACHE_KEY);
        log.info("KIS 토큰 무효화됨");
    }

    private String issueNewToken() {
        try {
            KisTokenRequest request = KisTokenRequest.clientCredentials(appKey, appSecret);
            KisTokenResponse response = tokenClient.issueToken(request);

            if (response != null && response.getAccessToken() != null) {
                return response.getAccessToken();
            }

            log.error("KIS 토큰 발급 응답이 비정상입니다: {}", response);
            return null;

        } catch (Exception e) {
            log.error("KIS 토큰 발급 실패: {}", e.getMessage(), e);
            return null;
        }
    }
}
