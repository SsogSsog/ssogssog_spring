package org.project.ssogssog.domain.stock.event;

/**
 * 주식 정보 업데이트 완료 이벤트
 * 이 이벤트를 수신한 리스너가 주식 정보(테마 등) 캐시를 무효화합니다.
 */
public record StockUpdatedEvent() {
}
