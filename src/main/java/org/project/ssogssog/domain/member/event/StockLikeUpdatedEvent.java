package org.project.ssogssog.domain.member.event;

/**
 * 주식 좋아요 변경 이벤트
 *
 * 특정 회원의 좋아요가 추가/삭제되었을 때 발행됩니다.
 * 해당 회원의 좋아요 캐시를 무효화합니다.
 *
 * @param memberUuid 좋아요가 변경된 회원 UUID
 */
public record StockLikeUpdatedEvent(String memberUuid) {
}
