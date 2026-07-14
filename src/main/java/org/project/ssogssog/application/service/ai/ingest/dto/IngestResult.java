package org.project.ssogssog.application.service.ai.ingest.dto;

/**
 * 공시 임베딩 적재 배치 결과.
 *
 * @param fetched  OpenDART에서 가져온 공시 총 건수
 * @param skipped  이미 적재돼 있어 건너뛴 건수(증분)
 * @param ingested 이번에 새로 임베딩·적재한 건수
 */
public record IngestResult(
        int fetched,
        int skipped,
        int ingested
) {
}
