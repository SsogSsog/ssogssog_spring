package org.project.ssogssog.application.service.stockmetric.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stockmetric.api.dto.StockMetricScreenerCondition;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stockmetric.entity.StockMetric;
import org.project.ssogssog.domain.stockmetric.repository.StockMetricRepository;
import org.project.ssogssog.presentation.controller.stockmetric.dto.StockMetricRequest;
import org.project.ssogssog.presentation.controller.stockmetric.dto.StockMetricResponse;
import org.project.ssogssog.presentation.controller.stockmetric.enums.MarketCapBucket;
import org.project.ssogssog.presentation.controller.stockmetric.enums.StockPriceRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockMetricService {
    private final StockMetricRepository stockMetricRepository;

    @Transactional(readOnly = true)
    public StockMetricResponse.ScreenerResponseDTO getScreener(StockMetricRequest.ScreenerRequestDTO dto) {

        StockMetricScreenerCondition condition = toCondition(dto);

        // 레포지토리에서 조건에 해당하는 엔티티 목록 반환
        List<StockMetric> metrics = stockMetricRepository.getScreener(condition);

        // 엔티티 클래스를 ScreenItemDTO로 변경
        List<StockMetricResponse.ScreenerItemDTO> items = metrics.stream()
                .map(this::toScreenerItemDto)
                .toList();

        // ScreenerResponseDTO로 결과 반환
        return StockMetricResponse.ScreenerResponseDTO.builder()
                .items(items)
                .totalCount(items.size())
                .build();

    }

    private StockMetricResponse.ScreenerItemDTO toScreenerItemDto(StockMetric metric) {
        Stock stock = metric.getStock(); // LAZY라도 readOnly 트랜잭션 안이라면 OK

        return StockMetricResponse.ScreenerItemDTO.builder()
                .stockId(stock.getId())
                .stockCode(stock.getStockCode())
                .corpName(stock.getCorpName())

                .currentPrice(metric.getCurrentPrice())
                .marketCap(metric.getMarketCap())

                .per(metric.getPer())
                .roe(metric.getRoe())
                .netProfitMargin(metric.getNetProfitMargin())

                .debtRatio(metric.getDebtRatio())

                .salesGrowthYoY(metric.getSalesGrowthYoY())
                .netProfitGrowthYoY(metric.getNetProfitGrowthYoY())

                .dividendYield(metric.getDividendYield())
                .foreignOwnershipRate(metric.getForeignOwnershipRate())

                .return3M(metric.getReturn3M())
                .return6M(metric.getReturn6M())
                .return12M(metric.getReturn12M())

                .calculatedAt(metric.getCalculatedAt())
                .build();
    }

    /**
     * presentation 계층의 DTO를 application 계층의 DTO로 변환해주는 메서드
     */
    private StockMetricScreenerCondition toCondition(StockMetricRequest.ScreenerRequestDTO dto) {
        return new StockMetricScreenerCondition(

                StockPriceRange.minPrice(dto.getStockPriceRange()),
                StockPriceRange.maxPrice(dto.getStockPriceRange()),

                MarketCapBucket.minPrice(dto.getMarketCapBucket()),
                MarketCapBucket.maxPrice(dto.getMarketCapBucket()),

                dto.getMinPer(),
                dto.getMaxPer(),

                dto.getMinRoe(),
                dto.getMaxRoe(),

                dto.getMinDebtRatio(),
                dto.getMaxDebtRatio(),

                dto.getMinSalesGrowthRatio(),
                dto.getMaxSalesGrowthRatio(),
                dto.getSalesGrowthMetricBasePeriod(),

                dto.getMinNetProfitGrowthRatio(),
                dto.getMaxNetProfitGrowthRatio(),
                dto.getNetProfitGrowthMetricBasePeriod(),

                dto.getMinDividendYieldRatio(),
                dto.getMaxDividendYieldRatio(),

                dto.getMinForeignOwnershipRate(),
                dto.getMaxForeignOwnershipRate()
        );
    }
}
