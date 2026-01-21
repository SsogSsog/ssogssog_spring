package org.project.ssogssog.application.service.stock.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.reader.StockCacheReader;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.policy.ThemeEmojiRegistry;
import org.project.ssogssog.domain.stock.projection.StockItemProjection;
import org.project.ssogssog.domain.stock.projection.ThemeItemProjection;
import org.project.ssogssog.domain.stock.projection.ThemeCountProjection;
import org.project.ssogssog.domain.stock.repository.DailyPriceRepository;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.application.service.stock.api.dto.StockResponse;
import org.project.ssogssog.domain.stockmetric.entity.StockMetric;
import org.project.ssogssog.domain.stockmetric.repository.StockMetricRepository;
import org.project.ssogssog.global.paging.PageDTO;
import org.project.ssogssog.global.paging.SliceDTO;
import org.project.ssogssog.global.payload.code.status.ErrorStatus;
import org.project.ssogssog.global.payload.exception.GeneralException;
import org.project.ssogssog.presentation.controller.stock.enums.RankingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final StockMetricRepository stockMetricRepository;

    private final StockCacheReader stockCacheReader;

    private final ThemeEmojiRegistry themeEmojiRegistry;

    public StockResponse.ThemeResponseDTO getThemeStockStats() {

        // 개선한 점
        // 1. sector 나 rate에 관해 null 예외 처리
        // 2. 처음 나오는 sector 값에 대해 rate 추가
        // 3. 마지막에 총합으로 평균 계산
        // 4. (중요!!) arrayList 정렬하기(사전 순으로)

        List<ThemeItemProjection> items = stockRepository.getThemeStockStats();

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

        for (var dto : collectedItems) {
            dto.setEmoji(themeEmojiRegistry.getEmoji(dto.getThemeName()));
        }

        // 4.
        Collections.sort(collectedItems);

        return new StockResponse.ThemeResponseDTO(
                collectedItems,
                collectedItems.size()
        );
    }

    /**
     * 종목별 뉴스 조회
     */
    @Transactional(readOnly = true)
    public SliceDTO<StockResponse.NewsResponseItemDTO> getStockNews(String stockCode, int page) {
        return stockCacheReader.getStockNews(stockCode, page);
    }

    /**
     * 종목별 공시 조회
     */
    @Transactional(readOnly = true)
    public SliceDTO<StockResponse.DisclosureItemResponseDTO> getDisclosures(String stockCode, int page) {
        return stockCacheReader.getDisclosures(stockCode, page);
    }

    public PageDTO<StockResponse.StockItemResponseDTO> getStocksForTheme(String theme, Pageable pageable) {

        Page<StockItemProjection> stockItems =
                stockRepository.getStocksForThemeOrderByClosePrice(theme, pageable);

        Page<StockResponse.StockItemResponseDTO> stockItemsResponse =
                stockItems.map(this::toStockItemDTO);

        return PageDTO.from(stockItemsResponse);
    }

    private StockResponse.StockItemResponseDTO toStockItemDTO(StockItemProjection stockItemProjection) {

        return StockResponse.StockItemResponseDTO.builder()
                .stockId(stockItemProjection.stockId())
                .corpName(stockItemProjection.corpName())
                .stockCode(stockItemProjection.stockCode())
                .closePrice(stockItemProjection.closePrice())
                .volume(stockItemProjection.volume())
                .changeRate(stockItemProjection.changeRate())
                .build();
    }

    private static final int CHART_MONTHS = 3;

    @Transactional(readOnly = true)
    public StockResponse.StockOverviewResponseDTO getStockOverview(String stockCode) {

        // 주식 가져오기
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> {
                    log.error("❌ 주식 조회 실패 - stockCode: {}", stockCode);
                    return new GeneralException(ErrorStatus.NOT_FOUND_STOCK);
                });

        // 최근 일별시세 조회
        DailyPrice latest = dailyPriceRepository.findTopByStockOrderByDateDesc(stock)
                .orElseThrow(() -> {
                    log.error("❌ 일별시세 조회 실패 - stockCode: {}", stockCode);
                    return new GeneralException(ErrorStatus.NOT_FOUND_DAILY_PRICE);
                });

        // 전일 = 직전 거래일
        DailyPrice prev = dailyPriceRepository
                .findTopByStockAndDateLessThanOrderByDateDesc(stock, latest.getDate())
                .orElse(null);

        // 최근 3개월 일별시세 가져오기
        LocalDate to = latest.getDate();
        LocalDate from = to.minusMonths(CHART_MONTHS);
        List<DailyPrice> recent = dailyPriceRepository.findByStockAndDateBetweenOrderByDateAsc(stock, from, to);

        // 통계 정보 가져오기
        StockMetric metric = stockMetricRepository.findByStock(stock).orElse(null);

        Integer currentPrice = latest.getClosePrice();
        Long previousClose = prev != null ? longValue(prev.getClosePrice()) : null;

        Integer changeAmount = null;
        Double changeRate = null;

        if (prev != null && prev.getClosePrice() != null && currentPrice != null) {
            changeAmount = currentPrice - prev.getClosePrice();
            if (prev.getClosePrice() != 0) {
                changeRate = (currentPrice - prev.getClosePrice()) * 100.0 / prev.getClosePrice();
            }
        } else {
            // DailyPrice에 changePrice/changeRate가 채워져 있다면 그걸 쓰는 fallback
            changeAmount = latest.getChangePrice();
            changeRate = latest.getChangeRate();
        }

        // ChartData: 가격/거래량 각각 리스트로 분리
        List<StockResponse.StockOverviewResponseDTO.PricePoint> priceHistory = recent.stream()
                .map(dp -> StockResponse.StockOverviewResponseDTO.PricePoint.builder()
                        .date(dp.getDate())
                        .price(dp.getClosePrice())
                        .build())
                .toList();

        List<StockResponse.StockOverviewResponseDTO.VolumePoint> volumeHistory = recent.stream()
                .map(dp -> StockResponse.StockOverviewResponseDTO.VolumePoint.builder()
                        .date(dp.getDate())
                        .volume(dp.getVolume())
                        .build())
                .toList();

        // FinancialInfo: StockMetric 우선, 없으면 DailyPrice fallback(시총/52주)
        Long marketCap = metric != null && metric.getMarketCap() != null
                ? metric.getMarketCap()
                : latest.getMarketCap();

        Double roe = metric != null ? metric.getRoe() : null;
        Double per = metric != null ? metric.getPer() : null;
        Double dividendYield = metric != null ? metric.getDividendYield() : null;

        Integer w52Low = latest.getW52LowPrice();
        Integer w52High = latest.getW52HighPrice();

        // CompanyInfo: 주석 기준으로 보면 marginRate=부채비율, interestRate=순이익률로 해석
        Double debtRatio = metric != null ? metric.getDebtRatio() : null;
        Double netProfitMargin = metric != null ? metric.getNetProfitMargin() : null;

        return StockResponse.StockOverviewResponseDTO.builder()
                .stockName(stock.getCorpName())     // 주식명
                .stockCode(stock.getStockCode())    // 주식번호

                .priceInfo(StockResponse.StockOverviewResponseDTO.PriceInfo.builder()
                        .currentPrice(currentPrice)     // 현재가
                        .changeAmount(changeAmount)     // 가격변동
                        .changeRate(changeRate)         // 등락률
                        .previousClose(previousClose)   // 전날 종가
                        .build())

                .chartData(StockResponse.StockOverviewResponseDTO.ChartData.builder()
                        .priceHistory(priceHistory)     // 3개월간 가격변화
                        .volumeHistory(volumeHistory)   // 3개월간 거래량 변화
                        .build())

                .financialInfo(StockResponse.StockOverviewResponseDTO.FinancialInfo.builder()
                        .marketCap(marketCap)           // 시가 총액
                        .roe(roe)                       // roe
                        .per(per)                       // per
                        .dividendYield(dividendYield)   // 배당 수익률
                        .week52Range(StockResponse.StockOverviewResponseDTO.WeekRange.builder()
                                .low(w52Low)            // 52주 최저가
                                .high(w52High)          // 52주 최고가
                                .build())
                        .build())

                .companyInfo(StockResponse.StockOverviewResponseDTO.CompanyInfo.builder()
                        .sector(stock.getSector())
                        .market(stock.getMarketType() != null ? stock.getMarketType().name() : null)
                        .debtRate(debtRatio)
                        .netProfitMargin(netProfitMargin)
                        .build())

                // TODO description은 Stock 엔티티에 없어서 일단 null (추후 Stock.description 추가 or 외부 API)
                .companyDescription(null)
                .build();


    }

    private Long longValue(Integer v) {
        return v == null ? null : v.longValue();
    }

    /**
     * 주식 리스트의 기본정보(이름, 종목코드, 분야, 최근 가격, 최근 등락률)을 가져오는 메서드
     * @param stocks
     * @return
     */
    @Transactional(readOnly = true)
    public List<StockResponse.StockPriceInfo> getStockPriceInfos(List<Stock> stocks) {
        return stocks.stream()
                .map(stock -> {
                    DailyPrice latestPrice = dailyPriceRepository
                            .findTopByStockOrderByDateDesc(stock)
                            .orElse(null);

                    return StockResponse.StockPriceInfo.builder()
                            .stockId(stock.getId())
                            .stockCode(stock.getStockCode())
                            .corpName(stock.getCorpName())
                            .sector(stock.getSector())
                            .closePrice(latestPrice != null ? latestPrice.getClosePrice() : null)
                            .changeRate(latestPrice != null ? latestPrice.getChangeRate() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }


    /**
     * 랭킹 top 5 조회
     * @param type: 급상승, 급하락, 거래량 중 관련 RankingType 대입
     * @return
     */
    @Transactional(readOnly = true)
    public StockResponse.RankingResponseDTO getRanking(RankingType type) {
        return stockCacheReader.getRanking(type);
    }

    /**
     * 테마별 주식 요약 조회 (총 개수, 상승 개수, 하락 개수)
     * @param theme 테마명 (sector)
     * @return ThemeCountDTO
     */
    @Transactional(readOnly = true)
    public StockResponse.ThemeCountDTO getThemeCount(String theme) {
        ThemeCountProjection projection = stockRepository.getThemeCount(theme);

        return StockResponse.ThemeCountDTO.builder()
                .totalCount(projection.totalCount())
                .risingCount(projection.risingCount())
                .fallingCount(projection.fallingCount())
                .build();
    }

    /**
     * 자동완성용 주식 검색 (종목명 또는 종목코드에 키워드 포함)
     * @param keyword 검색 키워드
     * @param limit 반환할 최대 개수
     * @return StockItemResponseDTO 목록
     */
    @Transactional(readOnly = true)
    public List<StockResponse.StockItemResponseDTO> searchAutocomplete(String keyword, int limit) {
        List<StockItemProjection> projections = stockRepository.searchAutocomplete(keyword, limit);

        return projections.stream()
                .map(this::toStockItemDTO)
                .toList();
    }

    /**
     * 전체 주식 검색 (종목명 또는 종목코드에 키워드 포함, 페이지네이션)
     * @param keyword 검색 키워드
     * @param pageable 페이지네이션 정보
     * @return PageDTO<StockItemResponseDTO>
     */
    @Transactional(readOnly = true)
    public PageDTO<StockResponse.StockItemResponseDTO> search(String keyword, Pageable pageable) {
        Page<StockItemProjection> projections = stockRepository.search(keyword, pageable);

        Page<StockResponse.StockItemResponseDTO> result = projections.map(this::toStockItemDTO);

        return PageDTO.from(result);
    }
}
