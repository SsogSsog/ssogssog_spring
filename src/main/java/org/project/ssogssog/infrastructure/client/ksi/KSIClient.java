package org.project.ssogssog.infrastructure.client.ksi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.usecase.dto.HistoricalPriceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class KSIClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.base-url}")
    private String baseUrl;

    // 토큰 발급
    public String getAccessToken() {
        try {
            String url = baseUrl + "/oauth2/tokenP";

            Map<String, String> body = new HashMap<>();
            body.put("grant_type", "client_credentials");
            body.put("appkey", appKey);
            body.put("appsecret", appSecret);

            ResponseEntity<String> response = restTemplate.postForEntity(url, body, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("access_token").asText();
        } catch (Exception e) {
            log.error("토큰 발급 에러", e);
            return null;
        }
    }

    public JsonNode getPriceRoot(String token, String stockCode) throws JsonProcessingException {
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price";
        URI uri = UriComponentsBuilder.fromUriString(url)
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                .queryParam("FID_INPUT_ISCD", stockCode)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "FHKST01010100");

        ResponseEntity<String> response = restTemplate.exchange(
                uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("KIS API 호출 실패 - 종목: {}, 상태: {}", stockCode, response.getStatusCode());
            throw new RuntimeException("KIS API 호출 실패: " + response.getStatusCode());
        }

        log.debug("API 호출 성공 - 종목: {}", stockCode);


        return objectMapper.readTree(response.getBody());
    }

    //TODO 같은 엔드포인트의 값을 하나는 JsonNode 기반, 다른 하나는 DTO 기반으로 값을 가져오므로 통일성을 맞출 필요가 있어보임
    public String fetchSector(String stockCode, String accessToken) {
        // API 경로: 국내주식시세 > 주식현재가 시세
        String path = "/uapi/domestic-stock/v1/quotations/inquire-price";

        // 1. URI 생성
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(path)
                .queryParam("FID_COND_MRKT_DIV_CODE", "J") // J: 주식, ETF 등
                .queryParam("FID_INPUT_ISCD", stockCode)   // 종목코드
                .build()
                .toUri();

        // 2. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "FHKST01010100"); // 거래 ID (주식현재가)

        // 3. 요청 엔티티 생성 (Body는 없으므로 null, Header만 포함)
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            // 4. API 호출 (GET 방식)
            ResponseEntity<KisPriceResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    requestEntity,
                    KisPriceResponse.class
            );

            // 5. 응답 파싱
            if (response.getBody() != null && response.getBody().getOutput() != null) {
                return response.getBody().getOutput().getSectorName();
            }

        } catch (Exception e) {
            log.warn("KIS API 호출 중 오류 발생 (종목코드: {}): {}", stockCode, e.getMessage());
        }

        return null;
    }

    public HistoricalPriceResponse fetchPastPrices(String stockCode, String accessToken, String strStartDate, String strEndDate) {

        // KIS API 호출 (국내주식 기간별 시세)
        final String path = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(path)
                .queryParam("FID_COND_MRKT_DIV_CODE", "J") // J: 주식
                .queryParam("FID_INPUT_ISCD", stockCode)   // 종목코드
                .queryParam("FID_INPUT_DATE_1", strStartDate) // 시작일
                .queryParam("FID_INPUT_DATE_2", strEndDate)   // 종료일
                .queryParam("FID_PERIOD_DIV_CODE", "D")    // D: 일봉
                .queryParam("FID_ORG_ADJ_PRC", "1")        // 1: 수정주가 (중요! 액면분할 반영)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "FHKST03010100"); // [중요] 기간별 시세 조회 TR ID

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<HistoricalPriceResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, requestEntity, HistoricalPriceResponse.class
            );

            HistoricalPriceResponse body = response.getBody();
            return body;
        }catch (Exception e) {
            log.error("과거 시세 수집 실패 [{}]: {}", stockCode, e.getMessage());
            return null;
        }
    }

    // --- 내부 DTO 클래스 (응답 매핑용) ---
    @Data
    static class KisPriceResponse {
        @JsonProperty("output")
        private Output output;

        @JsonProperty("rt_cd")
        private String returnCode;
    }

    @Data
    static class Output {
        // KIS API 응답 필드: bstp_kor_isnm (업종 한글명)
        @JsonProperty("bstp_kor_isnm")
        private String sectorName;
    }



}
