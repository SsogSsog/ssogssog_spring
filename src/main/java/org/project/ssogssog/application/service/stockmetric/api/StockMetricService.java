package org.project.ssogssog.application.service.stockmetric.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.domain.stockmetric.vo.StockMetricScreenerCondition;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stockmetric.entity.StockMetric;
import org.project.ssogssog.domain.stockmetric.repository.StockMetricRepository;
import org.project.ssogssog.application.service.stockmetric.api.dto.StockMetricRequest;
import org.project.ssogssog.application.service.stockmetric.api.dto.StockMetricResponse;
import org.project.ssogssog.global.paging.SliceDTO;
import org.project.ssogssog.presentation.controller.stockmetric.enums.MarketCapBucket;
import org.project.ssogssog.presentation.controller.stockmetric.enums.StockPriceRange;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
    public SliceDTO<StockMetricResponse.ScreenerItemDTO> getScreener(StockMetricRequest.ScreenerRequestDTO dto, Pageable pageable) {

        StockMetricScreenerCondition condition = toCondition(dto);

        // 레포지토리에서 조건에 해당하는 엔티티 목록 반환
        Slice<StockMetric> metrics = stockMetricRepository.getScreener(condition, pageable);

        Slice<StockMetricResponse.ScreenerItemDTO> content = metrics.map(
                this::toScreenerItemDto
        );

        // Slice를 SliceDTO로 반환
        return SliceDTO.from(content);

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
