package org.project.ssogssog.infrastructure.client.feign.opendart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * OpenDART 배당 정보 API 응답
 */
@Getter
@NoArgsConstructor
@ToString
public class OpenDartDividendResponse {

    @JsonProperty("status")
    private String status;  // "000"이면 성공
    @JsonProperty("message")
    private String message;
    @JsonProperty("list")
    private List<DividendItem> list;

    public boolean isSuccess() {
        return "000".equals(status);
    }

    @Getter
    @NoArgsConstructor
    @ToString
    public static class DividendItem {
        // 구분 (예: "주당 현금배당금(원)")
        @JsonProperty("se")
        private String se;

        // 주식 종류 (예: "보통주")
        @JsonProperty("stock_knd")
        private String stockKind;

        // 당기 (올해/작년 확정치)
        @JsonProperty("thstrm")
        private String thisTerm;

        // 전기
        @JsonProperty("frmtrm")
        private String prevTerm;

        // 전전기
        @JsonProperty("lwfr")
        private String prevPrevTerm;
    }
}
