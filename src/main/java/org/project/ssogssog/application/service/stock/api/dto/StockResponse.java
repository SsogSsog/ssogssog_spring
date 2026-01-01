package org.project.ssogssog.application.service.stock.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class StockResponse {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ThemeCollectedItemDTO implements Comparable<ThemeCollectedItemDTO> {

        private String themeName;
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

       String title;
       String link;
       String pubDate;
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


}
