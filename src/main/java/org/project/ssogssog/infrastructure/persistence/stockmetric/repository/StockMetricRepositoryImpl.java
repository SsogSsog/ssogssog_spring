package org.project.ssogssog.infrastructure.persistence.stockmetric.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.domain.stockmetric.vo.StockMetricScreenerCondition;
import org.project.ssogssog.domain.stockmetric.entity.QStockMetric;
import org.project.ssogssog.domain.stockmetric.entity.StockMetric;
import org.project.ssogssog.domain.stockmetric.repository.StockMetricRepositoryCustom;
import org.project.ssogssog.infrastructure.persistence.stockmetric.predicate.StockMetricPredicate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockMetricRepositoryImpl implements StockMetricRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;
    private final StockMetricPredicate stockMetricPredicate;

    private static final QStockMetric qStockMetric = QStockMetric.stockMetric;

    @Override
    public List<StockMetric> getScreener(StockMetricScreenerCondition condition) {

        BooleanExpression currentPriceCondition =
                stockMetricPredicate.filterCurrentPrice(
                        qStockMetric,
                        condition.minCurrentPrice(),
                        condition.maxCurrentPrice()
                );

        BooleanExpression marketCapCondition =
                stockMetricPredicate.filterMarketCap(
                        qStockMetric,
                        condition.minMarketCap(),
                        condition.maxMarketCap()
                );

        BooleanExpression perCondition =
                stockMetricPredicate.filterPER(
                        qStockMetric,
                        condition.minPer(),
                        condition.maxPer()
                );

        BooleanExpression roeCondition =
                stockMetricPredicate.filterROE(
                        qStockMetric,
                        condition.minRoe(),
                        condition.maxRoe()
                );

        BooleanExpression salesGrowthRateCondition =
                stockMetricPredicate.filterSalesGrowthRate(
                        qStockMetric,
                        condition.minSalesGrowthRatio(),
                        condition.maxSalesGrowthRatio(),
                        condition.salesGrowthMetricBasePeriod()
                );

        BooleanExpression netProfitGrowthRateCondition =
                stockMetricPredicate.filterNetProfitGrowthRate(
                        qStockMetric,
                        condition.minNetProfitGrowthRatio(),
                        condition.maxNetProfitGrowthRatio(),
                        condition.netProfitGrowthMetricBasePeriod()
                );

        BooleanExpression debtRatioCondition =
                stockMetricPredicate.filterDebtRatio(
                        qStockMetric,
                        condition.minDebtRatio(),
                        condition.maxDebtRatio()
                );

        BooleanExpression dividendYieldCondition =
                stockMetricPredicate.filterDividendYield(
                        qStockMetric,
                        condition.minDividendYieldRatio(),
                        condition.maxDividendYieldRatio()
                );

        BooleanExpression foreignOwnershipRateCondition =
                stockMetricPredicate.filterForeignOwnershipRate(
                        qStockMetric,
                        condition.minForeignOwnershipRate(),
                        condition.maxForeignOwnershipRate()
                );

        return jpaQueryFactory
                .selectFrom(qStockMetric)
                .leftJoin(qStockMetric.stock).fetchJoin() // N+1 방지
                .where(currentPriceCondition)
                .where(marketCapCondition)
                .where(perCondition)
                .where(roeCondition)
                .where(salesGrowthRateCondition)
                .where(netProfitGrowthRateCondition)
                .where(debtRatioCondition)
                .where(dividendYieldCondition)
                .where(foreignOwnershipRateCondition)
                // TODO: orderBy 조건 추가
                .fetch();
    }
}
