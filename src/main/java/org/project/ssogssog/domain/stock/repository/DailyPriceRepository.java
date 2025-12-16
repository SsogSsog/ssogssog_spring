package org.project.ssogssog.domain.stock.repository;

import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDate;
import java.util.Optional;

public interface DailyPriceRepository extends JpaRepository<DailyPrice, Long> {
    // 이미 수집한 날짜인지 확인용
    Optional<DailyPrice> findByStockIdAndDate(Long stockId, LocalDate date);

    // 가장 최신 KIS 데이터 조회
    Optional<DailyPrice> findTopByStockOrderByDateDesc(Stock stock);
}