package org.project.ssogssog.infrastructure.client.opendart;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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

    /**
     * 특정 기업의 작년(bsnsYear) 배당금(DPS)을 조회
     * @param corpCode OpenDART 고유번호 (8자리)
     * @param bsnsYear 사업연도 (예: "2024")
     * @return 주당 배당금 (없거나 에러 시 0 반환)
     */
    public int fetchLastYearDps(String corpCode, String bsnsYear) {

        // 1. URL 생성
        // 배당에 관한 사항 API (alotMatter.json)
        // reprt_code=11011 (사업보고서 -> 그래야 1년치 합산이 나옴)
        URI uri = UriComponentsBuilder.fromHttpUrl("https://opendart.fss.or.kr/api/alotMatter.json")
                .queryParam("crtfc_key", apiKey)
                .queryParam("corp_code", corpCode)
                .queryParam("bsns_year", bsnsYear)
                .queryParam("reprt_code", "11011")
                .build()
                .toUri();

        try {
            // 2. API 호출
            OpenDartDividendResponse response = restTemplate.getForObject(uri, OpenDartDividendResponse.class);
            // [🔍 디버깅 로그] API가 도대체 뭐라고 했는지 확인!
            if (response != null) {
                log.info("🔍 OpenDART 응답 상태: code={}, msg={}", response.getStatus(), response.getMessage());

                // 데이터가 있다면 리스트 내용도 살짝 엿보기
                if (response.getList() != null) {
                    log.info("🔍 받아온 데이터 개수: {}", response.getList().size());
                    // 첫 번째 데이터만 샘플로 찍어보기 (필드명 확인용)
                    if (!response.getList().isEmpty()) {
                        log.info("🔍 첫 번째 데이터 샘플: {}", response.getList().get(0));
                    }
                } else {
                    log.info("🔍 리스트(list)가 NULL입니다.");
                }
            } else {
                log.error("🔍 응답(Response) 객체가 NULL입니다.");
            }

            if (response == null || !"000".equals(response.getStatus()) || response.getList() == null) {
                log.warn("배당 정보 조회 실패 or 데이터 없음 (Code: {}, Year: {})", corpCode, bsnsYear);
                return 0;
            }

            // 3. 필터링 및 파싱 (핵심 로직!)
            // 리스트 중에서 "주당 현금배당금(원)" 이면서 "보통주"인 항목 찾기
            Optional<OpenDartDividendResponse.DividendItem> targetItem = response.getList().stream()
                    .filter(item -> "주당 현금배당금(원)".equals(item.getSe())) // 구분 확인
                    .filter(item -> "보통주".equals(item.getStockKind()))      // 주식 종류 확인
                    .findFirst();

            if (targetItem.isEmpty()) {
                // 배당금 항목이 아예 없는 경우 (배당 안 주는 회사)
                return 0;
            }

            // 4. 문자열("1,200") -> 숫자(1200) 변환
            String dpsStr = targetItem.get().getThisTerm();
            return parseDps(dpsStr);

        } catch (Exception e) {
            log.error("OpenDART 파싱 중 에러 발생: {}", e.getMessage());
            return 0;
        }
    }

    // 숫자 파싱 유틸 메서드
    private int parseDps(String value) {
        if (value == null || value.trim().isEmpty() || "-".equals(value.trim())) {
            return 0;
        }
        try {
            // 콤마 제거 후 정수 변환 ("1,444" -> 1444)
            return Integer.parseInt(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    // 내부 DTO
    @Getter
    @NoArgsConstructor
    @ToString
    public static class OpenDartDividendResponse {

        private String status;  // "000"이면 성공
        private String message; // 에러 메시지
        private List<DividendItem> list; // 데이터 리스트

        @Getter
        @NoArgsConstructor
        @ToString
        public static class DividendItem {
            // 예: "주당 현금배당금(원)" <- 이걸 찾아야 함
            @JsonProperty("se")
            private String se;

            // 예: "보통주" <- 이걸 찾아야 함
            @JsonProperty("stock_knd")
            private String stockKind;

            // 당기 (올해/작년 확정치) 값 (예: "1,444" or "-")
            @JsonProperty("thstrm")
            private String thisTerm;
        }
    }

}
