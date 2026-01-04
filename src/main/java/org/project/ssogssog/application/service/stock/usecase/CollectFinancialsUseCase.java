package org.project.ssogssog.application.service.stock.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.utils.ParserUtils;
import org.project.ssogssog.application.service.stock.writer.StockFinancialWriter;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.project.ssogssog.domain.stock.repository.StockFinancialRepository;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.global.payload.exception.GeneralException;
import org.project.ssogssog.infrastructure.client.opendart.OpenDartClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectFinancialsUseCase {

    private final StockRepository stockRepository;
    private final StockFinancialWriter stockFinancialWriter;
    private final StockFinancialRepository stockFinancialRepository;

    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 파싱용
    private final OpenDartClient openDartClient;

    /**
     * 전 종목 재무제표 수집 및 저장
     * @param year 대상 연도 (예: 2023)
     * @param reprtCode 보고서 코드 (1분기: 11013, 반기: 11012, 3분기: 11014, 사업보고서: 11011)
     */
    public void updateAllFinancials(Integer year, String reprtCode) {
        List<Stock> stocks = stockRepository.findAll(); // 2,500개 종목 로딩
        log.info("총 {}개 종목의 {}년도 보고서({}) 수집 시작...", stocks.size(), year, reprtCode);

        int successCount = 0;
        int failCount = 0;

        for (Stock stock : stocks) {
            try {
                // 1. API 호출 및 파싱
                StockFinancial financial = fetchFinancialData(stock, year, reprtCode);

                // 2. 데이터가 있으면 저장 (API에 데이터가 없는 경우 null 반환됨)
                if (financial != null) {
                    stockFinancialWriter.saveOrUpdate(financial);
                    successCount++;
                } else {
                    // 데이터가 없는 경우 (금융업이거나, 아직 공시 안 됨 등)
                    failCount++;
                }
                log.debug("Processing stock: {}, failCount: {}", stock.getStockCode(), failCount);

                // API 호출 제한 고려 (너무 빠르면 차단될 수 있으니 0.1초 대기)
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("❌ 실패: {} ({}) - {}", stock.getCorpName(), stock.getStockCode(), e.getMessage());
                failCount++;
            }
        }
        log.info("✅ 재무제표 수집 완료. 성공: {}, 실패/없음: {}", successCount, failCount);
    }





    // --- [내부 로직] API 호출 및 DTO 변환 ---
    private StockFinancial fetchFinancialData(Stock stock, Integer year, String reportCode) {

        if (stock.getCorpCode() == null || stock.getCorpCode().isEmpty()) {
            log.debug("CorpCode 없음: {} ({})", stock.getCorpName(), stock.getStockCode());
            return null;
        }

        try {
            String response = openDartClient.getFinancialInfo(stock.getCorpCode(), year, reportCode);
            JsonNode root = objectMapper.readTree(response);

            if (!"000".equals(root.path("status").asText())) {
                return null;
            }

            JsonNode listNode = root.path("list");
            if (listNode.isMissingNode() || listNode.isEmpty()) {
                return null;
            }

            // 1) CFS 먼저 시도
            StockFinancial cfs = parseByFsDiv(stock, year, reportCode, listNode, "CFS", true);
            if (cfs != null) {
                return cfs;
            }

            // 2) CFS에서 아무 것도 못 건지면 OFS로 전체 대체
            StockFinancial ofs = parseByFsDiv(stock, year, reportCode, listNode, "OFS", false);
            if (ofs != null) {
                log.debug("CFS 없음/매핑 실패 → OFS로 저장: {} ({}) year={}, quarter={}",
                        stock.getCorpName(), stock.getStockCode(), year,
                        ParserUtils.convertReportCodeToQuarter(reportCode));
            }
            return ofs;

        } catch (Exception e) {
            log.warn("파싱 에러: {} ({})", stock.getCorpName(), stock.getStockCode());
            return null;
        }
    }

    private StockFinancial parseByFsDiv(
            Stock stock,
            Integer year,
            String reportCode,
            JsonNode listNode,
            String fsDivToUse,
            boolean isConsolidated
    ) {
        StockFinancial.StockFinancialBuilder builder = StockFinancial.builder()
                .stock(stock)
                .year(year)
                .quarter(ParserUtils.convertReportCodeToQuarter(reportCode))
                .isConsolidated(isConsolidated); // OFS면 false로 내려감

        boolean isDataFound = false;

        for (JsonNode item : listNode) {
            if (!fsDivToUse.equals(item.path("fs_div").asText())) continue;

            String accountName = item.path("account_nm").asText();
            Long amount = ParserUtils.parseAmount(item.path("thstrm_amount").asText());

            if (accountName.contains("매출액") || accountName.equals("수익(매출액)")) {
                builder.revenue(amount);
                isDataFound = true;
            } else if (accountName.contains("영업이익")) { // "영업이익(손실)" 포함
                builder.operatingProfit(amount);
                isDataFound = true;
            } else if (accountName.contains("당기순이익")) { // "당기순이익(손실)" 포함
                builder.netIncome(amount);
                isDataFound = true;
            } else if (accountName.equals("자산총계")) {
                builder.totalAssets(amount);
                //isDataFound = true;
            } else if (accountName.equals("부채총계")) {
                builder.totalLiabilities(amount);
                //isDataFound = true;
            } else if (accountName.equals("자본총계")) {
                builder.totalEquity(amount);
                isDataFound = true;
            }
        }

        return isDataFound ? builder.build() : null;
    }

}
