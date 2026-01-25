package org.project.ssogssog.infrastructure.persistence.stock.repository;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.QDailyPrice;
import org.project.ssogssog.domain.stock.repository.DailyPriceRepositoryCustom;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DailyPriceRepositoryImpl implements DailyPriceRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;
    private static final QDailyPrice qDailyPrice = QDailyPrice.dailyPrice;

    /**
     * 모든 종목의 최신 DailyPrice 한 번에 조회
     *
     * SQL 변환:

     *
     * 성능: 비상관 서브쿼리로 O(N) - 상관 서브쿼리 O(N×M) 대비 효율적
     */
    @Override
    public List<DailyPrice> findAllLatestByStock() {
        QDailyPrice dpSub = new QDailyPrice("dpSub");

        // 서브쿼리: 각 stock별 max date (비상관 - 한 번만 실행)
        var maxDatePerStock = JPAExpressions
                .select(dpSub.stock.id, dpSub.date.max())
                .from(dpSub)
                .groupBy(dpSub.stock.id);

        // 메인 쿼리: 튜플 IN 절로 매칭
        return jpaQueryFactory
                .selectFrom(qDailyPrice)
                .where(
                        Expressions.list(qDailyPrice.stock.id, qDailyPrice.date)
                                .in(maxDatePerStock)
                )
                .fetch();
    }
}
