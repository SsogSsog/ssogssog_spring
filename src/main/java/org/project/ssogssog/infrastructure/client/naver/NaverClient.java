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
        // 1. API 호출 URL 설정 (JSON 요청)
        String apiURL = "https://openapi.naver.com/v1/search/news.json?query=" + query + "&display=10&sort=date";

        // 2. 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Naver-Client-Id", clientId);
        headers.add("X-Naver-Client-Secret", clientSecret);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // 3. API 호출 및 응답 받기 (String 형태의 JSON)
            ResponseEntity<String> response = restTemplate.exchange(apiURL, HttpMethod.GET, entity, String.class);
            String responseBody = response.getBody();

            return responseBody;


        } catch (Exception e) {
            log.error("네이버 뉴스 검색 실패 - query: {}, 에러: {}", query, e.getMessage());
            // 에러 발생 시 null 반환
            return null;
        }
    }

}
