package org.project.ssogssog.application.service.stock.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.domain.stock.entity.StockFinancial;
import org.project.ssogssog.domain.stock.repository.StockFinancialRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 재무 정보 저장 담당 Writer
 *
 * 저장만 담당하며, 캐시 무효화 이벤트는 UseCase에서 발행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockFinancialWriter {

    private final StockFinancialRepository stockFinancialRepository;

    /**
     * 재무 정보 저장 (Upsert)
     *
     * @param newData 저장할 재무 데이터
     */
    @Transactional
    public void saveOrUpdate(StockFinancial newData) {
        // 기존 데이터 확인 (Stock ID + 연도 + 분기)
        Optional<StockFinancial> existingOpt = stockFinancialRepository
                .findByStockIdAndYearAndQuarter(
                        newData.getStock().getId(),
                        newData.getYear(),
                        newData.getQuarter()
                );

        boolean isNewData = existingOpt.isEmpty();

        if (isNewData) {
            stockFinancialRepository.save(newData);
            log.debug("[StockFinancialWriter] 새 재무 데이터 저장: {} {}년 {}분기",
                    newData.getStock().getCorpName(), newData.getYear(), newData.getQuarter());
        }
        // 기존 데이터가 있으면 업데이트 없이 패스 (필요시 업데이트 로직 추가)
    }
}
