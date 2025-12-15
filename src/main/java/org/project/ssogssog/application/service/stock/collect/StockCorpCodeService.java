package org.project.ssogssog.application.service.stock.collect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.infrastructure.client.opendart.OpenDartClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockCorpCodeService {

    private final StockRepository stockRepository;
    private final OpenDartClient openDartClient;

    /**
     * [Step 1] DART 고유번호(corp_code) 업데이트
     * - OpenDART에서 전체 기업 목록(XML)을 다운로드
     * - 우리 DB에 있는 종목코드(stock_code)와 매칭되는 기업의 corp_code를 업데이트
     */
    @Transactional
    public void fillCorpCodes() {
        log.info("🚀 OpenDART 고유번호(CorpCode) 업데이트 시작...");

        try {
            // 1. ZIP 파일 다운로드
            byte[] zipBytes = openDartClient.getCorpCodeZip();
            if (zipBytes == null) throw new RuntimeException("OpenDART 다운로드 실패");

            // 2. 압축 해제 (XML 문자열 획득)
            String xmlData = unzip(zipBytes);

            // 3. XML 파싱 및 DB 업데이트 실행
            updateStocksWithXml(xmlData);

            log.info("✅ 고유번호(CorpCode) 업데이트 완료!");

        } catch (Exception e) {
            log.error("❌ 고유번호 업데이트 실패", e);
            throw new RuntimeException(e);
        }
    }

    // --- 내부 로직: XML 파싱 후 바로 업데이트 ---
    private void updateStocksWithXml(String xmlData) throws Exception {
        // 1. DB에 있는 모든 주식 종목 로딩 (Lookup 속도 향상을 위해 Map으로 변환)
        // Key: StockCode (005930), Value: Stock Entity
        Map<String, Stock> dbStockMap = stockRepository.findAll().stream()
                .collect(Collectors.toMap(Stock::getStockCode, Function.identity()));

        log.info("DB 로드 완료: {}개 종목 대기 중...", dbStockMap.size());

        // 2. XML 파싱 준비
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // XXE 공격 방지
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlData)));
        doc.getDocumentElement().normalize();

        NodeList nList = doc.getElementsByTagName("list");
        List<Stock> dirtyStocks = new ArrayList<>(); // 변경된 엔티티만 담을 리스트

        // 3. XML 리스트 순회 (약 10만개 데이터)
        for (int i = 0; i < nList.getLength(); i++) {
            Node node = nList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                // XML에서 stock_code와 corp_code 추출
                String xmlStockCode = getTagValue("stock_code", element).trim();
                String xmlCorpCode = getTagValue("corp_code", element).trim();

                // [필터링]
                // 1. stock_code가 없는 비상장사는 패스
                // 2. 우리 DB에 없는 종목도 패스 (우리는 KRX 리스트 기준이므로)
                if (!xmlStockCode.isEmpty() && dbStockMap.containsKey(xmlStockCode)) {
                    Stock targetStock = dbStockMap.get(xmlStockCode);

                    // corpCode가 없거나 다를 경우에만 업데이트 (Dirty Checking 대상)
                    if (targetStock.getCorpCode() == null || !targetStock.getCorpCode().equals(xmlCorpCode)) {
                        targetStock.updateCorpInfo(xmlCorpCode, targetStock.getCorpName()); // 이름도 DART 기준으로 최신화하려면 사용
                        dirtyStocks.add(targetStock);
                    }
                }
            }
        }

        // 4. 변경된 내용 일괄 저장 (Transaction 커밋 시점에 반영되지만 명시적으로 saveAll 호출도 가능)
        // JPA Dirty Checking으로 자동 저장되지만, 명시적으로 saveAll을 쓰면 배치 처리에 유리할 수 있음
        stockRepository.saveAll(dirtyStocks);

        log.info("총 {}개 종목의 CorpCode가 매칭되어 업데이트되었습니다.", dirtyStocks.size());
    }

    // --- 유틸리티 메소드 ---

    private String unzip(byte[] zipBytes) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry zipEntry = zis.getNextEntry(); // 파일이 하나만 들어있음
            if (zipEntry == null) return "";
            return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String getTagValue(String tag, Element element) {
        try {
            NodeList nodeList = element.getElementsByTagName(tag);
            if (nodeList.getLength() > 0) {
                Node node = nodeList.item(0).getChildNodes().item(0);
                return node != null ? node.getNodeValue() : "";
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }
}
