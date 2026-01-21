package org.project.ssogssog.infrastructure.persistence.stock.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import com.querydsl.core.Tuple;
import org.project.ssogssog.domain.stock.projection.StockItemProjection;
import org.project.ssogssog.domain.stock.projection.ThemeItemProjection;
import org.project.ssogssog.domain.stock.projection.ThemeCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.project.ssogssog.domain.stock.entity.QDailyPrice;
import org.project.ssogssog.domain.stock.entity.QStock;
import org.project.ssogssog.domain.stock.repository.StockRepositoryCustom;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockRepositoryImpl implements StockRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;
    private static final QStock qStock = QStock.stock;
    private static final QDailyPrice qDailyPrice = QDailyPrice.dailyPrice;

    @Override
    public List<ThemeItemProjection> getThemeStockStats() {

        QDailyPrice dpSub = new QDailyPrice("dpSub"); // 서브쿼리용 별칭

        return jpaQueryFactory
                .select(
                        Projections.constructor(ThemeItemProjection.class,
                                qStock.sector,
                                qDailyPrice.changeRate
                        ))
                .from(qStock)
                .leftJoin(qDailyPrice)
                .on(
                        qDailyPrice.stock.eq(qStock)
                                .and(
                                        qDailyPrice.date.eq(
                                                JPAExpressions
                                                        .select(dpSub.date.max())
                                                        .from(dpSub)
                                                        .where(dpSub.stock.eq(qStock))
                                        )
                                )

                )
                .fetch();


    }

    @Override
    public Page<StockItemProjection> getStocksForThemeOrderByClosePrice(String theme, Pageable pageable) {

        QDailyPrice dpSub = new QDailyPrice("dpSub"); // 서브쿼리용 별칭

        // 종목별 가장 최신 일자
        var latestDatePerStock = JPAExpressions
                .select(dpSub.date.max())
                .from(dpSub)
                .where(dpSub.stock.eq(qStock));

        List<StockItemProjection> content =
                jpaQueryFactory.select(
                        Projections.constructor(StockItemProjection.class,
                                qStock.id,
                                qStock.corpName,
                                qStock.stockCode,
                                qDailyPrice.closePrice,
                                qDailyPrice.volume,
                                qDailyPrice.changePrice,
                                qDailyPrice.changeRate
                        ))
                        .from(qStock)
                        .leftJoin(qDailyPrice).on(
                                qDailyPrice.stock.eq(qStock)
                                        .and(qDailyPrice.date.eq(latestDatePerStock))
                        )
                        .where(qStock.sector.eq(theme))
                        .orderBy(qDailyPrice.closePrice.desc().nullsLast()) // 가격 없는 종목은 뒤로 가도록 설정
                        .offset(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .fetch();


        Long total = jpaQueryFactory
                .select(qStock.count())
                .from(qStock)
                .where(qStock.sector.eq(theme))
                .fetchOne();

        Long safeTotal = (total == null) ? 0L : total;

        return new PageImpl<>(content, pageable, safeTotal);
    }

    @Override
    public ThemeCountProjection getThemeCount(String theme) {

        QDailyPrice dpSub = new QDailyPrice("dpSub");

        // 종목별 가장 최신 일자
        var latestDatePerStock = JPAExpressions
                .select(dpSub.date.max())
                .from(dpSub)
                .where(dpSub.stock.eq(qStock));

        // 테마에 속한 주식들의 최신 changeRate 조회
        List<Tuple> results = jpaQueryFactory
                .select(
                        qStock.id,
                        qDailyPrice.changeRate
                )
                .from(qStock)
                .leftJoin(qDailyPrice).on(
                        qDailyPrice.stock.eq(qStock)
                                .and(qDailyPrice.date.eq(latestDatePerStock))
                )
                .where(qStock.sector.eq(theme))
                .fetch();

        int totalCount = results.size();
        int risingCount = 0;
        int fallingCount = 0;

        for (Tuple tuple : results) {
            Double changeRate = tuple.get(qDailyPrice.changeRate);
            if (changeRate != null) {
                if (changeRate > 0) {
                    risingCount++;
                } else if (changeRate < 0) {
                    fallingCount++;
                }
            }
        }

        return new ThemeCountProjection(totalCount, risingCount, fallingCount);
    }

    @Override
    public List<StockItemProjection> searchAutocomplete(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        QDailyPrice dpSub = new QDailyPrice("dpSub");

        var latestDatePerStock = JPAExpressions
                .select(dpSub.date.max())
                .from(dpSub)
                .where(dpSub.stock.eq(qStock));

        String pattern = "%" + keyword + "%";

        return jpaQueryFactory
                .select(Projections.constructor(StockItemProjection.class,
                        qStock.id,
                        qStock.corpName,
                        qStock.stockCode,
                        qDailyPrice.closePrice,
                        qDailyPrice.volume,
                        qDailyPrice.changePrice,
                        qDailyPrice.changeRate
                ))
                .from(qStock)
                .leftJoin(qDailyPrice).on(
                        qDailyPrice.stock.eq(qStock)
                                .and(qDailyPrice.date.eq(latestDatePerStock))
                )
                .where(
                        qStock.corpName.like(pattern)
                                .or(qStock.stockCode.like(pattern))
                )
                .orderBy(qDailyPrice.closePrice.desc().nullsLast())
                .limit(limit)
                .fetch();
    }

    @Override
    public Page<StockItemProjection> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        QDailyPrice dpSub = new QDailyPrice("dpSub");

        var latestDatePerStock = JPAExpressions
                .select(dpSub.date.max())
                .from(dpSub)
                .where(dpSub.stock.eq(qStock));

        String pattern = "%" + keyword + "%";

        List<StockItemProjection> content = jpaQueryFactory
                .select(Projections.constructor(StockItemProjection.class,
                        qStock.id,
                        qStock.corpName,
                        qStock.stockCode,
                        qDailyPrice.closePrice,
                        qDailyPrice.volume,
                        qDailyPrice.changePrice,
                        qDailyPrice.changeRate
                ))
                .from(qStock)
                .leftJoin(qDailyPrice).on(
                        qDailyPrice.stock.eq(qStock)
                                .and(qDailyPrice.date.eq(latestDatePerStock))
                )
                .where(
                        qStock.corpName.like(pattern)
                                .or(qStock.stockCode.like(pattern))
                )
                .orderBy(qDailyPrice.closePrice.desc().nullsLast())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(qStock.count())
                .from(qStock)
                .where(
                        qStock.corpName.like(pattern)
                                .or(qStock.stockCode.like(pattern))
                )
                .fetchOne();

        Long safeTotal = (total == null) ? 0L : total;

        return new PageImpl<>(content, pageable, safeTotal);
    }

}
