package org.project.ssogssog.domain.stock.repository;

import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyPriceRepository extends JpaRepository<DailyPrice, Long> {
    // 이미 수집한 날짜인지 확인용
    Optional<DailyPrice> findByStockIdAndDate(Long stockId, LocalDate date);
}