package org.project.ssogssog.application.service.stock.usecase;

import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.stock.port.StockPort;
import org.project.ssogssog.application.utils.ParserUtils;
import org.project.ssogssog.application.service.stock.writer.StockFinancialWriter;
import org.project.ssogssog.application.service.stock.writer.StockWriter;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.infrastructure.client.ksi.KISClient;
import org.project.ssogssog.infrastructure.client.opendart.OpenDartClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/***
 * Stock의 비어있거나 변경해야 할 필드를 최신화(동기화) 해주는 UseCase
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncStockDataUseCase {

    private final StockRepository stockRepository;

    private final StockWriter stockWriter;
    private final StockPort stockPort;
    /**
     * Stock에 sector(분야) 정보가 없는 종목들을 찾아 Stock의 sector 업데이트
     */
    public void updateMissingSectors() {

        // 1. 섹터가 null인 종목 조회
        List<Stock> targetStocks = stockRepository.findBySectorIsNull();

        int count = 0;
        for (Stock stock : targetStocks) {
            try {
                // 2. KIS API 호출
                String sectorName = stockPort.fetchSector(stock.getStockCode());

                // 3. 업데이트 (Dirty Checking)
                if (sectorName != null && !sectorName.isEmpty()) {
                    stock.updateSector(sectorName);
                    stockRepository.save(stock);
                    count++;
                    log.info("[{}] 섹터 업데이트 완료: {}", stock.getCorpName(), sectorName);
                }

                // API 호출 빈도 조절 (초당 제한 방지, 필요시 조절)
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("[{}] 업데이트 실패: {}", stock.getCorpName(), e.getMessage());
            }
        }
        log.info("총 {}개 종목 섹터 업데이트 완료", count);
    }


    /**
     * - DART 고유번호(corp_code) 업데이트
     * - OpenDART에서 전체 기업 목록(XML)을 다운로드
     * - 우리 DB에 있는 종목코드(stock_code)와 매칭되는 기업의 Stock의 corp_code를 업데이트
     */
    @Transactional
    public void fillCorpCodes() {
        log.info("🚀 OpenDART 고유번호(CorpCode) 업데이트 시작...");

        try {
            // 1. ZIP 파일 다운로드
            byte[] zipBytes = stockPort.getCorpCodeZip();
            if (zipBytes == null) throw new RuntimeException("OpenDART 다운로드 실패");

            // 2. 압축 해제 (XML 문자열 획득)
            String xmlData = ParserUtils.unzip(zipBytes);

            // 3. XML 파싱 및 DB 업데이트 실행
            stockWriter.updateStocksWithXml(xmlData);

            log.info("✅ 고유번호(CorpCode) 업데이트 완료!");

        } catch (Exception e) {
            log.error("❌ 고유번호 업데이트 실패", e);
            throw new RuntimeException(e);
        }
    }



}
