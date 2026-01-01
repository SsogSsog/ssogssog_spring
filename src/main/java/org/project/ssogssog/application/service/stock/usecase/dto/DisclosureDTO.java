package org.project.ssogssog.application.service.stock.usecase.dto;

public record DisclosureDTO(
        String reportName, // 공시 제목 (예: 분기보고서)
        String receiptNo,  // 접수번호 (링크 생성용 핵심 Key)
        String submitter,  // 제출인 (회사명 or 제출자)
        String date       // 접수일자 (YYYYMMDD)
) {
}
