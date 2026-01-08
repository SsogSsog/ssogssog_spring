package org.project.ssogssog.application.service.stock.api.dto;

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
}
