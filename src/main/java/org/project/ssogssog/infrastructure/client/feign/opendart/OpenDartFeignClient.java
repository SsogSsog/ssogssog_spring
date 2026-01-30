package org.project.ssogssog.infrastructure.client.feign.opendart;

import org.project.ssogssog.infrastructure.client.feign.opendart.config.OpenDartFeignConfig;
import org.project.ssogssog.infrastructure.client.feign.opendart.dto.OpenDartDividendResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * OpenDART (금융감독원 공시) API Feign Client
 * 격리된 설정을 사용하여 다른 Feign Client에 영향을 주지 않음
 */
@FeignClient(
        name = "opendart-client",
        url = "https://opendart.fss.or.kr",
        configuration = OpenDartFeignConfig.class
)
public interface OpenDartFeignClient {

    /**
     * 기업코드 ZIP 파일 다운로드
     */
    @GetMapping("/api/corpCode.xml")
    byte[] getCorpCodeZip(@RequestParam("crtfc_key") String apiKey);

    /**
     * 재무정보 조회 (단일회사 재무제표)
     */
    @GetMapping("/api/fnlttSinglAcnt.json")
    String getFinancialInfo(
            @RequestParam("crtfc_key") String apiKey,
            @RequestParam("corp_code") String corpCode,
            @RequestParam("bsns_year") Integer year,
            @RequestParam("reprt_code") String reportCode
    );

    /**
     * 공시 목록 조회
     */
    @GetMapping("/api/list.json")
    String getDisclosures(
            @RequestParam("crtfc_key") String apiKey,
            @RequestParam("corp_code") String corpCode,
            @RequestParam("bgn_de") String beginDate,
            @RequestParam("end_de") String endDate,
            @RequestParam("page_no") int pageNo,
            @RequestParam("page_count") int pageCount
    );

    /**
     * 배당금 정보 조회
     */
    @GetMapping("/api/alotMatter.json")
    OpenDartDividendResponse getDividendInfo(
            @RequestParam("crtfc_key") String apiKey,
            @RequestParam("corp_code") String corpCode,
            @RequestParam("bsns_year") String year,
            @RequestParam("reprt_code") String reportCode
    );
}
