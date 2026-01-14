package org.project.ssogssog.infrastructure.persistence.stockmetric.predicate;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import org.project.ssogssog.global.payload.exception.GeneralException;
import org.project.ssogssog.global.payload.code.status.ErrorStatus;
import org.project.ssogssog.domain.stockmetric.entity.QStockMetric;
import org.project.ssogssog.domain.stockmetric.enums.MetricBasePeriod;
import org.springframework.stereotype.Component;

@Component
public class StockMetricPredicate {

    /**
     * 필터링 할 목록
     * 1. 현재가(종가 기준)
     * 2. 시가 총액
     * 3. PER
     * 4. ROE
     * 5. 부채비율
     * 6. 영업이익률
     * 7. 매출액 성장률
     * 8. 순이익 성장률
     * 9. 배당 수익률
     * 10. 외국인 보유률
     * (배당 수익률, 주가 수익률의 경우는 데이터 부족으로 현재 반영x)
     */
    public BooleanExpression filterCurrentPrice(QStockMetric qStockMetric, Integer minCurrentPrice, Integer maxCurrentPrice) {

        // 둘 다 null 이면 조건 없음
        if(minCurrentPrice == null && maxCurrentPrice == null) {
            return null;
        }

        // 둘 다 있으면 between
        if (minCurrentPrice != null && maxCurrentPrice != null){
            return qStockMetric.currentPrice.between(minCurrentPrice, maxCurrentPrice);
        }

        // 최소만 있으면 >=
        if (minCurrentPrice != null && maxCurrentPrice == null) {
            return qStockMetric.currentPrice.goe(minCurrentPrice);
        }

        // 최대만 있으면 <=
        return qStockMetric.currentPrice.loe(maxCurrentPrice);

    }


    public BooleanExpression filterMarketCap(QStockMetric qStockMetric, Long minMarketCap, Long maxMarketCap) {

        // 둘 다 null 이면 조건 없음
        if(minMarketCap == null && maxMarketCap == null){
            return null;
        }

        // 둘 다 있으면 between
        if (minMarketCap != null && maxMarketCap != null){
            return qStockMetric.marketCap.between(minMarketCap, maxMarketCap);
        }

        // 최소만 있으면 >=
        if (minMarketCap != null && maxMarketCap == null) {
            return qStockMetric.marketCap.goe(minMarketCap);
        }

        // 최대만 있으면 <=
        return qStockMetric.marketCap.loe(maxMarketCap);

    }

    public BooleanExpression filterPER(QStockMetric qStockMetric, Double minPer, Double maxPer) {

        // 둘 다 null 이면 조건 없음
        if (minPer == null && maxPer == null){
            return null;
        }

        // 둘 다 있으면 between
        if (minPer != null && maxPer != null){
            return qStockMetric.per.between(minPer, maxPer);
        }

        // 최소만 있으면 >=
        if (minPer != null &&  maxPer == null) {
            return qStockMetric.per.goe(minPer);
        }

        // 최대만 있으면 <=
        return qStockMetric.per.loe(maxPer);
    }

    // ROE
    public BooleanExpression filterROE(QStockMetric qStockMetric, Double minRoe, Double maxRoe) {

        // 둘 다 null 이면 조건 없음
        if (minRoe == null && maxRoe == null) {
            return null;
        }

        // 둘 다 있으면 between
        if (minRoe != null && maxRoe != null) {
            return qStockMetric.roe.between(minRoe, maxRoe);
        }

        // 최소만 있으면 >=
        if (minRoe != null && maxRoe == null) {
            return qStockMetric.roe.goe(minRoe);
        }

        // 최대만 있으면 <=
        return qStockMetric.roe.loe(maxRoe);
    }

    // 부채비율
    public BooleanExpression filterDebtRatio(QStockMetric qStockMetric, Double minDebtRatio, Double maxDebtRatio) {

        // 둘 다 null 이면 조건 없음
        if (minDebtRatio == null && maxDebtRatio == null) {
            return null;
        }

        // 둘 다 있으면 between
        if (minDebtRatio != null && maxDebtRatio != null) {
            return qStockMetric.debtRatio.between(minDebtRatio, maxDebtRatio);
        }

        // 최소만 있으면 >=
        if (minDebtRatio != null && maxDebtRatio == null) {
            return qStockMetric.debtRatio.goe(minDebtRatio);
        }

        // 최대만 있으면 <=
        return qStockMetric.debtRatio.loe(maxDebtRatio);
    }

    // 영업이익률
    public BooleanExpression filterOperatingProfitRatio(QStockMetric qStockMetric, Double minOperatingProfitRatio, Double maxOperatingProfitRatio) {

        // 둘 다 null 이면 조건 없음
        if (minOperatingProfitRatio == null && maxOperatingProfitRatio == null) {
            return null;
        }

        // 둘 다 있으면 between
        if (minOperatingProfitRatio != null && maxOperatingProfitRatio != null) {
            return qStockMetric.operatingProfitMargin.between(minOperatingProfitRatio, maxOperatingProfitRatio);
        }

        // 최소만 있으면 >=
        if (minOperatingProfitRatio != null && maxOperatingProfitRatio == null) {
            return qStockMetric.operatingProfitMargin.goe(minOperatingProfitRatio);
        }

        // 최대만 있으면 <=
        return qStockMetric.operatingProfitMargin.loe(maxOperatingProfitRatio);
    }

    // 매출액 성장률
    public BooleanExpression filterSalesGrowthRate(QStockMetric qStockMetric,
                                                   Double minSalesGrowthRatio,
                                                   Double maxSalesGrowthRatio,
                                                   MetricBasePeriod metricBasePeriod) {


        // 둘 다 null 이면 조건 없음
        if (minSalesGrowthRatio == null && maxSalesGrowthRatio == null) {
            return null;
        }

        NumberPath<Double> qSalesGrowthRatio;
        if(metricBasePeriod == MetricBasePeriod.PREV_YEAR){
            qSalesGrowthRatio = qStockMetric.salesGrowthYoY;
        }else if (metricBasePeriod == MetricBasePeriod.PREV_QUARTER){
            qSalesGrowthRatio = qStockMetric.salesGrowthQoQ;
        }else{
            throw new GeneralException(ErrorStatus.INVALID_METRIC_BASED_PERIOD);
        }

        // 둘 다 있으면 between
        if (minSalesGrowthRatio != null && maxSalesGrowthRatio != null) {
            return qSalesGrowthRatio.between(minSalesGrowthRatio, maxSalesGrowthRatio);
        }

        // 최소만 있으면 >=
        if (minSalesGrowthRatio != null && maxSalesGrowthRatio == null) {
            return qSalesGrowthRatio.goe(minSalesGrowthRatio);
        }

        // 최대만 있으면 <=
        return qSalesGrowthRatio.loe(maxSalesGrowthRatio);
    }

    // 순이익 성장률
    public BooleanExpression filterNetProfitGrowthRate(QStockMetric qStockMetric,
                                                       Double minNetProfitGrowthRatio,
                                                       Double maxNetProfitGrowthRatio,
                                                       MetricBasePeriod metricBasePeriod) {


        // 둘 다 null 이면 조건 없음
        if (minNetProfitGrowthRatio == null && maxNetProfitGrowthRatio == null) {
            return null;
        }

        NumberPath<Double> qNetProfitGrowthRatio;
        if(metricBasePeriod == MetricBasePeriod.PREV_YEAR){
            qNetProfitGrowthRatio = qStockMetric.netProfitGrowthYoY;
        }else if (metricBasePeriod == MetricBasePeriod.PREV_QUARTER){
            qNetProfitGrowthRatio = qStockMetric.netProfitGrowthQoQ;
        }else{
            throw new GeneralException(ErrorStatus.INVALID_METRIC_BASED_PERIOD);
        }

        // 둘 다 있으면 between
        if (minNetProfitGrowthRatio != null && maxNetProfitGrowthRatio != null) {
            return qNetProfitGrowthRatio.between(minNetProfitGrowthRatio, maxNetProfitGrowthRatio);
        }

        // 최소만 있으면 >=
        if (minNetProfitGrowthRatio != null && maxNetProfitGrowthRatio == null) {
            return qNetProfitGrowthRatio.goe(minNetProfitGrowthRatio);
        }

        // 최대만 있으면 <=
        return qNetProfitGrowthRatio.loe(maxNetProfitGrowthRatio);
    }

    // 배당 수익률
    public BooleanExpression filterDividendYield(QStockMetric qStockMetric,
                                                 Double minDividendYieldRatio,
                                                 Double maxDividendYieldRatio) {

        // 둘 다 null 이면 조건 없음
        if (minDividendYieldRatio == null && maxDividendYieldRatio == null) {
            return null;
        }

        // 둘 다 있으면 between
        if (minDividendYieldRatio != null && maxDividendYieldRatio != null) {
            return qStockMetric.dividendYield.between(minDividendYieldRatio, maxDividendYieldRatio);
        }

        // 최소만 있으면 >=
        if (minDividendYieldRatio != null && maxDividendYieldRatio == null) {
            return qStockMetric.dividendYield.goe(minDividendYieldRatio);
        }

        // 최대만 있으면 <=
        return qStockMetric.dividendYield.loe(maxDividendYieldRatio);
    }

    // 외국인 보유율
    public BooleanExpression filterForeignOwnershipRate(QStockMetric qStockMetric,
                                                        Double minForeignOwnershipRate,
                                                        Double maxForeignOwnershipRate) {

        // 둘 다 null 이면 조건 없음
        if (minForeignOwnershipRate == null && maxForeignOwnershipRate == null) {
            return null;
        }

        // 둘 다 있으면 between
        if (minForeignOwnershipRate != null && maxForeignOwnershipRate != null) {
            return qStockMetric.foreignOwnershipRate.between(minForeignOwnershipRate, maxForeignOwnershipRate);
        }

        // 최소만 있으면 >=
        if (minForeignOwnershipRate != null && maxForeignOwnershipRate == null) {
            return qStockMetric.foreignOwnershipRate.goe(minForeignOwnershipRate);
        }

        // 최대만 있으면 <=
        return qStockMetric.foreignOwnershipRate.loe(maxForeignOwnershipRate);
    }

}
