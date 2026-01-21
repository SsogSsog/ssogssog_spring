package org.project.ssogssog.infrastructure.persistence.stockmetric.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.domain.stock.entity.QDailyPrice;
import org.project.ssogssog.domain.stock.entity.QStock;
import org.project.ssogssog.domain.stock.projection.StockItemProjection;
import org.project.ssogssog.domain.stockmetric.vo.StockMetricScreenerCondition;
import org.project.ssogssog.domain.stockmetric.entity.QStockMetric;
import org.project.ssogssog.domain.stockmetric.repository.StockMetricRepositoryCustom;
import org.project.ssogssog.infrastructure.persistence.stockmetric.predicate.StockMetricPredicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockMetricRepositoryImpl implements StockMetricRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;
    private final StockMetricPredicate stockMetricPredicate;

    private static final QStockMetric qStockMetric = QStockMetric.stockMetric;
    private static final QStock qStock = QStock.stock;
    private static final QDailyPrice qDailyPrice = QDailyPrice.dailyPrice;

    @Override
    public Slice<StockItemProjection> getScreener(StockMetricScreenerCondition condition, Pageable pageable) {

        QDailyPrice dpSub = new QDailyPrice("dpSub");

        // 종목별 가장 최신 일자
        var latestDatePerStock = JPAExpressions
                .select(dpSub.date.max())
                .from(dpSub)
                .where(dpSub.stock.eq(qStock));

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

        BooleanExpression operatingProfitRatioCondition =
                stockMetricPredicate.filterOperatingProfitRatio(
                        qStockMetric,
                        condition.minOperatingProfitRatio(),
                        condition.maxOperatingProfitRatio()
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

        int size = pageable.getPageSize();

        // StockMetric 조건 필터링 + Stock + 최신 DailyPrice 조인하여 StockItemProjection 반환
        List<StockItemProjection> content =
                jpaQueryFactory
                    .select(Projections.constructor(StockItemProjection.class,
                            qStock.id,
                            qStock.corpName,
                            qStock.stockCode,
                            qDailyPrice.closePrice,
                            qDailyPrice.volume,
                            qDailyPrice.changePrice,
                            qDailyPrice.changeRate
                    ))
                    .from(qStockMetric)
                    .join(qStockMetric.stock, qStock)
                    .leftJoin(qDailyPrice).on(
                            qDailyPrice.stock.eq(qStock)
                                    .and(qDailyPrice.date.eq(latestDatePerStock))
                    )
                    .where(currentPriceCondition)
                    .where(marketCapCondition)
                    .where(perCondition)
                    .where(roeCondition)
                    .where(salesGrowthRateCondition)
                    .where(netProfitGrowthRateCondition)
                    .where(debtRatioCondition)
                    .where(operatingProfitRatioCondition)
                    .where(dividendYieldCondition)
                    .where(foreignOwnershipRateCondition)
                    .orderBy(qDailyPrice.closePrice.desc().nullsLast())  // TODO: OrderSpecifier로 동적으로 정렬 조건 설정하도록 변경하기
                    .offset(pageable.getOffset())
                    .limit(size + 1L)
                    .fetch();

        // content 사이즈로 추가 데이터가 존재하는지 판단
        boolean hasNext = content.size() > size;
        if (hasNext) {
            content.remove(size);
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }
}
