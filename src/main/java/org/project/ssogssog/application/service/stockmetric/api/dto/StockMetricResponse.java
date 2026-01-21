package org.project.ssogssog.application.service.stockmetric.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class StockMetricResponse {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class StockItemResponseDTO {
        private Long stockId;
        private String corpName;
        private String stockCode;
        private Integer closePrice;
        private Long volume;
        private Double changeRate;
    }

}
