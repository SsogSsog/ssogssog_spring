package org.project.ssogssog.domain.stock.projection;

import lombok.Builder;

@Builder
public record StockItemDTO(
        Long stockId,
        String corpName,
        String stockCode,
        Integer closePrice,
        Long volume, // 거래량
        Integer changePrice,   // 등락금
        Double changeRate // 등락률
) {
}
