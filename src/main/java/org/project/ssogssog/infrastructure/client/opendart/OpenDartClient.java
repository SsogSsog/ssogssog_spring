package org.project.ssogssog.infrastructure.client.opendart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenDartClient {

    @Value("${opendart.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private static final String OPENDART_CORPCODE_URL  = "https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=";
    private static final String OPENDART_FINANCIAL_API_URL = "https://opendart.fss.or.kr/api/fnlttSinglAcnt.json";

    // OpenDart 에서 가져온 전체 XML 데이터 전달
    public byte[] getCorpCodeZip() {
        return restTemplate.getForObject(OPENDART_CORPCODE_URL + apiKey, byte[].class);
    }

    public String getFinancialInfo(String corpCode, Integer year, String reportCode){

        URI uri = UriComponentsBuilder.fromUriString(OPENDART_FINANCIAL_API_URL)
                .queryParam("crtfc_key", apiKey)
                .queryParam("corp_code", corpCode) // Stock 엔티티의 corpCode 사용
                .queryParam("bsns_year", year)
                .queryParam("reprt_code", reportCode)
                .build()
                .toUri();

        return restTemplate.getForObject(uri, String.class);


    }

    /**
     * 특정 기업(corp_code)의 최근 3개월 공시 목록 조회
     */
    public String getDisclosures(String corpCode, int page) {
        if (corpCode == null || corpCode.isEmpty()) {
            return null;
        }

        // 0-based page 방어
        int safePage = Math.max(page, 0);
        int pageNo = safePage + 1; // OpenDART는 1부터 시작

        // 1. 날짜 계산 (최근 3개월)
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String threeMonthsAgo = LocalDate.now().minusMonths(3).format(DateTimeFormatter.BASIC_ISO_DATE);

        // 2. URL 생성 (UriComponentsBuilder 활용 추천)
        String url = UriComponentsBuilder.fromHttpUrl("https://opendart.fss.or.kr/api/list.json")
                .queryParam("crtfc_key", apiKey)
                .queryParam("corp_code", corpCode)
                .queryParam("bgn_de", threeMonthsAgo)
                .queryParam("end_de", today)
                .queryParam("page_no", pageNo)
                .queryParam("page_count", 20) // 한 페이지에 20개만
                .build()
                .toUriString();

        try {
            // 3. API 호출
            String responseBody = restTemplate.getForObject(url, String.class);
            return responseBody;

        } catch (Exception e) {
            log.error("OpenDART 공시 조회 실패 - corpCode: {}, 에러: {}", corpCode, e.getMessage());
            return null;
        }
    }

}
