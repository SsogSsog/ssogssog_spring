package org.project.ssogssog.application.service.stock.writer;

import lombok.RequiredArgsConstructor;
import org.project.ssogssog.domain.stock.entity.DailyPrice;
import org.project.ssogssog.domain.stock.repository.DailyPriceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class DailyPriceWriter {

    private final DailyPriceRepository dailyPriceRepository;

    @Transactional
    // --- 저장 (중복 방지) ---
    public void saveDailyPrice(DailyPrice newPrice) {
        Optional<DailyPrice> existing = dailyPriceRepository.findByStockIdAndDate(
                newPrice.getStock().getId(), newPrice.getDate());

        if (existing.isEmpty()) {
            dailyPriceRepository.save(newPrice);
        } else {
            // 이미 있으면 업데이트 로직 (필요시 구현)
        }
    }

}
