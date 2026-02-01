package org.project.ssogssog.infrastructure.adapter.stock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.port.StockIssuePort;
import org.project.ssogssog.application.service.stock.collect.dto.DisclosureDTO;
import org.project.ssogssog.application.service.stock.collect.dto.NewsDTO;
import org.project.ssogssog.infrastructure.client.feign.naver.NaverFeignClient;
import org.project.ssogssog.infrastructure.client.feign.opendart.OpenDartFeignClient;
import org.project.ssogssog.infrastructure.client.feign.opendart.validator.OpenDartValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 뉴스/공시 어댑터
 * - Naver + OpenDART Feign Client 사용
 * - Resilience4j 어노테이션으로 회복 탄력성 적용
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class StockIssueAdapter implements StockIssuePort {

    private final ObjectMapper objectMapper;
    private final NaverFeignClient naverFeignClient;
    private final OpenDartFeignClient openDartFeignClient;

    @Value("${opendart.api-key}")
    private String openDartApiKey;

    /**
     * 뉴스 검색
     */
    @Override
    @Retry(name = "naver-retry", fallbackMethod = "searchNewsFallback")
    @CircuitBreaker(name = "naver-circuit")
    @RateLimiter(name = "naver-rate-limiter")
    public List<NewsDTO> searchNews(String keyword, int page) {
        if (page < 0) page = 0;

        final int display = 10;
        int start = page * display + 1;

        // Naver API 제약사항: start는 1~1000 범위
        if (start > 1000) {
            log.warn("요청한 페이지({})가 Naver API 제한을 초과하여 빈 결과를 반환합니다.", page);
            return Collections.emptyList();
        }

        String responseBody = naverFeignClient.searchNews(keyword, display, start, "date");

        if (responseBody == null || responseBody.isBlank()) {
            log.error("네이버 뉴스 검색 실패 - keyword: {}", keyword);
            return Collections.emptyList();
        }

        return parseNewsResponse(responseBody);
    }

    public List<NewsDTO> searchNewsFallback(String keyword, int page, Exception e) {

        if (e instanceof CallNotPermittedException) {
            log.warn("[Circuit 차단] 서킷이 열려있어 요청이 거부됨");
        }else{
            log.warn("네이버 뉴스 검색 실패 (fallback) - keyword: {}, page: {}, 원인: {}",
                    keyword, page, e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * 공시 검색
     */
    @Override
    @Retry(name = "opendart-retry", fallbackMethod = "searchDisclosuresFallback")
    @CircuitBreaker(name = "opendart-circuit")
    @RateLimiter(name = "opendart-rate-limiter")
    public List<DisclosureDTO> searchDisclosures(String corpCode, int page) {
        if (corpCode == null || corpCode.isEmpty()) {
            return Collections.emptyList();
        }

        // 0-based page 방어
        int safePage = Math.max(page, 0);
        int pageNo = safePage + 1;  // OpenDART는 1부터 시작

        // 날짜 계산 (최근 3개월)
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String threeMonthsAgo = LocalDate.now().minusMonths(3).format(DateTimeFormatter.BASIC_ISO_DATE);

        String responseBody = openDartFeignClient.getDisclosures(
                openDartApiKey,
                corpCode,
                threeMonthsAgo,
                today,
                pageNo,
                20  // 한 페이지에 20개
        );

        if (responseBody == null || responseBody.isBlank()) {
            log.error("OpenDart 공시 검색 실패 - corpCode: {}", corpCode);
            return Collections.emptyList();
        }

        return parseDisclosureResponse(responseBody);
    }

    public List<DisclosureDTO> searchDisclosuresFallback(String corpCode, int page, Exception e) {
        if (e instanceof CallNotPermittedException) {
            log.warn("[Circuit 차단] 서킷이 열려있어 요청이 거부됨");
        }else{
            log.warn("OpenDART 공시 검색 실패 (fallback) - corpCode: {}, page: {}, 원인: {}",
                    corpCode, page, e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * JSON 문자열을 파싱하여 태그를 제거하고 DTO 리스트로 변환
     */
    private List<NewsDTO> parseNewsResponse(String jsonResponse) {
        JsonNode root;
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
                // 제목 정제 (HTML 태그 제거 & 특수문자 복원)
                String rawTitle = item.path("title").asText();
                String cleanTitle = rawTitle.replaceAll("<[^>]*>", "")
                        .replaceAll("&quot;", "\"")
                        .replaceAll("&apos;", "'")
                        .replaceAll("&amp;", "&")
                        .replaceAll("&lt;", "<")
                        .replaceAll("&gt;", ">");

                String link = item.path("link").asText();
                String pubDate = item.path("pubDate").asText();

                newsList.add(new NewsDTO(cleanTitle, link, pubDate));
            }
        }

        return newsList;
    }

    /**
     * JSON 파싱 로직
     */
    private List<DisclosureDTO> parseDisclosureResponse(String jsonResponse) {
        JsonNode root;
        try {
            root = objectMapper.readTree(jsonResponse);
        } catch (JsonProcessingException e) {
            log.error("OpenDart 공시 검색 실패 - 에러: {}", e.getMessage());
            return Collections.emptyList();
        }

        try {
            OpenDartValidator.validate(root);
        } catch (Exception e) {
            // validate()에서 던진 예외를 상위로 던져야 Resilience4j가 반응
            throw e;
        }

        // 013(데이터 없음)인 경우 빈 리스트 반환
        if ("013".equals(root.path("status").asText())) {
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
