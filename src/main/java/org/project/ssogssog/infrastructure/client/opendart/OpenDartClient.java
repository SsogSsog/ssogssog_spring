package org.project.ssogssog.infrastructure.client.opendart;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@RequiredArgsConstructor
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

}
