package org.project.ssogssog.application.service.stock.collect;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.RateLimiter; // Guava 라이브러리
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.common.utils.ParserUtils;
import org.project.ssogssog.application.service.stock.collect.dto.KisHistoricalPriceResponse;
import org.project.ssogssog.application.service.stock.writer.DailyPriceWriter;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.infrastructure.client.ksi.KSIClient;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
     * 전 종목 시세 업데이트(당일 정보) (Batch용)
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

    /**
     * 특정 종목의 과거 N개월치 데이터를 가져와 DB에 저장
     * @param stockCode 종목코드
     * @param months 몇 개월 전부터 가져올지 (예: 6)
     */
    public void fetchAndSavePastPrices(String stockCode, int months) {
        // 0. 토큰 확보
        String accessToken = ksiClient.getAccessToken(); // 기존에 만드신 메소드 활용

        // KIS API가 한 번에 100건 밖에 데이터를 가져올 수 없으므로 3개월씩 쪼개서 가져오는 로직으로 변경
        // 전체 목표 기간: 오늘 ~ N개월 전
        final LocalDate finalEndDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
        final LocalDate finalStartDate = finalEndDate.minusMonths(months);

        log.info("[{}] 과거 {}개월 데이터 수집 시작 (목표: {} ~ {})",
                stockCode, months, finalStartDate, finalEndDate);

        // 3개월 마다 자를 기간 중 마지막 기간을 나타내는 변수 지정
        LocalDate currentEnd = finalEndDate;

        // 루프: 현재 종료일이 최종 시작일 보다 미래인 동안 계속 실행
        while(currentEnd.isAfter(finalStartDate)) {
            // 1. 이번 요청의 시작일 계산
            LocalDate currentStart = currentEnd.minusMonths(3);

            // 만약 3개월 전이 최종 목표보다 더 과거라면, currentStart를 최종 목표일로 맞춤
            if(currentStart.isBefore(finalStartDate)) {
                currentStart = finalStartDate;
            }

            // 날짜 포맷팅 (YYYYMMDD)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String strStartDate = currentStart.format(formatter);
            String strEndDate = currentEnd.format(formatter);

            log.info(" >>> API 요청 구간: {} ~ {}", strStartDate, strEndDate);

            // 1초에 10번만 통과 가능하므로, 요청이 몰리면 여기서 자동으로 대기(Block)합니다.
            rateLimiter.acquire();

            // Thread.sleep 보다 Guava의 rateLimiter가 안전한 이유
            // 전역 통제: 스레드가 1개든 10개든 rateLimiter 인스턴스 하나를 공유한다면
            // (Spring Bean은 기본 싱글톤이므로 공유됨), 전체 합쳐서 초당 10회 절대 안 넘는다.

            // 유연함: API 응답이 빨라지면 RateLimiter도 그에 맞춰 바로 다음 요청을 보내고,
            // 느리면 알아서 기다줌. 따라서 sleep(100)처럼 무조건 기다리는 낭비 시간이 사라진다.

            // 2. KIS API 호출 (국내주식 기간별 시세)
            KisHistoricalPriceResponse kisHistoricalPriceResponse = ksiClient.fetchPastPrices(stockCode, accessToken, strStartDate, strEndDate);

            // 3. DB 저장
            if (kisHistoricalPriceResponse != null && kisHistoricalPriceResponse.getDailyItems() != null) {
                dailyPriceWriter.saveHistoricalPrices(stockCode, kisHistoricalPriceResponse.getDailyItems());
            }

            // 4. 종료일을 '이번 시작일의 하루 전'으로 설정
            currentEnd = currentStart.minusDays(1);

            log.info("[{}] 데이터 수집 완료!", stockCode);

        }


    }

}