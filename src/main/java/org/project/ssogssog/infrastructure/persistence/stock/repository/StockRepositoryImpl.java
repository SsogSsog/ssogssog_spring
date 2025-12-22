package org.project.ssogssog.infrastructure.persistence.stock.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.domain.stock.vo.ThemeItemDTO;
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

    QDailyPrice dpSub = new QDailyPrice("dpSub"); // 서브쿼리용 별칭

    @Override
    public List<ThemeItemDTO> getThemeStockStats() {

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

}
