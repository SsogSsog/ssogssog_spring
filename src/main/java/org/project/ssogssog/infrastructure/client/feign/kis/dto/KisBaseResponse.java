package org.project.ssogssog.infrastructure.client.feign.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * KIS API 응답의 공통 필드
 * rt_cd: 응답 코드 ("0" = 성공)
 * msg_cd: 메시지 코드 (에러 코드)
 * msg1: 메시지
 */
@Getter
@NoArgsConstructor
@ToString
public class KisBaseResponse {

    @JsonProperty("rt_cd")
    private String rtCd;

    @JsonProperty("msg_cd")
    private String msgCd;

    @JsonProperty("msg1")
    private String msg1;

    /**
     * 응답이 성공인지 확인
     */
    public boolean isSuccess() {
        return "0".equals(rtCd);
    }

    /**
     * Rate Limit 에러인지 확인 (초당 거래건수 초과)
     */
    public boolean isRateLimitError() {
        return "EGW00201".equals(msgCd);
    }
}
