package org.project.ssogssog.infrastructure.client.feign.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * KIS 휴장일 조회 API 응답
 * 엔드포인트: /uapi/domestic-stock/v1/quotations/chk-holiday
 */
@Getter
@NoArgsConstructor
@ToString
public class KisHolidayResponse {

    @JsonProperty("ctx_area_nk")
    private String ctxAreaNk;

    @JsonProperty("ctx_area_fk")
    private String ctxAreaFk;

    @JsonProperty("output")
    private List<HolidayInfo> output;

    @Getter
    @NoArgsConstructor
    @ToString
    public static class HolidayInfo {
        // 기준일자 (YYYYMMDD)
        @JsonProperty("bass_dt")
        private String baseDate;

        // 개장여부 (Y:개장, N:휴장)
        @JsonProperty("opnd_yn")
        private String openYn;

        // 요일명
        @JsonProperty("wday_dvsn_cd_name")
        private String dayName;

        public boolean isMarketOpen() {
            return "Y".equals(openYn);
        }
    }
}
