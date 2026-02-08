package org.project.ssogssog.infrastructure.client.feign.naver;

import org.project.ssogssog.infrastructure.client.feign.naver.config.NaverFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 네이버 검색 API Feign Client
 * 격리된 설정을 사용하여 다른 Feign Client에 영향을 주지 않음
 */
@FeignClient(
        name = "naver-client",
        url = "${naver-search.base-url}",
        configuration = NaverFeignConfig.class
)
public interface NaverFeignClient {

    /**
     * 뉴스 검색
     */
    @GetMapping("/v1/search/news.json")
    String searchNews(
            @RequestParam("query") String query,
            @RequestParam("display") int display,
            @RequestParam("start") int start,
            @RequestParam("sort") String sort
    );
}
