package org.project.ssogssog.presentation.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.application.service.ai.ingest.DisclosureIngestService;
import org.project.ssogssog.application.service.ai.ingest.dto.IngestResult;
import org.project.ssogssog.global.payload.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class DisclosureIngestController {

    private final DisclosureIngestService disclosureIngestService;

    @PostMapping("/ingest-disclosures")
    @Operation(
            summary = "공시 임베딩 적재 배치 (수동 트리거)",
            description = """
            OpenDART에서 해당 기업의 공시(제목/메타데이터)를 가져와,
            아직 적재되지 않은 것만 임베딩하여 pgvector에 저장합니다.
            RAG 적재(Ingestion) 단계용 개발 엔드포인트입니다.
            """
    )
    public ApiResponse<IngestResult> ingestDisclosures(
            @Parameter(description = "OpenDART 기업 고유 코드(8자리)", example = "00126380")
            @RequestParam final String corpCode
    ) {
        return ApiResponse.onSuccess(disclosureIngestService.ingest(corpCode));
    }
}
