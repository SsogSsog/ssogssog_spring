package org.project.ssogssog.infrastructure.client.feign.kis.auth;

import org.project.ssogssog.infrastructure.client.feign.kis.dto.KisTokenRequest;
import org.project.ssogssog.infrastructure.client.feign.kis.dto.KisTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * KIS 토큰 발급 전용 Feign Client
 * - KisFeignClient와 분리하여 순환 참조 방지
 * - RequestInterceptor가 적용되지 않음 (토큰 발급에는 토큰이 필요 없음)
 */
@FeignClient(
        name = "kis-token-client",
        url = "${kis.base-url}",
        configuration = KisTokenFeignConfig.class
)
public interface KisTokenFeignClient {

    @PostMapping("/oauth2/tokenP")
    KisTokenResponse issueToken(@RequestBody KisTokenRequest request);
}
