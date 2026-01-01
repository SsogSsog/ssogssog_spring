package org.project.ssogssog.infrastructure.client.naver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@RequiredArgsConstructor
@Component
public class NaverClient {

    private final RestTemplate restTemplate;

    @Value("${naver-search.app-key}")
    private String clientId;

    @Value("${naver-search.app-secret}")
    private String clientSecret;

    /**
     * 네이버 뉴스 검색 후 responseBody를 string 자료형으로 반환
     */
    public String searchNews(String query) {
        // 1. API 호출 URL 설정 (URI 객체로 생성하여 이중 인코딩 방지)
        URI uri = UriComponentsBuilder
                .fromUriString("https://openapi.naver.com/v1/search/news.json")
                .queryParam("query", query)
                .queryParam("display", 10)
                .queryParam("sort", "date")
                .encode() // 인코딩 적용
                .build()
                .toUri();

        // 2. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Naver-Client-Id", clientId);
        headers.add("X-Naver-Client-Secret", clientSecret);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // 3. API 호출 및 응답 받기 (String 형태의 JSON)
            // 문자열 URL 대신 URI 객체를 전달해야 RestTemplate이 추가 인코딩을 하지 않음
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            return response.getBody();

        } catch (Exception e) {
            log.error("네이버 뉴스 검색 실패 - query: {}, 에러: {}", query, e.getMessage());
            // 에러 발생 시 null 반환
            return null;
        }
    }

}
