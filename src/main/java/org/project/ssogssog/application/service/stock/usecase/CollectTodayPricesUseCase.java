package org.project.ssogssog.application.service.stock.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.port.DailyPricePort;
import org.project.ssogssog.application.utils.ParserUtils;
import org.project.ssogssog.application.service.stock.writer.DailyPriceWriter;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.StockRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectTodayPricesUseCase {

    private final StockRepository stockRepository;
    private final DailyPriceWriter dailyPriceWriter;

    private final DailyPricePort dailyPricePort;

    /**
     * 전 종목 시세 업데이트(당일 정보) (Batch용)
     */
    public void updateAllStockPrices() {
        List<Stock> stocks = stockRepository.findAll();

        log.info("총 {}개 종목 시세 수집 시작...", stocks.size());
        LocalDate today = LocalDate.now();

        int success = 0;
        int index = 0;

        for (Stock stock : stocks) {
            index++;

            log.info("[{} / {}] 시세 조회 시작 - 종목: {}({})",
                    index, stocks.size(), stock.getCorpName(), stock.getStockCode());

            try {
                DailyPrice dailyPrice = fetchPrice(stock, today);
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


    // --- 개별 종목 시세 조회 ---
    private DailyPrice fetchPrice(Stock stock, LocalDate date) {

        try {
            JsonNode root = dailyPricePort.getPriceRoot(stock.getStockCode());

            if(root == null){
                log.warn("root 노드가 존재하지 않습니다");
                return null;
            }

            // 에러코드 체크
            String rtCd = root.path("rt_cd").asText();
            String msgCd = root.path("msg_cd").asText();

            if ("1".equals(rtCd) && "EGW00201".equals(msgCd)) {
                log.warn("⚠️ 초당 거래건수 초과. 1초 대기 후 재시도 - 종목: {}", stock.getStockCode());
                Thread.sleep(1000); // 1초 쉬고
                // 재시도 한 번만 (무한 루프 방지)
                return retryFetchPriceOnce(stock, date);
            }

            JsonNode output = root.path("output");
            // 문자열 파싱 전 trim() 처리 & 값 확인
            if (output.isMissingNode() || output.isNull()) {
                log.warn("❌ output 노드가 없습니다. (에러 응답 가능성): {}", root.toString());
                return null;
            }

            String businessDateStr = output.path("stck_bsop_date").asText().trim();
            LocalDate actualDate;

            if (businessDateStr.isEmpty()) {
                // 만약 API가 날짜를 안 주면(그럴 리 없지만), 요청 날짜로 fallback 하거나 에러 처리
                log.warn("영업일자(stck_bsop_date)가 비어있습니다. 요청 날짜로 대체합니다. 종목: {}", stock.getStockCode());
                actualDate = date;
            } else {
                // "20240105" -> LocalDate 변환
                actualDate = LocalDate.parse(businessDateStr);
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

            int w52HighPrice = ParserUtils.parseStringToInt(output.path("w52_hgpr").asText().trim()); // 52주 최고가
            int w52LowPrice = ParserUtils.parseStringToInt(output.path("w52_lwpr").asText().trim());  // 52주 최저가
            double pbr = ParserUtils.parseStringToDouble(output.path("pbr").asText().trim());         // PBR

            return DailyPrice.builder()
                    .stock(stock)
                    .date(actualDate)
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
                    .w52HighPrice(w52HighPrice)
                    .w52LowPrice(w52LowPrice)
                    .pbr(pbr)
                    .build();

        } catch (Exception e) {
            log.error("시세 파싱 에러 - 종목: {}({}), 메시지: {}",
                    stock.getCorpName(), stock.getStockCode(), e.getMessage(), e);
            return null;
        }
    }

    // TODO queue 방식으로 재시도 로직 리팩토링 하기
    // TODO 공통 로직 private 메서드로 분리하기
    private DailyPrice retryFetchPriceOnce(Stock stock, LocalDate date) {

        try {
            JsonNode root = dailyPricePort.getPriceRoot(stock.getStockCode());
            if(root == null){
                log.warn("root 노드가 존재하지 않습니다");
                return null;
            }

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