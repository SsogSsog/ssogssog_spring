package org.project.ssogssog.application.service.member.reader;

import lombok.RequiredArgsConstructor;
import org.project.ssogssog.application.service.member.api.converter.StrategyConverter;
import org.project.ssogssog.application.service.member.api.dto.MemberResponse;
import org.project.ssogssog.domain.member.entity.Member;
import org.project.ssogssog.domain.member.entity.Strategy;
import org.project.ssogssog.domain.member.repository.MemberRepository;
import org.project.ssogssog.domain.member.repository.StockLikeRepository;
import org.project.ssogssog.domain.member.repository.StrategyRepository;
import org.project.ssogssog.global.payload.code.status.ErrorStatus;
import org.project.ssogssog.global.payload.exception.GeneralException;
import org.project.ssogssog.infrastructure.config.cache.CacheType;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberCacheReader {

    private final StockLikeRepository stockLikeRepository;
    private final StrategyRepository strategyRepository;
    private final MemberRepository memberRepository;

    /**
     * 좋아요한 Stock ID 목록 조회 (캐시)
     */
    @Cacheable(
            value = CacheType.Values.MEMBER_LIKED_STOCKS,
            key = CacheType.Keys.MEMBER_UUID
    )
    public List<Long> getLikedStockIds(String uuid) {
        return stockLikeRepository.findStockIdsByMemberUuid(uuid);
    }

    /**
     * 좋아요 캐시 무효화
     */
    @CacheEvict(
            value = CacheType.Values.MEMBER_LIKED_STOCKS,
            key = CacheType.Keys.MEMBER_UUID
    )
    public void evictLikedStocks(String uuid) {
        // 캐시만 삭제
    }

    /**
     * 전략 목록 조회 (캐시)
     */
    @Cacheable(
            value = CacheType.Values.MEMBER_STRATEGIES,
            key = CacheType.Keys.MEMBER_UUID
    )
    public List<MemberResponse.StrategyDetailResponse> getStrategies(String uuid) {
        Member member = memberRepository.findByUuid(uuid)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND_MEMBER));

        List<Strategy> strategies = strategyRepository.findAllByMember(member);

        return strategies.stream()
                .map(StrategyConverter::toDetailResponse)
                .collect(Collectors.toList());
    }

    /**
     * 전략 캐시 무효화
     */
    @CacheEvict(
            value = CacheType.Values.MEMBER_STRATEGIES,
            key = CacheType.Keys.MEMBER_UUID
    )
    public void evictStrategies(String uuid) {
        // 캐시만 삭제
    }
}
