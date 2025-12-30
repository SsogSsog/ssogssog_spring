package org.project.ssogssog.application.service.stock.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.port.StockIssuePort;
import org.project.ssogssog.application.service.stock.usecase.dto.NewsDTO;
import org.project.ssogssog.domain.stock.vo.ThemeItemDTO;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.application.service.stock.api.dto.StockResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final StockIssuePort stockIssuePort;

    public StockResponse.ThemeResponseDTO getThemeStockStats() {

        // 개선한 점
        // 1. sector 나 rate에 관해 null 예외 처리
        // 2. 처음 나오는 sector 값에 대해 rate 추가
        // 3. 마지막에 총합으로 평균 계산
        // 4. (중요!!) arrayList 정렬하기(사전 순으로)

        List<ThemeItemDTO> items = stockRepository.getThemeStockStats();

        Map<String, StockResponse.ThemeCollectedItemDTO> m = new HashMap<>();
        for(var item : items){

            //TODO ThemeName이 비어있다면 "기타" 항목으로 처리 고민해보기...

            // 1. null 예외 처리
            if(item.themeName() == null || item.themeName().isBlank() || item.changeRate() == null){
                continue;
            }

            // 1. null 예외처리
            double changeRate = item.changeRate();

            // 2. String-response 객체 로 연결 및 value 업데이트
            boolean isContain = m.containsKey(item.themeName());
            if(!isContain){
                m.put(item.themeName(), new StockResponse.ThemeCollectedItemDTO(item.themeName()));
            }

            m.get(item.themeName()).addRate(changeRate);
        }

        // 3. 평균 계산
        for(Map.Entry<String, StockResponse.ThemeCollectedItemDTO> entry : m.entrySet()){
            entry.getValue().calculateAverage();
        }

        List<StockResponse.ThemeCollectedItemDTO> collectedItems = new ArrayList<>(m.values());

        // 4.
        Collections.sort(collectedItems);

        return new StockResponse.ThemeResponseDTO(
                collectedItems,
                collectedItems.size()
        );
    }

    // TODO 페이지 네이션 적용
    public StockResponse.NewsResponseDTO getStockNews(String keyword){
        List<NewsDTO> news = stockIssuePort.searchNews(keyword);

        List<StockResponse.NewsResponseItemDTO> newsItems =
                news.stream()
                        .map(n -> StockResponse.NewsResponseItemDTO.builder()
                                .title(n.title())
                                .link(n.link())
                                .pubDate(n.pubDate())
                                .build()
                        )
                        .collect(Collectors.toList());


        return StockResponse.NewsResponseDTO.builder()
                .items(newsItems)
                .totalCount(news.size())
                .build();

    }

}
