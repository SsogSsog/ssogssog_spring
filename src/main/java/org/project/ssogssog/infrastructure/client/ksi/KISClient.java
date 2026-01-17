package org.project.ssogssog.infrastructure.client.ksi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.usecase.dto.HistoricalPriceDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class KISClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String KEY = "KIS_ACCESS_TOKEN";
    private final Cache<String, String> kisTokenCache; // CacheConfig에 설정 등록(12시간) 넉넉히 12시간 마진으로 설정

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.base-url}")
    private String baseUrl;

    /**
     * TTL에 맞게 KIS 토큰 저장 및 필요 시 조회 메서드
     *
     * @return
     */
    public String getValidAccessToken() {
        String token = kisTokenCache.getIfPresent(KEY);
        if (token != null)
            return token;

        // 토큰 만료 시 KIS api 동시 요청을 막기 위한 락
        synchronized (this) {
            // 이 전 synchronized 내부에 들어간 스레드가 KIS 토큰을 가져올 수 있으므로 한 번 더 검사
            token = kisTokenCache.getIfPresent(KEY);
            if (token != null)
                return token;

            String issued = this.getAccessToken();
            if (issued == null) {
                log.warn("KIS access token 발급 실패...");
                return null;
            }

            kisTokenCache.put(KEY, issued);
            return issued;
        }
    }

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

    public JsonNode getPriceRoot(String token, String stockCode)  {
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


        try {
            return objectMapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            log.error("KIS API 호출 중 JsonNode 파싱 에러 (종목코드: {}): {}", stockCode, e.getMessage());
            return null;
        }
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
            log.warn("KIS API 호출 중 KisPriceResponse 파싱 에러 발생 (종목코드: {}): {}", stockCode, e.getMessage());
        }

        return null;
    }

    // TODO: application DTO를 값을 가져올 때 사용하는 건 리팩토링이 필요해보임
    public HistoricalPriceDTO fetchPastPrices(String stockCode, String accessToken, String strStartDate, String strEndDate) {

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
            ResponseEntity<HistoricalPriceDTO> response = restTemplate.exchange(
                    uri, HttpMethod.GET, requestEntity, HistoricalPriceDTO.class
            );

            HistoricalPriceDTO body = response.getBody();
            return body;
        }catch (Exception e) {
            log.error("KIS API 호출 중 과거 시세 수집 실패 [{}]: {}", stockCode, e.getMessage());
            return null;
        }
    }

    public boolean isMarketOpen(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 1. KIS 휴장일 조회 API 호출
        // WebClient나 RestTemplate 사용 (기존에 쓰시던 방식 그대로)
        KisHolidayResponse response = this.checkHoliday(dateStr);

        if (response == null || response.getOutput() == null || response.getOutput().isEmpty()) {
            log.error("휴장일 정보를 가져오지 못했습니다. 보수적으로 '휴장'으로 처리합니다.");
            return false;
        }

        // 2. 결과 확인
        // 보통 요청한 날짜가 리스트의 첫 번째 혹은 해당 날짜로 옴
        KisHolidayResponse.HolidayInfo info = response.getOutput().stream()
                .filter(i -> i.getBaseDate().equals(dateStr))
                .findFirst()
                .orElse(response.getOutput().get(0));

        log.info("날짜: {}, 요일: {}, 개장여부: {}", info.getBaseDate(), info.getDayName(), info.getOpenYn());

        return "Y".equals(info.getOpenYn()); // Y이면 true(개장), N이면 false(휴장)
    }


    private KisHolidayResponse checkHoliday(String dateStr) {
        String accessToken = this.getValidAccessToken(); // 토큰 가져오기

        // 1. 헤더 설정 (필수값들)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "CTCA0903R"); // ★ 중요: 휴장일 조회용 ID
        headers.set("tr_cont", "");        // 연속 거래 여부 (없음)
        headers.set("custtype", "P");      // 개인

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 2. URL 및 파라미터 생성
        // 결과 예시: https://.../chk-holiday?BASS_DT=20260110&CTX_AREA_NK=&CTX_AREA_FK=
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/uapi/domestic-stock/v1/quotations/chk-holiday")
                .queryParam("BASS_DT", dateStr) // 기준일자 (YYYYMMDD)
                .queryParam("CTX_AREA_NK", "")  // 필수지만 빈 값
                .queryParam("CTX_AREA_FK", "")  // 필수지만 빈 값
                .build()
                .toUri();

        try {
            log.info("휴장일 확인 요청 URI: {}", uri);

            // 3. API 호출 (GET)
            ResponseEntity<KisHolidayResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    KisHolidayResponse.class
            );

            return response.getBody();

        } catch (Exception e) {
            log.error("❌ KIS 휴장일 조회 API 호출 실패: {}", e.getMessage());
            return null; // 실패 시 null 반환 (호출부에서 처리)
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


    @Getter
    @NoArgsConstructor
    @ToString
    public static class KisHolidayResponse {

        @JsonProperty("ctx_area_nk")
        private String ctxAreaNk;

        @JsonProperty("ctx_area_fk")
        private String ctxAreaFk;

        @JsonProperty("output")
        private List<HolidayInfo> output;

        @Getter
        @NoArgsConstructor
        @ToString
        public static class HolidayInfo {
            @JsonProperty("bass_dt")
            private String baseDate; // 기준일자 (20260110)

            @JsonProperty("opnd_yn")
            private String openYn;   // Y:개장, N:휴장

            @JsonProperty("wday_dvsn_cd_name")
            private String dayName;  // 요일명 (토요일)
        }
    }
}
