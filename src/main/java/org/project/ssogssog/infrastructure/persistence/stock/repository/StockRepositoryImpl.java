package org.project.ssogssog.infrastructure.persistence.stock.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.domain.stock.projection.StockItemDTO;
import org.project.ssogssog.domain.stock.projection.ThemeItemDTO;
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
    public List<ThemeItemDTO> getThemeStockStats() {

        QDailyPrice dpSub = new QDailyPrice("dpSub"); // 서브쿼리용 별칭

        return jpaQueryFactory
                .select(
                        Projections.constructor(ThemeItemDTO.class,
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
    public Page<StockItemDTO> getStocksForThemeOrderByClosePrice(String theme, Pageable pageable) {

        QDailyPrice dpSub = new QDailyPrice("dpSub"); // 서브쿼리용 별칭

        // 종목별 가장 최신 일자
        var latestDatePerStock = JPAExpressions
                .select(dpSub.date.max())
                .from(dpSub)
                .where(dpSub.stock.eq(qStock));

        List<StockItemDTO> content =
                jpaQueryFactory.select(
                        Projections.constructor(StockItemDTO.class,
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

}
