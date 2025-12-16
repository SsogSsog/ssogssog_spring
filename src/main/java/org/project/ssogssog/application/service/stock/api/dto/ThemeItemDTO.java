package org.project.ssogssog.application.service.stock.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Builder
public record ThemeItemDTO(String themeName, Double changeRate) {


}