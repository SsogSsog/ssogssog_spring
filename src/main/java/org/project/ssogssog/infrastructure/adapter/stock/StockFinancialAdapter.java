package org.project.ssogssog.infrastructure.adapter.stock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.port.StockFinancialPort;
import org.project.ssogssog.infrastructure.client.ksi.KISClient;
import org.project.ssogssog.infrastructure.client.opendart.OpenDartClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StockFinancialAdapter implements StockFinancialPort {

    // Spring이 제공하는 ObjectMapper 주입 (JSON 파싱용)
    private final ObjectMapper objectMapper;

    private final OpenDartClient openDartClient;
    private final RateLimiter rateLimiter;

    // 주의!
    // @Qualifier 어노테이션 사용 시 생성자를 직접 만들어야 에러가 안 생긴다..
    public StockFinancialAdapter(ObjectMapper objectMapper,
                             OpenDartClient openDartClient,
                             @Qualifier("openDartRateLimiter") RateLimiter rateLimiter) {
        this.objectMapper = objectMapper;
        this.openDartClient = openDartClient;
        this.rateLimiter = rateLimiter;
    }


    @Override
    public JsonNode getFinancialInfo(String corpCode, Integer year, String reportCode) {

        try {
            rateLimiter.acquire();

            String response = openDartClient.getFinancialInfo(corpCode, year, reportCode);
            JsonNode root = objectMapper.readTree(response);
            return root;
        } catch (JsonProcessingException e) {
            log.warn("OpenDART JSON 파싱 실패 corpCode={}, year={}, reportCode={}", corpCode, year, reportCode, e);
            return null;
        }


    }
}
