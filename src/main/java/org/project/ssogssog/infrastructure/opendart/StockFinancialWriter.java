package org.project.ssogssog.infrastructure.opendart;

import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.project.ssogssog.domain.stock.repository.StockFinancialRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class StockFinancialWriter {

    StockFinancialRepository stockFinancialRepository;

    @Transactional
    // --- [내부 로직] 저장 (Upsert) ---
    public void saveOrUpdate(StockFinancial newData) {
        // 기존 데이터 확인 (Stock ID + 연도 + 분기)
        Optional<StockFinancial> existingOpt = stockFinancialRepository
                .findByStockIdAndYearAndQuarter(
                        newData.getStock().getId(),
                        newData.getYear(),
                        newData.getQuarter()
                );

        if (existingOpt.isPresent()) {
            // 이미 있으면? -> (JPA Dirty Checking으로 업데이트하거나, 여기선 편의상 삭제 후 재등록 or 값 변경)
            // 여기선 간단하게 기존 데이터가 있으면 pass (업데이트 로직 필요시 추가)
            // log.info("이미 존재함: {}", newData.getStock().getCorpName());
        } else {
            stockFinancialRepository.save(newData);
        }
    }


}
