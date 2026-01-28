package org.project.ssogssog.infrastructure.adapter.stock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.port.StockIssuePort;
import org.project.ssogssog.application.service.stock.collect.dto.DisclosureDTO;
import org.project.ssogssog.application.service.stock.collect.dto.NewsDTO;
import org.project.ssogssog.infrastructure.client.naver.NaverClient;
import org.project.ssogssog.infrastructure.client.opendart.OpenDartClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class StockIssueAdapter implements StockIssuePort {

    // Spring이 제공하는 ObjectMapper 주입 (JSON 파싱용)
    private final ObjectMapper objectMapper;

    private final NaverClient naverClient;
    private final OpenDartClient openDartClient;

    // TODO NaverClient 이름 변경 및 RateLimiter 적용하기

    @Override
    public List<NewsDTO> searchNews(String keyword, int page){


        String responseBody = naverClient.searchNews(keyword, page);

        if (responseBody == null || responseBody.isBlank()) {
            log.error("네이버 뉴스 검색 실패 - keyword: {}", keyword);
            // 에러 발생 시 emptyList 반환
            return Collections.emptyList();
        }

        return parseNewsResponse(responseBody);

    }

    @Override
    public List<DisclosureDTO> searchDisclosures(String corpCode, int page){
        String responseBody = openDartClient.getDisclosures(corpCode, page);

        if (responseBody == null || responseBody.isBlank()) {
            log.error("OpenDart 공시 검색 실패 - corpCode: {}", corpCode);

            return Collections.emptyList();
        }

        return parseDisclosureResponse(responseBody);
    }


    /**
     * JSON 문자열을 파싱하여 태그를 제거하고 DTO 리스트로 변환
     */
    private List<NewsDTO> parseNewsResponse(String jsonResponse){
        // JSON 문자열 -> JsonNode 트리 구조로 변환
        JsonNode root = null;
        try {
            root = objectMapper.readTree(jsonResponse);
        } catch (JsonProcessingException e) {
            log.error("네이버 뉴스 검색 실패 - 에러: {}", e.getMessage());

            return Collections.emptyList();
        }
        JsonNode items = root.path("items");

        List<NewsDTO> newsList = new ArrayList<>();

        if (items.isArray()) {
            for (JsonNode item : items) {
                // 5. 제목 정제 (HTML 태그 제거 & 특수문자 복원)
                String rawTitle = item.path("title").asText();
                String cleanTitle = rawTitle.replaceAll("<[^>]*>", "") // <b> 등 태그 삭제
                        .replaceAll("&quot;", "\"")
                        .replaceAll("&apos;", "'")
                        .replaceAll("&amp;", "&")
                        .replaceAll("&lt;", "<")
                        .replaceAll("&gt;", ">");

                String link = item.path("link").asText();
                String pubDate = item.path("pubDate").asText();

                // DTO 생성 및 리스트 추가
                newsList.add(new NewsDTO(cleanTitle, link, pubDate));
            }
        }

        return newsList;
    }

    /**
     * JSON 파싱 로직
     */
    private List<DisclosureDTO> parseDisclosureResponse(String jsonResponse){

        JsonNode root = null;
        try {
            root = objectMapper.readTree(jsonResponse);
        } catch (JsonProcessingException e) {
            log.error("OpenDart 공시 검색 실패 - 에러: {}", e.getMessage());

            return Collections.emptyList();
        }

        // OpenDART 상태 코드 확인 ("000"이 정상)
        String status = root.path("status").asText();
        if (!"000".equals(status)) {
            // 데이터가 없거나("013") 에러인 경우
            return Collections.emptyList();
        }

        JsonNode listNode = root.path("list");
        List<DisclosureDTO> resultList = new ArrayList<>();

        if (listNode.isArray()) {
            for (JsonNode item : listNode) {
                String reportName = item.path("report_nm").asText();
                String receiptNo = item.path("rcept_no").asText();
                String submitter = item.path("flr_nm").asText();
                String date = item.path("rcept_dt").asText();

                resultList.add(new DisclosureDTO(reportName, receiptNo, submitter, date));
            }
        }
        return resultList;
    }
}
