package org.project.ssogssog.application.service.stock.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

public class StockResponse {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ThemeCollectedItemDTO implements Comparable<ThemeCollectedItemDTO> {

        private String themeName;
        @Setter
        private String emoji;
        private double changeRateAverage = 0.0;
        private int totalCount = 0;
        private double sum = 0.0;

        @Override
        public int compareTo(ThemeCollectedItemDTO o) {
            return this.themeName.compareTo(o.themeName);
        }

        /**
         * 아래는 비즈니스 로직
         */
        public void addRate(double changeRateAverage) {
            this.sum += changeRateAverage;
            this.totalCount += 1;
        }


        public ThemeCollectedItemDTO(String themeName){
            this.themeName = themeName;
            this.changeRateAverage = 0.0;
            this.totalCount = 0;
            this.sum = 0.0;
        }

        public void calculateAverage() {
            if (this.totalCount <= 0) {
                this.changeRateAverage = 0.0;
            } else {
                double result = this.sum / this.totalCount;

                // 소수점 두 번째 자리까지 표현
                this.changeRateAverage = Math.round(result * 100.0) / 100.0;
            }
        }
    }


    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ThemeResponseDTO {

        private List<ThemeCollectedItemDTO> items;
        private int totalCount;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class NewsResponseItemDTO {

       private String title;
       private String link;
       private String pubDate;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DisclosureItemResponseDTO{
        private String reportName; // 공시 제목 (예: 분기보고서)
        private String receiptNo;  // 접수번호 (링크 생성용 핵심 Key)
        private String submitter;  // 제출인 (회사명 or 제출자)
        private String date;       // 접수일자 (YYYYMMDD)
    }


    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StockItemResponseDTO {
        private Long stockId;
        private String corpName;
        private String stockCode;
        private Integer closePrice;
        private Long volume;
        private Double changeRate;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StockOverviewResponseDTO {

        private String stockName;           // 주식명
        private String stockCode;           // 주식번호
        private PriceInfo priceInfo;        // 가격 정보
        private ChartData chartData;        // 차트 데이터
        private FinancialInfo financialInfo; // 기본 정보
        private CompanyInfo companyInfo;    // 기업 정보
        private String companyDescription;  // 기업 소개

        @Getter
        @Builder
        public static class PriceInfo {
            private Integer currentPrice;     // 현재가 (최근 거래일 종가)
            private Integer changeAmount;     // 전일 대비 가격 변동
            private Double changeRate;        // 전일 대비 변동률
            private Long previousClose;       // 전일 종가
            private Long previousVolume;      // 전일 거래량
        }

        @Getter
        @Builder
        public static class ChartData {
            private List<PricePoint> priceHistory;  // 최근 3개월 거래가
            private List<VolumePoint> volumeHistory; // 최근 3개월 거래량
        }

        @Getter
        @Builder
        public static class PricePoint { // {날짜, 가격}
            private LocalDate date;
            private Integer price;
        }

        @Getter
        @Builder
        public static class VolumePoint { // {날짜, 거래량}
            private LocalDate date;
            private Long volume;
        }

        @Getter
        @Builder
        public static class FinancialInfo {
            private Long marketCap;          // 시가총액
            private Double roe;              // ROE
            private Double per;              // PER
            private Double dividendYield;    // 배당 수익률
            private WeekRange week52Range;   // 52주 최저, 최고
        }

        @Getter
        @Builder
        public static class WeekRange {
            private Integer low;             // 52주 최저
            private Integer high;            // 52주 최고
        }

        @Getter
        @Builder
        public static class CompanyInfo {
            private String sector;           // 분야
            private String market;           // 시장구분 (KOSDAQ, KOSPI)
            private Double debtRate;       // 부채비율
            private Double netProfitMargin;     // 순이익률
        }

    }


    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RankingItemDTO{
        @Schema(description = "순위", example = "1")
        int rank;

        @Schema(description = "종목 코드", example = "005930")
        String stockCode;

        @Schema(description = "종목명", example = "삼성전자")
        String corpName;

        @Schema(description = "현재가", example = "72500")
        Long currentPrice;

        @Schema(description = "등락률 (%)", example = "2.5")
        Double changeRate;

        @Schema(description = "거래량", example = "15000000")
        Long tradingVolume;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RankingResponseDTO{
        private List<StockResponse.RankingItemDTO> items;
        private int totalCount;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StockPriceInfo {
        private Long stockId;
        private String stockCode;
        private String corpName;
        private String sector;
        private Integer closePrice;
        private Double changeRate;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "테마별 주식 요약 정보")
    public static class ThemeCountDTO {
        @Schema(description = "테마에 속한 총 주식 개수", example = "50")
        private int totalCount;

        @Schema(description = "상승한 주식 개수 (changeRate > 0)", example = "30")
        private int risingCount;

        @Schema(description = "하락한 주식 개수 (changeRate < 0)", example = "15")
        private int fallingCount;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "일별 시세 정보")
    public static class DailyPriceItemDTO {
        @Schema(description = "날짜", example = "2025-04-05")
        private LocalDate date;

        @Schema(description = "종가", example = "19070")
        private Integer closePrice;

        @Schema(description = "전일대비 가격 변동", example = "-640")
        private Integer changePrice;

        @Schema(description = "전일대비 등락률 (%)", example = "-3.00")
        private Double changeRate;

        @Schema(description = "거래량", example = "4265988")
        private Long volume;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "재무 정보 개요")
    public static class FinancialOverviewResponseDTO {

        @Schema(description = "재무 요약 (PER, ROE, 배당수익률, 부채비율)")
        private FinancialSummary summary;

        @Schema(description = "실적 분석 (연간/분기별 매출액, 영업이익, 당기순이익)")
        private PerformanceAnalysis performance;

        @Schema(description = "재무 안정성 지표 (부채총계, 자본총계)")
        private FinancialStability stability;

        @Getter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @Schema(description = "재무 요약")
        public static class FinancialSummary {
            @Schema(description = "PER (주가수익비율)", example = "15.2")
            private Double per;

            @Schema(description = "PBR (주가순자산비율)", example = "1.2")
            private Double pbr;

            @Schema(description = "ROE (자기자본이익률, %)", example = "12.5")
            private Double roe;

            @Schema(description = "배당수익률 (%)", example = "2.3")
            private Double dividendYield;

            @Schema(description = "부채비율 (%)", example = "45.0")
            private Double debtRatio;
        }

        @Getter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @Schema(description = "실적 분석")
        public static class PerformanceAnalysis {
            @Schema(description = "연간 실적 (최근 3년, 4Q 기준)")
            private List<PerformanceItem> annual;

            @Schema(description = "분기 실적 (최근 3분기)")
            private List<PerformanceItem> quarterly;
        }

        @Getter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @Schema(description = "실적 항목")
        public static class PerformanceItem {
            @Schema(description = "연도", example = "2024")
            private Integer year;

            @Schema(description = "분기", example = "4Q")
            private String quarter;

            @Schema(description = "매출액", example = "1000000000")
            private Long revenue;

            @Schema(description = "영업이익", example = "200000000")
            private Long operatingProfit;

            @Schema(description = "당기순이익", example = "150000000")
            private Long netIncome;

            @Schema(description = "연결재무제표 여부", example = "true")
            private Boolean isConsolidated;
        }

        @Getter
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        @Schema(description = "재무 안정성 지표")
        public static class FinancialStability {
            @Schema(description = "부채총계", example = "500000000")
            private Long totalLiabilities;

            @Schema(description = "자본총계", example = "1000000000")
            private Long totalEquity;

            @Schema(description = "연결재무제표 여부", example = "true")
            private Boolean isConsolidated;
        }
    }

    /**
     * 주식 좋아요 여부 응답 DTO
     */
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "주식 좋아요 여부")
    public static class LikedStatusDTO{
        @Schema(description = "좋아요 여부", example = "true")
        private boolean liked;
    }

}
