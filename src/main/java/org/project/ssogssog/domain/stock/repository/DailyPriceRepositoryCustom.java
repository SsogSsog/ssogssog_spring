package org.project.ssogssog.domain.stock.repository;

import org.project.ssogssog.domain.stock.entity.DailyPrice;

import java.util.List;

public interface DailyPriceRepositoryCustom {

    /**
     * 모든 종목의 최신 DailyPrice 한 번에 조회
     * 비상관 서브쿼리 + 튜플 IN 절 사용으로 O(N) 성능
     */
    List<DailyPrice> findAllLatestByStock();
}
