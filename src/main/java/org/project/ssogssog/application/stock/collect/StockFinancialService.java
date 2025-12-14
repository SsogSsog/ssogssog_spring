package org.project.ssogssog.application.stock.collect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.stock.writer.StockFinancialWriter;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.project.ssogssog.domain.stock.repository.StockFinancialRepository;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.infrastructure.client.opendart.OpenDartClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockFinancialService {

    private final StockRepository stockRepository;
    private final StockFinancialRepository stockFinancialRepository;
    private final StockFinancialWriter stockFinancialWriter;

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
                // 에러거나 데이터가 없음 (013: 데이터 없음 등)
                return null;
            }

            JsonNode listNode = root.path("list");
            if (listNode.isMissingNode() || listNode.isEmpty()) {
                return null;
            }

            // 빌더 초기화
            StockFinancial.StockFinancialBuilder builder = StockFinancial.builder()
                    .stock(stock) // @ManyToOne 관계 설정 (객체 자체를 넣음)
                    .year(year)
                    .quarter(convertReportCodeToQuarter(reportCode));

            boolean isDataFound = false;

            // 리스트 순회하며 필요한 값 매핑
            for (JsonNode item : listNode) {
                // [중요] 연결재무제표(CFS) 우선, 없으면 별도(OFS) 로직은 여기서 단순화하여 CFS만 처리
                // (더 정교하게 하려면 CFS 다 찾고 없으면 OFS 찾는 로직 추가 필요)
                if ("CFS".equals(item.path("fs_div").asText())) {
                    String accountName = item.path("account_nm").asText();
                    Long amount = parseAmount(item.path("thstrm_amount").asText());

                    // 계정명 매핑 (OpenDART 계정명이 회사마다 조금씩 다를 수 있어 contain 등 사용)
                    if (accountName.contains("매출액") || accountName.equals("수익(매출액)")) {
                        builder.revenue(amount);
                        isDataFound = true;
                    } else if (accountName.contains("영업이익")) {
                        builder.operatingProfit(amount);
                    } else if (accountName.contains("당기순이익")) {
                        builder.netIncome(amount);
                    } else if (accountName.equals("자산총계")) {
                        builder.totalAssets(amount);
                    } else if (accountName.equals("부채총계")) {
                        builder.totalLiabilities(amount);
                    } else if (accountName.equals("자본총계")) {
                        builder.totalEquity(amount);
                    }
                }
            }

            return isDataFound ? builder.build() : null;

        } catch (Exception e) {
            log.warn("파싱 에러: {}", stock.getCorpName());
            return null;
        }
    }

    // 금액 파싱 (콤마 제거, 공백 처리)
    private Long parseAmount(String amountStr) {
        try {
            if (amountStr == null || amountStr.isEmpty() || amountStr.equals("-")) return 0L;
            return Long.parseLong(amountStr.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    // 보고서 코드를 "1Q", "4Q" 등으로 변환
    private String convertReportCodeToQuarter(String reportCode) {
        return switch (reportCode) {
            case "11013" -> "1Q";
            case "11012" -> "2Q"; // 반기
            case "11014" -> "3Q";
            case "11011" -> "4Q"; // 사업보고서 (연간)
            default -> "Etc";
        };
    }
}