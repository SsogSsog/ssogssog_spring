package org.project.ssogssog.infrastructure.adapter.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.util.concurrent.RateLimiter;

import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.port.DailyPricePort;
import org.project.ssogssog.application.service.stock.usecase.dto.HistoricalPriceDTO;
import org.project.ssogssog.infrastructure.client.ksi.KISClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DailyPriceAdapter implements DailyPricePort {

    private final KISClient kisClient;

    private final RateLimiter rateLimiter;

    // 주의!
    // @Qualifier 어노테이션 사용 시 생성자를 직접 만들어야 에러가 안 생긴다..
    public DailyPriceAdapter(KISClient kisClient,
            Cache<String, String> kisTokenCache,
            @Qualifier("kisRateLimiter") RateLimiter rateLimiter) {
        this.kisClient = kisClient;
        this.rateLimiter = rateLimiter;
    }

    // 현재 문제점
    // 1. 중간에 log.warn으로 실패했을 때 재시도 로직이 필요(캐시 큐에 저장 후 스케쥬럴에서 누락된 로직 재실행)
    // 2. 비동기로 했을 때 성능이 과연 올라갈지..?
    // 3. 해당 토큰을 가지고 간 후 다른 메서드에서 사용될 때 토큰 시간이 만료되면 어쩌지...
    // 4. 중복 요청, 토큰 대기 시간 등 에러가 뜰 때 방어 로직이 필요하다 -> AOP로 설계??
    // 5. 꼭 넣어야 하는 방어 로직 -> 토큰 발급 에러의 경우 1분 이상 대기하도록 락 걸어 놓기

    @Override
    public JsonNode getPriceRoot(String stockCode) {

        String accessToken = kisClient.getValidAccessToken();

        // KIS access token 발급 실패
        if (accessToken == null) {
            return null;
        }

        rateLimiter.acquire();
        return kisClient.getPriceRoot(accessToken, stockCode);

    }


    @Override
    public HistoricalPriceDTO fetchPastPrices(String stockCode, String strStartDate, String strEndDate) {
        String accessToken = kisClient.getValidAccessToken();

        // KIS access token 발급 실패
        if (accessToken == null) {
            return null;
        }

        rateLimiter.acquire();
        return kisClient.fetchPastPrices(accessToken, stockCode, strStartDate, strEndDate);

    }


}
