package org.project.ssogssog.application.service.stockmetric.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class StockMetricResponse {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ScreenerItemDTO {

        // 기본 종목 정보
        private Long stockId;
        private String stockCode;
        private String corpName;

        // 가격/시총
        private Integer currentPrice;   // 현재가
        private Long marketCap;         // 시가총액

        // 수익성
        private Double per;             // PER
        private Double roe;             // ROE
        private Double netProfitMargin; // 순이익률

        // 안정성
        private Double debtRatio;       // 부채비율

        // 성장성 (주로 YoY 위주 노출 추천)
        private Double salesGrowthYoY;      // 매출액 성장률 (전년)
        private Double netProfitGrowthYoY;  // 순이익 성장률 (전년)

        // 배당
        private Double dividendYield;       // 배당수익률

        // 수급/구조
        private Double foreignOwnershipRate; // 외국인 보유율

        // 주가 수익률
        private Double return3M;
        private Double return6M;
        private Double return12M;

        // 지표 기준일 (선택)
        private LocalDate calculatedAt;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ScreenerResponseDTO {

        private List<ScreenerItemDTO> items;
        private int totalCount;
    }
}
