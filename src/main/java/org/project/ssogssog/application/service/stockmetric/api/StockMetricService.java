package org.project.ssogssog.application.service.stockmetric.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.common.dto.condition.GrowthConditionDTO;
import org.project.ssogssog.application.common.dto.condition.RangeConditionDTO;
import org.project.ssogssog.application.service.stockmetric.api.dto.StockMetricResponse;
import org.project.ssogssog.domain.stock.projection.StockItemProjection;
import org.project.ssogssog.domain.stockmetric.enums.MetricBasePeriod;
import org.project.ssogssog.domain.stockmetric.vo.StockMetricScreenerCondition;
import org.project.ssogssog.domain.stockmetric.repository.StockMetricRepository;
import org.project.ssogssog.application.service.stockmetric.api.dto.StockMetricRequest;
import org.project.ssogssog.global.paging.PageDTO;
import org.project.ssogssog.presentation.controller.stockmetric.enums.MarketCapBucket;
import org.project.ssogssog.presentation.controller.stockmetric.enums.StockPriceRange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockMetricService {
    private final StockMetricRepository stockMetricRepository;

    @Transactional(readOnly = true)
    public PageDTO<StockMetricResponse.StockItemResponseDTO> getScreener(StockMetricRequest.ScreenerRequestDTO dto, Pageable pageable) {

        StockMetricScreenerCondition condition = toCondition(dto);

        // 레포지토리에서 조건 필터링 후 StockItemProjection 반환
        Page<StockItemProjection> projections = stockMetricRepository.getScreener(condition, pageable);

        Page<StockMetricResponse.StockItemResponseDTO> content = projections.map(this::toStockItemDTO);

        return PageDTO.from(content);
    }

    private StockMetricResponse.StockItemResponseDTO toStockItemDTO(StockItemProjection projection) {

        // 가격 변화량은 스크리너 조회에 필요없으므로 굳이 response에 반환하지 않음
        return StockMetricResponse.StockItemResponseDTO.builder()
                .stockId(projection.stockId())
                .corpName(projection.corpName())
                .stockCode(projection.stockCode())
                .closePrice(projection.closePrice())
                .volume(projection.volume())
                .changeRate(projection.changeRate())
                .build();
    }

    /**
     * presentation 계층의 DTO를 domain 계층의 VO로 변환해주는 메서드
     */
    private StockMetricScreenerCondition toCondition(StockMetricRequest.ScreenerRequestDTO dto) {
        return new StockMetricScreenerCondition(

                StockPriceRange.minPrice(dto.getStockPriceRange()),
                StockPriceRange.maxPrice(dto.getStockPriceRange()),

                MarketCapBucket.minPrice(dto.getMarketCapBucket()),
                MarketCapBucket.maxPrice(dto.getMarketCapBucket()),

                min(dto.getPer()),
                max(dto.getPer()),

                min(dto.getRoe()),
                max(dto.getRoe()),

                min(dto.getDebtRatio()),
                max(dto.getDebtRatio()),

                min(dto.getOperatingProfitRatio()),
                max(dto.getOperatingProfitRatio()),

                min(dto.getSalesGrowthRatio()),
                max(dto.getSalesGrowthRatio()),
                basePeriod(dto.getSalesGrowthRatio()),

                min(dto.getNetProfitGrowthRatio()),
                max(dto.getNetProfitGrowthRatio()),
                basePeriod(dto.getNetProfitGrowthRatio()),

                min(dto.getDividendYieldRatio()),
                max(dto.getDividendYieldRatio()),

                min(dto.getForeignOwnershipRate()),
                max(dto.getForeignOwnershipRate())
        );
    }

    private Double min(RangeConditionDTO dto) {
        return dto != null ? dto.getMin() : null;
    }

    private Double max(RangeConditionDTO dto) {
        return dto != null ? dto.getMax() : null;
    }

    private Double min(GrowthConditionDTO dto) {
        return dto != null ? dto.getMin() : null;
    }

    private Double max(GrowthConditionDTO dto) {
        return dto != null ? dto.getMax() : null;
    }

    private MetricBasePeriod basePeriod(GrowthConditionDTO dto) {
        return dto != null ? dto.getBasePeriod() : null;
    }
}
