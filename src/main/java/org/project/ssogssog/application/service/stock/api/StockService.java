package org.project.ssogssog.application.service.stock.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.presentation.controller.stock.dto.StockResponse;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    public StockResponse.ThemeResponseDTO findStocksGroupedPerSector() {

        // 개선한 점
        // 1. sector 나 rate에 관해 null 예외 처리
        // 2. 처음 나오는 sector 값에 대해 rate 추가
        // 3. 마지막에 총합으로 평균 계산
        // 4. (중요!!) arrayList 정렬하기(사전 순으로)

        List<StockResponse.ThemeItemDTO> items = stockRepository.findStocksGroupedPerSector();

        Map<String, StockResponse.ThemeCollectedItemDTO> m = new HashMap<>();
        for(var item : items){

            //TODO ThemeName이 비어있다면 "기타" 항목으로 처리 고민해보기...

            // 1. null 예외 처리
            if(item.getThemeName() == null || item.getThemeName().isBlank() || item.getChangeRate() == null){
                continue;
            }

            // 1. null 예외처리
            double changeRate = item.getChangeRate();

            // 2. String-response 객체 로 연결 및 value 업데이트
            boolean isContain = m.containsKey(item.getThemeName());
            if(!isContain){
                m.put(item.getThemeName(), new StockResponse.ThemeCollectedItemDTO(item.getThemeName()));
            }

            m.get(item.getThemeName()).addRate(changeRate);
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

}
