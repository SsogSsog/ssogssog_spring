package org.project.ssogssog.infrastructure.client.feign.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * KIS 국내주식 기간별 시세 API 응답
 * 엔드포인트: /uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice
 */
@Getter
@NoArgsConstructor
@ToString(callSuper = true)
public class KisHistoricalPriceResponse extends KisBaseResponse {

    @JsonProperty("output1")
    private Output1 output1;

    @JsonProperty("output2")
    private List<DailyPrice> output2;

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Output1 {
        @JsonProperty("stck_prpr")
        private String currentPrice;

        @JsonProperty("prdy_vrss")
        private String priceChange;

        @JsonProperty("prdy_ctrt")
        private String priceChangeRate;
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class DailyPrice {
        // 영업일자 (YYYYMMDD)
        @JsonProperty("stck_bsop_date")
        private String date;

        // 종가
        @JsonProperty("stck_clpr")
        private String closePrice;

        // 시가
        @JsonProperty("stck_oprc")
        private String openPrice;

        // 고가
        @JsonProperty("stck_hgpr")
        private String highPrice;

        // 저가
        @JsonProperty("stck_lwpr")
        private String lowPrice;

        // 거래량
        @JsonProperty("acml_vol")
        private String volume;

        // 전일대비
        @JsonProperty("prdy_vrss")
        private String priceChange;

        // 전일대비율
        @JsonProperty("prdy_ctrt")
        private String priceChangeRate;
    }
}
