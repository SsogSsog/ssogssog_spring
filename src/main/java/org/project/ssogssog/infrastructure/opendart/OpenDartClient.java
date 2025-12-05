package org.project.ssogssog.infrastructure.opendart;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class OpenDartClient {

    @Value("${opendart.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    static final String OPENDART_URL  = "https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=";

    // OpenDart 에서 가져온 전체 XML 데이터 전달
    public byte[] getCorpCodeZip() {
        return restTemplate.getForObject(OPENDART_URL + apiKey, byte[].class);
    }

}
