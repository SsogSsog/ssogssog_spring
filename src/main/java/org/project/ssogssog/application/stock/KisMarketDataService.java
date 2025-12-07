package org.project.ssogssog.application.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.RateLimiter; // Guava 라이브러리
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.common.utils.ParserUtils;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.infrastructure.ksi.DailyPriceWriter;
import org.project.ssogssog.infrastructure.ksi.KSIClient;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisMarketDataService {

    private final StockRepository stockRepository;
    private final DailyPriceWriter dailyPriceWriter;

    // 1초에 10개 요청 제한 (KIS 제한: 초당 20건, 안전마진 확보)
    private final RateLimiter rateLimiter = RateLimiter.create(10.0);

    private final KSIClient ksiClient;

    /**
     * 전 종목 시세 업데이트 (Batch용)
     */
    public void updateAllStockPrices() {
        // 1. 토큰 발급 (루프 시작 전 1회)
        String accessToken = ksiClient.getAccessToken();
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
                DailyPrice dailyPrice = fetchPrice(accessToken, stock, today);
                log.info("[{} / {}] 시세 조회 완료 - 종목: {}({})",
                        index, stocks.size(), stock.getCorpName(), stock.getStockCode());

                if (dailyPrice != null) {
                    dailyPriceWriter.saveDailyPrice(dailyPrice);
                    success++;
                }
            } catch (Exception e) {
                log.error("[{} / {}] 시세 수집 실패 - 종목: {}({}), 메시지: {}",
                        index, stocks.size(), stock.getCorpName(), stock.getStockCode(), e.getMessage(), e);
            }
        }
        log.info("✅ 시세 업데이트 완료. 성공: {}/{}", success, stocks.size());
    }


    // --- [내부 2] 개별 종목 시세 조회 ---
    private DailyPrice fetchPrice(String token, Stock stock, LocalDate date) {

        try {
            JsonNode root = ksiClient.getPriceRoot(token, stock.getStockCode());

            // 에러코드 체크
            String rtCd = root.path("rt_cd").asText();
            String msgCd = root.path("msg_cd").asText();

            if ("1".equals(rtCd) && "EGW00201".equals(msgCd)) {
                log.warn("⚠️ 초당 거래건수 초과. 1초 대기 후 재시도 - 종목: {}", stock.getStockCode());
                Thread.sleep(1000); // 1초 쉬고
                // 재시도 한 번만 (무한 루프 방지)
                return retryFetchPriceOnce(token, stock, date);
            }

            JsonNode output = root.path("output");
            // 문자열 파싱 전 trim() 처리 & 값 확인
            if (output.isMissingNode() || output.isNull()) {
                log.warn("❌ output 노드가 없습니다. (에러 응답 가능성): {}", root.toString());
                return null;
            }


            // "stck_prpr"가 현재가가 아닐 수도 있으니 로그 확인 필요
            String closeStr = output.path("stck_prpr").asText().trim();
            String openStr = output.path("stck_oprc").asText().trim();

            // 값이 비어있거나 0이면 저장하지 않도록 방어 로직 (선택 사항)
            if (closeStr.isEmpty() || closeStr.equals("0")) {
                log.warn("⚠️ 가격 정보가 0입니다. 종목: {}", stock.getCorpName());
                // return null; // 0인 데이터는 저장하기 싫으면 여기서 리턴
            }

            int closePrice = ParserUtils.parseStringToInt(closeStr);
            int openPrice = ParserUtils.parseStringToInt(openStr);
            int highPrice = ParserUtils.parseStringToInt(output.path("stck_hgpr").asText().trim());
            int lowPrice = ParserUtils.parseStringToInt(output.path("stck_lwpr").asText().trim());
            long volume = ParserUtils.parseStringToLong(output.path("acml_vol").asText().trim());
            long marketCap = ParserUtils.parseStringToLong(output.path("hts_avls").asText().trim()); // 억 단위로 환산됨

            long listedShares = ParserUtils.parseStringToLong(output.path("lstn_stcn").asText().trim());
            long foreignHeldShares = ParserUtils.parseStringToLong(output.path("frgn_hldn_qty").asText().trim());
            int changePrice = ParserUtils.parseStringToInt(output.path("prdy_vrss").asText().trim());
            double changeRate = ParserUtils.parseStringToDouble(output.path("prdy_ctrt").asText().trim());
            int prevClosePrice = ParserUtils.parseStringToInt(output.path("stck_sdpr").asText().trim());

            return DailyPrice.builder()
                    .stock(stock)
                    .date(date)
                    .closePrice(closePrice)
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

//    public void testSingleStock() {
//        String accessToken = ksiClient.getAccessToken();
//        LocalDate today = LocalDate.now();
//
//        Stock samsung = Stock.builder()
//                .stockCode("005930")
//                .corpName("삼성전자")
//                .build();
//
//        log.info(">>> 삼성전자 단건 테스트 시작");
//        DailyPrice result = fetchPrice(accessToken, samsung, today);
//
//        if (result != null) {
//            log.info(">>> 파싱 결과: 종목={}, 코드={}, 종가={}, 시가={}, 고가={}, 저가={}, 거래량={}, 시가총액={}",
//                    samsung.getCorpName(), samsung.getStockCode(),
//                    result.getClosePrice(), result.getOpenPrice(), result.getHighPrice(),
//                    result.getLowPrice(), result.getVolume(), result.getMarketCap());
//        } else {
//            log.error(">>> 삼성전자조차 null이 반환됨. API 호출 설정 문제임.");
//        }
//    }


    private DailyPrice retryFetchPriceOnce(String token, Stock stock, LocalDate date) {
        rateLimiter.acquire(); // 재시도도 RateLimiter 적용

        try {
            JsonNode root = ksiClient.getPriceRoot(token, stock.getStockCode());
            JsonNode output = root.path("output");

            // 문자열 파싱 전 trim() 처리 & 값 확인
            if (output.isMissingNode() || output.isNull()) {
                log.warn("❌ output 노드가 없습니다. (에러 응답 가능성): {}", root.toString());
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

            int closePrice = ParserUtils.parseStringToInt(closeStr);
            int openPrice = ParserUtils.parseStringToInt(openStr);
            int highPrice = ParserUtils.parseStringToInt(output.path("stck_hgpr").asText().trim());
            int lowPrice = ParserUtils.parseStringToInt(output.path("stck_lwpr").asText().trim());
            long volume = ParserUtils.parseStringToLong(output.path("acml_vol").asText().trim());
            long marketCap = ParserUtils.parseStringToLong(output.path("hts_avls").asText().trim()); // 억 단위로 환산됨

            long listedShares = ParserUtils.parseStringToLong(output.path("lstn_stcn").asText().trim());
            long foreignHeldShares = ParserUtils.parseStringToLong(output.path("frgn_hldn_qty").asText().trim());
            int changePrice = ParserUtils.parseStringToInt(output.path("prdy_vrss").asText().trim());
            double changeRate = ParserUtils.parseStringToDouble(output.path("prdy_ctrt").asText().trim());
            int prevClosePrice = ParserUtils.parseStringToInt(output.path("stck_sdpr").asText().trim());

            return DailyPrice.builder()
                    .stock(stock)
                    .date(date)
                    .closePrice(closePrice)
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