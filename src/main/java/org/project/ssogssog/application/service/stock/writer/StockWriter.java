package org.project.ssogssog.application.service.stock.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.common.utils.ParserUtils;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class StockWriter {

    private final StockRepository stockRepository;

    // --- 내부 로직: XML 파싱 후 바로 업데이트 ---
    public void updateStocksWithXml(String xmlData) throws Exception {
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
                String xmlStockCode = ParserUtils.getTagValue("stock_code", element).trim();
                String xmlCorpCode = ParserUtils.getTagValue("corp_code", element).trim();

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



}
