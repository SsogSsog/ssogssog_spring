package org.project.ssogssog.application.service.stock.usecase;

import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.utils.DateUtils;
import org.project.ssogssog.application.service.stock.usecase.dto.HistoricalPriceDTO;
import org.project.ssogssog.application.service.stock.writer.DailyPriceWriter;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.infrastructure.client.ksi.KISClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectHistoricalPricesUseCase {

    private final StockRepository stockRepository;
    private final DailyPriceWriter dailyPriceWriter;

    // 1초에 10개 요청 제한 (KIS 제한: 초당 20건, 안전마진 확보)
    private final RateLimiter rateLimiter = RateLimiter.create(10.0);

    private final KISClient KISClient;

    /**
     * 특정 종목의 과거 N개월치 데이터를 가져와 DB에 저장
     * @param months 몇 개월 전부터 가져올지 (예: 6)
     */
    public void fetchAndSavePastPrices(int months) {
        // 0. 토큰 확보
        String accessToken = KISClient.getAccessToken(); // 기존에 만든 메소드 활용

        if (accessToken == null) {
            log.error("❌ 토큰 발급 실패로 작업을 중단합니다.");
            return;
        }

        List<Stock> stocks = stockRepository.findAll();
        for(Stock stock : stocks) {
            // KIS API가 한 번에 100건 밖에 데이터를 가져올 수 없으므로 3개월씩 쪼개서 가져오는 로직으로 변경
            // 전체 목표 기간: 오늘 ~ N개월 전
            final LocalDate finalEndDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
            final LocalDate finalStartDate = finalEndDate.minusMonths(months);

            log.info("[{}] 과거 {}개월 데이터 수집 시작 (목표: {} ~ {})",
                    stock.getStockCode(), months, finalStartDate, finalEndDate);

            // 3개월 마다 자를 기간 중 마지막 기간을 나타내는 변수 지정
            LocalDate currentEnd = finalEndDate;

            // 루프: 현재 종료일이 최종 시작일 보다 미래인 동안 계속 실행
            while(currentEnd.isAfter(finalStartDate)) {
                // 1. 이번 요청의 시작일 계산
                LocalDate currentStart = currentEnd.minusMonths(3);

                // 만약 3개월 전이 최종 목표보다 더 과거라면, currentStart를 최종 목표일로 맞춤
                if(currentStart.isBefore(finalStartDate)) {
                    currentStart = finalStartDate;
                }

                // 날짜 포맷팅 (YYYYMMDD)
                DateTimeFormatter formatter = DateUtils.dateTimeFormatter;
                String strStartDate = currentStart.format(formatter);
                String strEndDate = currentEnd.format(formatter);

                log.info(" >>> API 요청 구간: {} ~ {}", strStartDate, strEndDate);

                // 1초에 10번만 통과 가능하므로, 요청이 몰리면 여기서 자동으로 대기(Block)합니다.
                rateLimiter.acquire();

                // Thread.sleep 보다 Guava의 rateLimiter가 안전한 이유
                // 전역 통제: 스레드가 1개든 10개든 rateLimiter 인스턴스 하나를 공유한다면
                // (Spring Bean은 기본 싱글톤이므로 공유됨), 전체 합쳐서 초당 10회 절대 안 넘는다.

                // 유연함: API 응답이 빨라지면 RateLimiter도 그에 맞춰 바로 다음 요청을 보내고,
                // 느리면 알아서 기다줌. 따라서 sleep(100)처럼 무조건 기다리는 낭비 시간이 사라진다.

                // 2. KIS API 호출 (국내주식 기간별 시세)
                HistoricalPriceDTO historicalPriceDTO = KISClient.fetchPastPrices(stock.getStockCode(), accessToken, strStartDate, strEndDate);

                // 3. DB 저장
                if (historicalPriceDTO != null && historicalPriceDTO.getDailyItems() != null) {
                    dailyPriceWriter.saveHistoricalPrices(stock.getStockCode(), historicalPriceDTO.getDailyItems());
                }

                // 4. 종료일을 '이번 시작일의 하루 전'으로 설정
                currentEnd = currentStart.minusDays(1);

                //log.info("[{}] 데이터 수집 완료!", stockCode);

            }
        }



    }

}
