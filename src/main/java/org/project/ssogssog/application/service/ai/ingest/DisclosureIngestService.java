package org.project.ssogssog.application.service.ai.ingest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.ai.ingest.dto.IngestResult;
import org.project.ssogssog.application.service.stock.collect.dto.DisclosureDTO;
import org.project.ssogssog.application.service.stock.port.StockIssuePort;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenDART 공시(제목/메타데이터)를 임베딩해 pgvector에 증분 적재하는 배치.
 *
 * <p>파이프라인: 공시 조회(재사용) → 증분 필터 → Document 변환 → vectorStore.add.
 * 임베딩과 저장은 {@code vectorStore.add(docs)}가 내부에서 EmbeddingModel을 호출해 한 번에 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisclosureIngestService {

    /** 증분 판정 키. 저장(toDocument)과 조회(alreadyIngested)가 반드시 같은 키를 써야 한다. */
    private static final String META_RECEIPT_NO = "receiptNo";

    private final StockIssuePort stockIssuePort;   // (1) 공시 조회 재사용
    private final VectorStore vectorStore;         // (2)(4) 증분 조회 + 적재

    /**
     * 주어진 기업의 공시를 가져와 아직 적재되지 않은 것만 임베딩·적재한다.
     *
     * @param corpCode OpenDART 기업 고유 코드(8자리)
     * @return 가져온/스킵/적재 건수
     */
    public IngestResult ingest(final String corpCode) {
        // (1) 공시 조회 — 재사용 부품 (Resilience4j 적용됨). 실패 시 빈 리스트 반환.
        final List<DisclosureDTO> fetched = stockIssuePort.searchDisclosures(corpCode, 0);

        // (2)(3) 증분 필터 → Document 변환: 아직 적재 안 된 공시만 남겨 Document로 만든다
        final List<Document> toIngest = fetched.stream()
                .filter(d -> !alreadyIngested(d.receiptNo()))
                .map(this::toDocument)
                .toList();

        // (4) 임베딩 + 적재 한 번에. 빈 리스트면 add를 부르지 않는다.
        if (!toIngest.isEmpty()) {
            vectorStore.add(toIngest);
        }

        // (5) 결과 집계
        final int ingested = toIngest.size();
        final int skipped = fetched.size() - ingested;
        log.info("공시 적재 완료 - corpCode: {}, fetched: {}, skipped: {}, ingested: {}",
                corpCode, fetched.size(), skipped, ingested);
        return new IngestResult(fetched.size(), skipped, ingested);
    }

    /**
     * 해당 receiptNo의 공시가 이미 pgvector에 적재돼 있는지 확인한다.
     *
     * <p>VectorStore에는 존재 확인 전용 API가 없어, 메타데이터 필터를 건 similaritySearch로 대신한다.
     * query는 형식상 필요할 뿐 판정의 본체는 filterExpression이다.
     */
    private boolean alreadyIngested(final String receiptNo) {
        final FilterExpressionBuilder b = new FilterExpressionBuilder();
        final List<Document> found = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(receiptNo)
                        .topK(1)
                        .filterExpression(b.eq(META_RECEIPT_NO, receiptNo).build())
                        .build()
        );
        return found != null && !found.isEmpty();
    }

    /**
     * 공시 DTO를 임베딩·저장할 Document로 변환한다.
     * text(임베딩 대상)는 제목 위주로 구성하고, metadata에 증분 키(receiptNo) 등을 담는다.
     */
    private Document toDocument(final DisclosureDTO d) {

        final Map<String, Object> m = new HashMap<>();
        m.put(META_RECEIPT_NO, d.receiptNo());   // 증분 판정 키 (alreadyIngested와 일치)
        m.put("reportName", d.reportName());     // 출처 표시용
        m.put("submitter", d.submitter());
        m.put("date", d.date());

        return Document.builder()
                .text(d.reportName())            // 임베딩 대상: 공시 제목
                .metadata(m)
                .build();
    }
}
