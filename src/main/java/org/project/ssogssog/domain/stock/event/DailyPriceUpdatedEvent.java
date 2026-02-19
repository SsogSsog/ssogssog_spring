package org.project.ssogssog.domain.stock.event;

/**
 * 일별 시세 데이터 업데이트 완료 이벤트
 * 이 이벤트를 수신한 리스너가 관련 캐시(랭킹, 테마 통계 등)를 무효화합니다.
 */
public record DailyPriceUpdatedEvent() {
}
