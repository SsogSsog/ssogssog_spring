package org.project.ssogssog.application.service.stock.collect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class KisDailyPriceResponse {
    @JsonProperty("output1")
    private Output1 output1; // 대표 정보

    @JsonProperty("output2")
    private List<DailyItem> dailyItems; // 일별 데이터 리스트

    @JsonProperty("rt_cd")
    private String returnCode;

    @JsonProperty("msg1")
    private String message;

    @Data
    public static class Output1 {
        @JsonProperty("prdy_vrss")
        private String priceChange; // 전일 대비
    }

    @Data
    public static class DailyItem {
        @JsonProperty("stck_bsop_date")
        private String date; // 영업 일자 (YYYYMMDD)

        @JsonProperty("stck_clpr")
        private String closePrice; // 종가

        @JsonProperty("stck_oprc")
        private String openPrice; // 시가

        @JsonProperty("stck_hgpr")
        private String highPrice; // 고가

        @JsonProperty("stck_lwpr")
        private String lowPrice; // 저가

        @JsonProperty("acml_vol")
        private String volume; // 누적 거래량

        // 과거 데이터를 가져오는 api는 위의 필드만 지원
    }
}