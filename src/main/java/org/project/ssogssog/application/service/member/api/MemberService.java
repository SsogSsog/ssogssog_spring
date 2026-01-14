package org.project.ssogssog.application.service.member.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.common.dto.condition.GrowthConditionDTO;
import org.project.ssogssog.application.common.dto.condition.RangeConditionDTO;
import org.project.ssogssog.application.service.member.api.dto.MemberRequest;
import org.project.ssogssog.application.service.member.api.dto.MemberResponse;
import org.project.ssogssog.domain.member.entity.Member;
import org.project.ssogssog.domain.member.entity.Strategy;
import org.project.ssogssog.domain.member.entity.range.GrowthRangeCondition;
import org.project.ssogssog.domain.member.entity.range.RangeCondition;
import org.project.ssogssog.domain.member.repository.MemberRepository;
import org.project.ssogssog.domain.member.repository.StrategyRepository;
import org.project.ssogssog.domain.stockmetric.enums.MetricBasePeriod;
import org.project.ssogssog.global.payload.code.status.ErrorStatus;
import org.project.ssogssog.global.payload.exception.GeneralException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final StrategyRepository strategyRepository;

    @Transactional
    public MemberResponse.RegisterResponse register(MemberRequest.RegisterRequest request) {

        String uuid = request.getUuid();
        String fcmToken = request.getFcm();

        if (uuid == null || uuid.isBlank()) {
            throw new GeneralException(ErrorStatus.NOT_EMPTY_UUID);
        }

        Optional<Member> existingMember = memberRepository.findByUuid(uuid);
        Member member;

        if (existingMember.isPresent()) {
            // 이미 있는 회원이면 -> FCM 토큰만 최신으로 업데이트
            member = existingMember.get();
            if (fcmToken != null && !fcmToken.isBlank()) {
                member.updateFcmToken(fcmToken);
                memberRepository.save(member);
            }
        } else {
            // 없는 회원이면 -> 신규 등록 (회원가입 효과)
            member = new Member(uuid, fcmToken);
            memberRepository.save(member);
        }

        return MemberResponse.RegisterResponse.builder()
                .memberId(member.getId())
                .build();
    }

    private static final int MAX_STRATEGY_COUNT = 5;
    private static final String STRATEGY_NAME = "전략";

    @Transactional
    public MemberResponse.StrategyResponse saveStrategy(String uuid, MemberRequest.StrategyRequest request) {

        Member member = memberRepository.findByUuid(uuid)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND_MEMBER));

        int currentStrategyCount = strategyRepository.countByMember(member);
        if (currentStrategyCount >= MAX_STRATEGY_COUNT) {
            throw new GeneralException(ErrorStatus.STRATEGY_LIMIT_EXCEEDED);
        }

        String strategyName = STRATEGY_NAME + (currentStrategyCount + 1);

        Strategy strategy = Strategy.builder()
                .member(member)
                .strategyName(strategyName)
                .stockPriceRange(request.getStockPriceRange())
                .marketCapBucket(request.getMarketCapBucket())
                .per(toRangeCondition(request.getPer()))
                .roe(toRangeCondition(request.getRoe()))
                .debtRatio(toRangeCondition(request.getDebtRatio()))
                .operatingProfitMargin(toRangeCondition(request.getOperatingProfitRatio()))
                .salesGrowthQoQ(toGrowthRangeCondition(request.getSalesGrowthRatio(), MetricBasePeriod.PREV_QUARTER))
                .salesGrowthYoY(toGrowthRangeCondition(request.getSalesGrowthRatio(), MetricBasePeriod.PREV_YEAR))
                .netProfitGrowthQoQ(toGrowthRangeCondition(request.getNetProfitGrowthRatio(), MetricBasePeriod.PREV_YEAR))
                .netProfitGrowthYoY(toGrowthRangeCondition(request.getNetProfitGrowthRatio(), MetricBasePeriod.PREV_YEAR))
                .dividendYield(toRangeCondition(request.getDividendYieldRatio()))
                .foreignOwnershipRate(toRangeCondition(request.getForeignOwnershipRate()))
                .build();

        Strategy savedStrategy = strategyRepository.save(strategy);

        return MemberResponse.StrategyResponse.builder()
                .strategyId(savedStrategy.getId())
                .strategyName(savedStrategy.getStrategyName())
                .build();
    }

    private RangeCondition toRangeCondition(RangeConditionDTO dto) {
        if (dto == null) {
            return null;
        }
        return RangeCondition.of(dto.getMin(), dto.getMax());
    }

    private GrowthRangeCondition toGrowthRangeCondition(
            GrowthConditionDTO dto,
            MetricBasePeriod expectedPeriod
    ) {
        if (dto == null || dto.getBasePeriod() != expectedPeriod) {
            return null;
        }
        return GrowthRangeCondition.builder()
                .min(dto.getMin())
                .max(dto.getMax())
                .basePeriod(dto.getBasePeriod())
                .build();
    }
}
