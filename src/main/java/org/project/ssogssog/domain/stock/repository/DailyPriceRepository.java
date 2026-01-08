package org.project.ssogssog.domain.stock.repository;

import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface DailyPriceRepository extends JpaRepository<DailyPrice, Long> {
    // 이미 수집한 날짜인지 확인용
    Optional<DailyPrice> findByStockIdAndDate(Long stockId, LocalDate date);

    // 가장 최신 KIS 데이터 조회
    Optional<DailyPrice> findTopByStockOrderByDateDesc(Stock stock);

    // latestDate보다 과거 중 가장 최신 = 직전 거래일
    Optional<DailyPrice> findTopByStockAndDateLessThanOrderByDateDesc(Stock stock, LocalDate date);

    // 해당 기간 동안 주식의 stock 가져오기
    List<DailyPrice> findByStockAndDateBetweenOrderByDateAsc(Stock stock, LocalDate from, LocalDate to);


    // 엔티티 전체가 아니라 '날짜'만 조회하는 쿼리 (가볍고 빠름)
    @Query("SELECT d.date FROM DailyPrice d WHERE d.stock = :stock")
    Set<LocalDate> findAllDatesByStock(@Param("stock") Stock stock);
}