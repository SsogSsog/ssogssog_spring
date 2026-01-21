package org.project.ssogssog.domain.stock.projection;

import lombok.Builder;


@Builder
public record ThemeItemProjection(String themeName, Double changeRate) {


}