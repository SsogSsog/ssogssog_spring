package org.project.ssogssog.application.stock.collect;


import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.infrastructure.client.ksi.KSIClient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisSectorService {

    private final StockRepository stockRepository;
    // 1초에 10개 요청 제한 (KIS 제한: 초당 20건, 안전마진 확보)
    private final RateLimiter rateLimiter = RateLimiter.create(10.0);
    private final KSIClient ksiClient;

    /**
     * 섹터 정보가 없는 종목들을 찾아 KIS API로 업데이트
     */
    @Transactional
    public void updateMissingSectors() {

        // 0. 토큰 발급 (루프 시작 전 1회)
        String accessToken = ksiClient.getAccessToken();
        if (accessToken == null) {
            log.error("❌ 토큰 발급 실패로 작업을 중단합니다.");
            return;
        }

        // 1. 섹터가 null인 종목 조회
        List<Stock> targetStocks = stockRepository.findBySectorIsNull();

        int count = 0;
        for (Stock stock : targetStocks) {
            try {

                rateLimiter.acquire();
                // 2. KIS API 호출
                String sectorName = ksiClient.fetchSectorFromKis(stock.getStockCode(), accessToken);

                // 3. 업데이트 (Dirty Checking)
                if (sectorName != null && !sectorName.isEmpty()) {
                    stock.updateSector(sectorName);
                    count++;
                    log.info("[{}] 섹터 업데이트 완료: {}", stock.getCorpName(), sectorName);
                }

                // API 호출 빈도 조절 (초당 제한 방지, 필요시 조절)
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("[{}] 업데이트 실패: {}", stock.getCorpName(), e.getMessage());
            }
        }
        log.info("총 {}개 종목 섹터 업데이트 완료", count);
    }
}