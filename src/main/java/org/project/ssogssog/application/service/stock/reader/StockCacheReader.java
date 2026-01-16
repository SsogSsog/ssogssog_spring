package org.project.ssogssog.application.service.stock.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.api.dto.StockResponse;
import org.project.ssogssog.application.service.stock.port.StockIssuePort;
import org.project.ssogssog.application.service.stock.usecase.dto.DisclosureDTO;
import org.project.ssogssog.application.service.stock.usecase.dto.NewsDTO;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.DailyPriceRepository;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.global.paging.SliceDTO;
import org.project.ssogssog.global.payload.code.status.ErrorStatus;
import org.project.ssogssog.global.payload.exception.GeneralException;
import org.project.ssogssog.infrastructure.config.cache.CacheType;
import org.project.ssogssog.presentation.controller.stock.enums.RankingType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockCacheReader {

    private final StockRepository stockRepository;
    private final DailyPriceRepository dailyPriceRepository;
    private final StockIssuePort stockIssuePort;

    private static final int NEWS_PAGE_SIZE = 10;
    private static final int DISCLOSURE_PAGE_SIZE = 20;

    /**
     * 종목별 뉴스 조회 (캐시)
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheType.Values.STOCK_NEWS,
            key = CacheType.Keys.STOCK_NEWS
    )
    public SliceDTO<StockResponse.NewsResponseItemDTO> getStockNews(String stockCode, int page) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND_STOCK));

        String keyword = stock.getCorpName();
        if (keyword == null || keyword.isBlank()) {
            return SliceDTO.of(Collections.emptyList(), page, NEWS_PAGE_SIZE, false);
        }

        List<NewsDTO> news = stockIssuePort.searchNews(keyword, page);

        List<StockResponse.NewsResponseItemDTO> newsItems =
                news.stream()
                        .map(n -> StockResponse.NewsResponseItemDTO.builder()
                                .title(n.title())
                                .link(n.link())
                                .pubDate(n.pubDate())
                                .build()
                        )
                        .collect(Collectors.toList());

        // TODO: 현재 hasNext가 true여서 무한으로 뉴스 검색 할 수 있으므로 횟수 제한 적용하기
        return SliceDTO.of(newsItems, page, NEWS_PAGE_SIZE, true);
    }

    /**
     * 종목별 공시 조회 (캐시)
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheType.Values.STOCK_DISCLOSURES,
            key = CacheType.Keys.STOCK_DISCLOSURE
    )
    public SliceDTO<StockResponse.DisclosureItemResponseDTO> getDisclosures(String stockCode, int page) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND_STOCK));

        String corpCode = stock.getCorpCode();
        if (corpCode == null || corpCode.isBlank()) {
            return SliceDTO.of(Collections.emptyList(), page, NEWS_PAGE_SIZE, false);
        }

        List<DisclosureDTO> disclosures = stockIssuePort.searchDisclosures(corpCode, page);

        List<StockResponse.DisclosureItemResponseDTO> disclosureItems =
                disclosures.stream()
                        .map(d -> StockResponse.DisclosureItemResponseDTO.builder()
                                .reportName(d.reportName())
                                .receiptNo(d.receiptNo())
                                .submitter(d.submitter())
                                .date(d.date())
                                .build()
                        )
                        .collect(Collectors.toList());

        // TODO: 현재 hasNext가 true여서 무한으로 공시 검색 할 수 있으므로 횟수 제한 적용하기
        return SliceDTO.of(disclosureItems, page, DISCLOSURE_PAGE_SIZE, true);
    }

    /**
     * 랭킹 top 5 조회 (캐시)
     */
    @Cacheable(
            value = CacheType.Values.STOCK_RANKING,
            key = CacheType.Keys.STOCK_RANKING
    )
    public StockResponse.RankingResponseDTO getRanking(RankingType type) {
        List<DailyPrice> dailyPrices = switch (type) {
            case RISING -> dailyPriceRepository.findTop5RisingStocks();
            case FALLING -> dailyPriceRepository.findTop5FallingStocks();
            case VOLUME -> dailyPriceRepository.findTop5VolumeStocks();
        };
        return convertToRankingDTO(dailyPrices);
    }

    /**
     * DailyPrice → RankingItemDTO 변환
     */
    private StockResponse.RankingResponseDTO convertToRankingDTO(List<DailyPrice> dailyPrices) {
        List<StockResponse.RankingItemDTO> items = new ArrayList<>();

        for (int i = 0; i < dailyPrices.size(); i++) {
            DailyPrice dp = dailyPrices.get(i);
            items.add(StockResponse.RankingItemDTO.builder()
                    .rank(i + 1)
                    .stockCode(dp.getStock().getStockCode())
                    .corpName(dp.getStock().getCorpName())
                    .currentPrice(dp.getClosePrice() != null ? dp.getClosePrice().longValue() : null)
                    .changeRate(dp.getChangeRate())
                    .tradingVolume(dp.getVolume())
                    .build());
        }

        return StockResponse.RankingResponseDTO.builder()
                .items(items)
                .totalCount(items.size())
                .build();
    }
}
