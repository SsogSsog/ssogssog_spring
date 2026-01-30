package org.project.ssogssog.infrastructure.client.feign.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * KIS 주식현재가 시세 API 응답
 * 엔드포인트: /uapi/domestic-stock/v1/quotations/inquire-price
 */
@Getter
@NoArgsConstructor
@ToString(callSuper = true)
public class KisPriceResponse extends KisBaseResponse {

    @JsonProperty("output")
    private Output output;

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Output {
        // 영업 일자 (ex: "20240105")
        @JsonProperty("stck_bsop_date")
        private String businessDate;

        // 현재가
        @JsonProperty("stck_prpr")
        private String currentPrice;

        // 시가
        @JsonProperty("stck_oprc")
        private String openPrice;

        // 고가
        @JsonProperty("stck_hgpr")
        private String highPrice;

        // 저가
        @JsonProperty("stck_lwpr")
        private String lowPrice;

        // 전일 대비
        @JsonProperty("prdy_vrss")
        private String priceChange;

        // 전일 대비율
        @JsonProperty("prdy_ctrt")
        private String priceChangeRate;

        // 누적 거래량
        @JsonProperty("acml_vol")
        private String accumulatedVolume;

        // 시가총액(억)
        @JsonProperty("hts_avls")
        private String marketCap;

        // 상장 주식수
        @JsonProperty("lstn_stcn")
        private String listedShares;

        // 외국인 보유 수량
        @JsonProperty("frgn_hldn_qty")
        private String foreignHeldShares;

        // 전일 종가
        @JsonProperty("stck_sdpr")
        private String previousClosePrice;

        // 52주 최고가
        @JsonProperty("w52_hgpr")
        private String week52High;

        // 52주 최저가
        @JsonProperty("w52_lwpr")
        private String week52Low;

        // PBR
        @JsonProperty("pbr")
        private String pbr;

        // 업종명 (섹터)
        @JsonProperty("bstp_kor_isnm")
        private String sectorName;
    }
}
