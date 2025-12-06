package org.project.ssogssog.application.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter; // Guava 라이브러리
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.common.utils.Parser;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.DailyPriceRepository;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisMarketDataService {

    private final StockRepository stockRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    // 1초에 15개 요청 제한 (KIS 제한: 초당 20건, 안전마진 확보)
    private final RateLimiter rateLimiter = RateLimiter.create(10.0);

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;


    @Value("${kis.base-url}")
    private String baseUrl;

    /**
     * 전 종목 시세 업데이트 (Batch용)
     */
    public void updateAllStockPrices() {
        // 1. 토큰 발급 (루프 시작 전 1회)
        String accessToken = getAccessToken();
        if (accessToken == null) {
            log.error("❌ 토큰 발급 실패로 작업을 중단합니다.");
            return;
        }

        List<Stock> stocks = stockRepository.findAll();
        // 테스트용: stocks = stocks.subList(0, 10);

        log.info("총 {}개 종목 시세 수집 시작...", stocks.size());
        LocalDate today = LocalDate.now();

        int success = 0;
        int index = 0;

        for (Stock stock : stocks) {
            index++;
            rateLimiter.acquire();

            log.info("[{} / {}] 시세 조회 시작 - 종목: {}({})",
                    index, stocks.size(), stock.getCorpName(), stock.getStockCode());

            try {
                DailyPrice dailyPrice = fetchPrice(restTemplate, accessToken, stock, today);
                log.info("[{} / {}] 시세 조회 완료 - 종목: {}({})",
                        index, stocks.size(), stock.getCorpName(), stock.getStockCode());

                if (dailyPrice != null) {
                    saveDailyPrice(dailyPrice);
                    success++;
                }
            } catch (Exception e) {
                log.error("[{} / {}] 시세 수집 실패 - 종목: {}({}), 메시지: {}",
                        index, stocks.size(), stock.getCorpName(), stock.getStockCode(), e.getMessage(), e);
            }
        }
        log.info("✅ 시세 업데이트 완료. 성공: {}/{}", success, stocks.size());
    }

    // --- 내부 1: 토큰 발급 ---
    private String getAccessToken() {
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

    // --- [내부 2] 개별 종목 시세 조회 ---
    private DailyPrice fetchPrice(RestTemplate restTemplate, String token, Stock stock, LocalDate date) {
        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price";

        URI uri = UriComponentsBuilder.fromUriString(url)
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                .queryParam("FID_INPUT_ISCD", stock.getStockCode())
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "FHKST01010100"); // 주식현재가 시세 TR ID

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());

            // 에러코드 체크
            String rtCd = root.path("rt_cd").asText();
            String msgCd = root.path("msg_cd").asText();

            if ("1".equals(rtCd) && "EGW00201".equals(msgCd)) {
                log.warn("⚠️ 초당 거래건수 초과. 1초 대기 후 재시도 - 종목: {}", stock.getStockCode());
                Thread.sleep(1000); // 1초 쉬고
                // 재시도 한 번만 (무한 루프 방지)
                return retryFetchPriceOnce(restTemplate, token, stock, date);
            }

            // [중요 1] API가 준 원본 JSON을 눈으로 확인해야 함!
            log.info("API Response for {}: {}", stock.getCorpName(), response.getBody());

            JsonNode output = objectMapper.readTree(response.getBody()).path("output");

            // [중요 2] output 자체가 비어있는지 체크
            if (output.isMissingNode() || output.isNull()) {
                log.warn("❌ output 노드가 없습니다. (에러 응답 가능성): {}", response.getBody());
                return null;
            }

            // [중요 3] 문자열 파싱 전 trim() 처리 & 값 확인
            // "stck_prpr"가 현재가가 아닐 수도 있으니 로그 확인 필요
            String closeStr = output.path("stck_prpr").asText().trim();
            String openStr = output.path("stck_oprc").asText().trim();

            // 값이 비어있거나 0이면 저장하지 않도록 방어 로직 (선택 사항)
            if (closeStr.isEmpty() || closeStr.equals("0")) {
                log.warn("⚠️ 가격 정보가 0입니다. 종목: {}", stock.getCorpName());
                // return null; // 0인 데이터는 저장하기 싫으면 여기서 리턴
            }

            int closePrice = Parser.parserStringToInt(closeStr);
            int openPrice = Parser.parserStringToInt(openStr);
            int highPrice = Parser.parserStringToInt(output.path("stck_hgpr").asText().trim());
            int lowPrice = Parser.parserStringToInt(output.path("stck_lwpr").asText().trim());
            long volume = Parser.parserStringToLong(output.path("acml_vol").asText().trim());
            long marketCap = Parser.parserStringToLong(output.path("hts_avls").asText().trim()); // 억 단위로 환산됨

            long listedShares     = Parser.parserStringToLong(output.path("lstn_stcn").asText().trim());
            long foreignHeldShares = Parser.parserStringToLong(output.path("frgn_hldn_qty").asText().trim());
            int changePrice = Parser.parserStringToInt(output.path("prdy_vrss").asText().trim());
            double changeRate = Parser.parserStringToDouble(output.path("prdy_ctrt").asText().trim());
            int prevClosePrice = Parser.parserStringToInt(output.path("stck_sdpr").asText().trim());

            return DailyPrice.builder()
                    .stock(stock)
                    .date(date)
                    .closePrice(closePrice) // Entity 필드명과 DB 컬럼 매핑이 잘 되었는지도 확인
                    .openPrice(openPrice)
                    .highPrice(highPrice)
                    .lowPrice(lowPrice)
                    .volume(volume)
                    .marketCap(marketCap)
                    .listedShares(listedShares)
                    .foreignHeldShares(foreignHeldShares)
                    .changePrice(changePrice)
                    .changeRate(changeRate)
                    .prevClosePrice(prevClosePrice)
                    .build();

        } catch (Exception e) {
            log.error("시세 파싱 에러 - 종목: {}({}), 메시지: {}",
                    stock.getCorpName(), stock.getStockCode(), e.getMessage(), e);
            return null;
        }
    }

    // --- [내부 3] 저장 (중복 방지) ---
    protected void saveDailyPrice(DailyPrice newPrice) {
        Optional<DailyPrice> existing = dailyPriceRepository.findByStockIdAndDate(
                newPrice.getStock().getId(), newPrice.getDate());

        if (existing.isEmpty()) {
            dailyPriceRepository.save(newPrice);
        } else {
            // 이미 있으면 업데이트 로직 (필요시 구현)
        }
    }

    public void testSingleStock() {
        String accessToken = getAccessToken();
        LocalDate today = LocalDate.now();

        Stock samsung = Stock.builder()
                .stockCode("002410")
                .corpName("삼성전자")
                .build();

        log.info(">>> 삼성전자 단건 테스트 시작");
        DailyPrice result = fetchPrice(restTemplate, accessToken, samsung, today);

        if (result != null) {
            log.info(">>> 파싱 결과: 종목={}, 코드={}, 종가={}, 시가={}, 고가={}, 저가={}, 거래량={}, 시가총액={}",
                    samsung.getCorpName(), samsung.getStockCode(),
                    result.getClosePrice(), result.getOpenPrice(), result.getHighPrice(),
                    result.getLowPrice(), result.getVolume(), result.getMarketCap());
        } else {
            log.error(">>> 삼성전자조차 null이 반환됨. API 호출 설정 문제임.");
        }
    }


    private DailyPrice retryFetchPriceOnce(RestTemplate restTemplate, String token, Stock stock, LocalDate date)
            throws InterruptedException {
        rateLimiter.acquire(); // 재시도도 RateLimiter 적용

        String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price";

        URI uri = UriComponentsBuilder.fromUriString(url)
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                .queryParam("FID_INPUT_ISCD", stock.getStockCode())
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "FHKST01010100"); // 주식현재가 시세 TR ID

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            // [중요 1] API가 준 원본 JSON을 눈으로 확인해야 함!
            log.info("API Response for {}: {}", stock.getCorpName(), response.getBody());

            JsonNode output = objectMapper.readTree(response.getBody()).path("output");

            // [중요 2] output 자체가 비어있는지 체크
            if (output.isMissingNode() || output.isNull()) {
                log.warn("❌ output 노드가 없습니다. (에러 응답 가능성): {}", response.getBody());
                return null;
            }

            // [중요 3] 문자열 파싱 전 trim() 처리 & 값 확인
            // "stck_prpr"가 현재가가 아닐 수도 있으니 로그 확인 필요
            String closeStr = output.path("stck_prpr").asText().trim();
            String openStr = output.path("stck_oprc").asText().trim();

            // 값이 비어있거나 0이면 저장하지 않도록 방어 로직 (선택 사항)
            if (closeStr.isEmpty() || closeStr.equals("0")) {
                log.warn("⚠️ 가격 정보가 0입니다. 종목: {}", stock.getCorpName());
                // return null; // 0인 데이터는 저장하기 싫으면 여기서 리턴
            }

            int closePrice = Parser.parserStringToInt(closeStr);
            int openPrice = Parser.parserStringToInt(openStr);
            int highPrice = Parser.parserStringToInt(output.path("stck_hgpr").asText().trim());
            int lowPrice = Parser.parserStringToInt(output.path("stck_lwpr").asText().trim());
            long volume = Parser.parserStringToLong(output.path("acml_vol").asText().trim());
            long marketCap = Parser.parserStringToLong(output.path("hts_avls").asText().trim()); // 억 단위로 환산됨

            long listedShares     = Parser.parserStringToLong(output.path("lstn_stcn").asText().trim());
            long foreignHeldShares = Parser.parserStringToLong(output.path("frgn_hldn_qty").asText().trim());
            int changePrice = Parser.parserStringToInt(output.path("prdy_vrss").asText().trim());
            double changeRate = Parser.parserStringToDouble(output.path("prdy_ctrt").asText().trim());
            int prevClosePrice = Parser.parserStringToInt(output.path("stck_sdpr").asText().trim());

            return DailyPrice.builder()
                    .stock(stock)
                    .date(date)
                    .closePrice(closePrice) // Entity 필드명과 DB 컬럼 매핑이 잘 되었는지도 확인
                    .openPrice(openPrice)
                    .highPrice(highPrice)
                    .lowPrice(lowPrice)
                    .volume(volume)
                    .marketCap(marketCap)
                    .listedShares(listedShares)
                    .foreignHeldShares(foreignHeldShares)
                    .changePrice(changePrice)
                    .changeRate(changeRate)
                    .prevClosePrice(prevClosePrice)
                    .build();

        } catch (Exception e) {
            log.error("시세 파싱 에러 - 종목: {}({}), 메시지: {}",
                    stock.getCorpName(), stock.getStockCode(), e.getMessage(), e);
            return null;
        }

    }
}