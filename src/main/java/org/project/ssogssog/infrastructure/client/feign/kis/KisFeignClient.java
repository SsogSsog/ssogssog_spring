package org.project.ssogssog.infrastructure.client.feign.kis;

import org.project.ssogssog.infrastructure.client.feign.kis.config.KisFeignConfig;
import org.project.ssogssog.infrastructure.client.feign.kis.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 격리된 설정을 사용하여 다른 Feign Client에 영향을 주지 않음
 */
@FeignClient(
        name = "kis-client",
        url = "${kis.base-url}",
        configuration = KisFeignConfig.class
)
public interface KisFeignClient {

    /**
     * 토큰 발급
     * - RequestInterceptor에서 제외됨 (토큰이 필요 없음)
     */
    @PostMapping("/oauth2/tokenP")
    KisTokenResponse issueToken(@RequestBody KisTokenRequest request);

    /**
     * 주식 현재가 시세 조회
     */
    @GetMapping("/uapi/domestic-stock/v1/quotations/inquire-price")
    KisPriceResponse getCurrentPrice(
            @RequestHeader("tr_id") String trId,
            @RequestParam("FID_COND_MRKT_DIV_CODE") String marketCode,
            @RequestParam("FID_INPUT_ISCD") String stockCode
    );

    /**
     * 국내주식 기간별 시세 조회 (일봉)
     */
    @GetMapping("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
    KisHistoricalPriceResponse getHistoricalPrices(
            @RequestHeader("tr_id") String trId,
            @RequestParam("FID_COND_MRKT_DIV_CODE") String marketCode,
            @RequestParam("FID_INPUT_ISCD") String stockCode,
            @RequestParam("FID_INPUT_DATE_1") String startDate,
            @RequestParam("FID_INPUT_DATE_2") String endDate,
            @RequestParam("FID_PERIOD_DIV_CODE") String periodCode,
            @RequestParam("FID_ORG_ADJ_PRC") String adjustedPrice
    );

    /**
     * 휴장일 확인
     */
    @GetMapping("/uapi/domestic-stock/v1/quotations/chk-holiday")
    KisHolidayResponse checkHoliday(
            @RequestHeader("tr_id") String trId,
            @RequestHeader("tr_cont") String trCont,
            @RequestHeader("custtype") String custType,
            @RequestParam("BASS_DT") String baseDate,
            @RequestParam("CTX_AREA_NK") String ctxAreaNk,
            @RequestParam("CTX_AREA_FK") String ctxAreaFk
    );
}
