package org.project.ssogssog.application.service.stock.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.port.StockFinancialPort;
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

    private final StockFinancialPort stockFinancialPort;

    // 1초에 10개 요청 제한
    private final RateLimiter rateLimiter = RateLimiter.create(10.0);

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

    /**
     * (year, reprtCode=quarter) 기준으로 DB에 없는 종목만 재무제표 재수집
     */
    public void refillMissingFinancials(Integer year, String reprtCode) {
        String quarter = ParserUtils.convertReportCodeToQuarter(reprtCode);

        List<Stock> stocks = stockRepository.findAll();
        Set<Long> existingStockIds = new HashSet<>(
                stockFinancialRepository.findStockIdsByYearAndQuarter(year, quarter)
        );

        // corpCode 없는 종목은 애초에 호출 불가라 제외(원하면 별도 실패로 집계 가능)
        List<Stock> targets = stocks.stream()
                .filter(s -> s.getCorpCode() != null && !s.getCorpCode().isBlank())
                .filter(s -> !existingStockIds.contains(s.getId()))
                .collect(Collectors.toList());

        log.info("[재무 누락 재수집] year={}, quarter={}, DB기존={}, 전체종목={}, 누락대상={}",
                year, quarter, existingStockIds.size(), stocks.size(), targets.size());

        int success = 0;
        int fail = 0;

        // 실패 종목 기록(너무 길어질 수 있으니 마지막에 일부만 출력)
        List<String> failedStocks = new ArrayList<>();

        for (Stock stock : targets) {
            try {
                //요청이 몰리지 않게 여기서 자동으로 대기(Block)합니다.
                rateLimiter.acquire();

                StockFinancial financial = fetchFinancialData(stock, year, reprtCode);

                if (financial != null) {
                    stockFinancialWriter.saveOrUpdate(financial);
                    log.info("수집 성공 : {} ({}) year={}, quarter={}",
                            stock.getCorpName(), stock.getStockCode(), year, quarter);
                    success++;
                } else {
                    fail++;
                    failedStocks.add(stock.getStockCode() + " (" + stock.getCorpName() + ") - NO_DATA");
                    log.warn("❌ 재무 없음: {} ({}) year={}, quarter={}",
                            stock.getCorpName(), stock.getStockCode(), year, quarter);
                }


            } catch (Exception e) {
                fail++;
                failedStocks.add(stock.getStockCode() + " (" + stock.getCorpName() + ") - " + e.getClass().getSimpleName());
                log.error("❌ 재수집 실패: {} ({}) year={}, quarter={}, err={}",
                        stock.getCorpName(), stock.getStockCode(), year, quarter, e.getMessage());
            }
        }

        // 수집한 년도, 분기 및 성공 개수, 실패 개수 출력
        log.info("[재무 누락 재수집 완료] year={}, quarter={}, 성공={}, 실패={}", year, quarter, success, fail);

        // 실패한 목록 출력
        if (!failedStocks.isEmpty()) {
            int show = Math.min(50, failedStocks.size());
            log.warn("[재무 누락 재수집 실패 목록] ({}개 중 {}개만 표시) {}",
                    failedStocks.size(),
                    show,
                    String.join(", ", failedStocks.subList(0, show))
            );
        }
    }

    /**
     * 특정 종목의 재무재표 세부 정보를 출력하는 디버깅 메서드
     * @param stockId
     * @param year
     * @param reportCode
     */
    public void debugOpenDartAccountNames(Long stockId, Integer year, String reportCode) {
        String quarter = ParserUtils.convertReportCodeToQuarter(reportCode);

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));

        if (stock.getCorpCode() == null || stock.getCorpCode().isBlank()) {
            log.warn("[DART 디버그] corpCode 없음: {} ({})", stock.getCorpName(), stock.getStockCode());
            return;
        }

        JsonNode root = stockFinancialPort.getFinancialInfo(stock.getCorpCode(), year, reportCode);
        if(root == null){
            // 로깅은 port에서 처리 했음
            return;
        }

        try {
            String status = root.path("status").asText();
            String message = root.path("message").asText();

            if (!"000".equals(status)) {
                log.warn("[DART 디버그] status!=000: {} ({}) year={}, quarter={}, reprtCode={}, status={}, msg={}",
                        stock.getCorpName(), stock.getStockCode(), year, quarter, reportCode, status, message);
                return;
            }

            JsonNode listNode = root.path("list");
            if (listNode.isMissingNode() || listNode.isEmpty()) {
                log.warn("[DART 디버그] list 비어있음: {} ({}) year={}, quarter={}, reprtCode={}",
                        stock.getCorpName(), stock.getStockCode(), year, quarter, reportCode);
                return;
            }

            int totalRows = listNode.size();
            log.info("[DART 디버그] {} ({}) year={}, quarter={}, reprtCode={}, listRows={}",
                    stock.getCorpName(), stock.getStockCode(), year, quarter, reportCode, totalRows);

            // fs_div(CFS/OFS) + sj_div(재무상태표/손익계산서 등) 단위로 account_nm 모아보기
            Map<String, Set<String>> accountNamesByGroup = new LinkedHashMap<>();

            // 실제로 어떤 row가 오는지 샘플도 보고 싶으면 최대 N개만 출력
            int maxPrint = Math.min(totalRows, 200);

            for (int i = 0; i < totalRows; i++) {
                JsonNode item = listNode.get(i);

                String fsDiv = item.path("fs_div").asText();      // CFS / OFS
                String sjDiv = item.path("sj_div").asText();      // BS / IS / CF ...
                String accountNm = item.path("account_nm").asText();
                String amountStr = item.path("thstrm_amount").asText();

                String groupKey = fsDiv + "|" + sjDiv;
                accountNamesByGroup.computeIfAbsent(groupKey, k -> new LinkedHashSet<>()).add(accountNm);

                // row 샘플 출력 (너무 많으면 maxPrint까지만)
                if (i < maxPrint) {
                    log.info("[DART row] {} ({}) fs_div={}, sj_div={}, account_nm={}, thstrm_amount={}",
                            stock.getCorpName(), stock.getStockCode(), fsDiv, sjDiv, accountNm, amountStr);
                }
            }

            // 그룹별로 어떤 account_nm들이 오는지 요약 출력 (너무 길면 50개까지만)
            for (Map.Entry<String, Set<String>> entry : accountNamesByGroup.entrySet()) {
                List<String> names = new ArrayList<>(entry.getValue());
                int show = Math.min(names.size(), 50);

                log.info("[DART account_nm 요약] group={} distinctAccountNm={} (show {}): {}",
                        entry.getKey(),
                        names.size(),
                        show,
                        String.join(", ", names.subList(0, show))
                );
            }

        } catch (Exception e) {
            log.error("[DART 디버그] 실패: {} ({}) year={}, reprtCode={}, err={}",
                    stock.getCorpName(), stock.getStockCode(), year, reportCode, e.getMessage(), e);
        }
    }



    // --- [내부 로직] API 호출 및 DTO 변환 ---
    private StockFinancial fetchFinancialData(Stock stock, Integer year, String reportCode) {

        if (stock.getCorpCode() == null || stock.getCorpCode().isEmpty()) {
            log.debug("CorpCode 없음: {} ({})", stock.getCorpName(), stock.getStockCode());
            return null;
        }

        JsonNode root = stockFinancialPort.getFinancialInfo(stock.getCorpCode(), year, reportCode);
        if(root == null){
            // 로깅은 port에서 처리 했음
            return null;
        }

        try {
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
