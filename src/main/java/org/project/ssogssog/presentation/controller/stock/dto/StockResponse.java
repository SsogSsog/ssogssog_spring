package org.project.ssogssog.presentation.controller.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class StockResponse {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ThemeItemDTO {

        private String themeName;
        private Double changeRate;

    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ThemeCollectedItemDTO  {

        private String themeName;
        private double changeRateAverage = 0.0;
        private int totalCount = 0;
        private double sum = 0.0;

    }


    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ThemeResponseDTO {

        private List<ThemeCollectedItemDTO> items;
        private int totalCount;
    }
}
