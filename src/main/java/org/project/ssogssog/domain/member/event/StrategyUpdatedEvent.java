package org.project.ssogssog.domain.member.event;

/**
 * 전략 변경 이벤트
 *
 * 특정 회원의 전략이 추가/수정/삭제되었을 때 발행됩니다.
 * 해당 회원의 전략 캐시를 무효화합니다.
 *
 * @param memberUuid 전략이 변경된 회원 UUID
 */
public record StrategyUpdatedEvent(String memberUuid) {
}
