package org.project.ssogssog.application.service.stock.api.converter;

import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.api.dto.StockResponse;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.project.ssogssog.domain.stock.projection.StockItemProjection;
import org.project.ssogssog.domain.stockmetric.factory.StockMetricCalculator;

import java.util.Map;

@Slf4j
public class StockConverter {

    /**
     * 4Q 단독 실적을 PerformanceItem으로 변환
     * StockMetricCalculator를 활용하여 연간누적(4Q) - 1Q - 2Q - 3Q 계산
     */
    public static StockResponse.FinancialOverviewResponseDTO.PerformanceItem toQ4StandalonePerformanceItem(
            Integer year, Map<String, StockFinancial> yearData) {

        StockFinancial q4 = yearData.get("4Q");

        if (q4 == null) {
            log.warn("4Q 데이터가 없어 계산 불가 - year: {}", year);
            return null;
        }

        StockFinancial q1 = yearData.get("1Q");
        StockFinancial q2 = yearData.get("2Q");
        StockFinancial q3 = yearData.get("3Q");

        // 1Q, 2Q, 3Q 중 하나라도 없으면 계산 불가 → 4Q 원본 반환 (fallback)
        if (q1 == null || q2 == null || q3 == null) {
            log.warn("4Q 단독 실적 계산 불가 (분기 데이터 부족) - year: {}", year);
            // 데이터가 없으니 추정값이라도 전달
            return StockResponse.FinancialOverviewResponseDTO.PerformanceItem.builder()
                    .year(year)
                    .quarter("4Q")
                    .revenue(safeDivideBy4(q4.getRevenue()/4L))
                    .operatingProfit(safeDivideBy4(q4.getOperatingProfit()/4L))
                    .netIncome(safeDivideBy4(q4.getNetIncome()/4L))
                    .isConsolidated(q4.isConsolidated())
                    .build();
        }

        // StockMetricCalculator를 활용한 4Q 단독 계산
        Long revenue = StockMetricCalculator.calculateQ4StandaloneValue(
                q4.getRevenue(), q1.getRevenue(), q2.getRevenue(), q3.getRevenue());
        Long operatingProfit = StockMetricCalculator.calculateQ4StandaloneValue(
                q4.getOperatingProfit(), q1.getOperatingProfit(), q2.getOperatingProfit(), q3.getOperatingProfit());
        Long netIncome = StockMetricCalculator.calculateQ4StandaloneValue(
                q4.getNetIncome(), q1.getNetIncome(), q2.getNetIncome(), q3.getNetIncome());

        return StockResponse.FinancialOverviewResponseDTO.PerformanceItem.builder()
                .year(year)
                .quarter("4Q")
                .revenue(revenue)
                .operatingProfit(operatingProfit)
                .netIncome(netIncome)
                .isConsolidated(q4.isConsolidated())
                .build();
    }

    /**
     * 1Q, 2Q, 3Q의 경우 단독 실적을 PerformanceItem으로 변환
     */
    public static StockResponse.FinancialOverviewResponseDTO.PerformanceItem toPerformanceItem(StockFinancial sf) {
        return StockResponse.FinancialOverviewResponseDTO.PerformanceItem.builder()
                .year(sf.getYear())
                .quarter(sf.getQuarter())
                .revenue(sf.getRevenue())
                .operatingProfit(sf.getOperatingProfit())
                .netIncome(sf.getNetIncome())
                .isConsolidated(sf.isConsolidated())
                .build();
    }

    public static StockResponse.StockItemResponseDTO toStockItemDTO(StockItemProjection stockItemProjection) {

        return StockResponse.StockItemResponseDTO.builder()
                .stockId(stockItemProjection.stockId())
                .corpName(stockItemProjection.corpName())
                .stockCode(stockItemProjection.stockCode())
                .closePrice(stockItemProjection.closePrice())
                .volume(stockItemProjection.volume())
                .changeRate(stockItemProjection.changeRate())
                .build();
    }

    private static Long safeDivideBy4(Long value) {
        if (value == null) {
            return null; // 값이 없으면 null 반환 (0으로 주면 데이터 왜곡됨)
        }
        return value / 4L;
    }
}
