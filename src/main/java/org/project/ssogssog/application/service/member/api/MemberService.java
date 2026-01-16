package org.project.ssogssog.application.service.member.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.member.api.dto.MemberRequest;
import org.project.ssogssog.application.service.member.api.dto.MemberResponse;
import org.project.ssogssog.domain.member.entity.Member;
import org.project.ssogssog.domain.member.entity.StockLike;
import org.project.ssogssog.domain.member.entity.Strategy;
import org.project.ssogssog.application.service.member.api.converter.StrategyConverter;
import org.project.ssogssog.domain.member.factory.StrategyFactory;
import org.project.ssogssog.domain.member.repository.StockLikeRepository;
import org.project.ssogssog.domain.member.repository.MemberRepository;
import org.project.ssogssog.domain.member.repository.StrategyRepository;
import org.project.ssogssog.domain.stock.entity.Stock;
import org.project.ssogssog.domain.stock.repository.StockRepository;
import org.project.ssogssog.global.payload.code.status.ErrorStatus;
import org.project.ssogssog.global.payload.exception.GeneralException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final StrategyRepository strategyRepository;
    private final StockLikeRepository stockLikeRepository;
    private final StockRepository stockRepository;

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

        List<Strategy> strategies = strategyRepository.findAllByMember(member);
        //TODO 동시 요청 고려하기
        if (strategies.size() >= MAX_STRATEGY_COUNT) {
            throw new GeneralException(ErrorStatus.STRATEGY_LIMIT_EXCEEDED);
        }

        int maxNumber = strategies.stream()
                .map(s -> extractNumber(s.getStrategyName()))
                .max(Integer::compareTo)
                .orElse(0);

        String strategyName = STRATEGY_NAME + (maxNumber + 1);

        // 생성
        Strategy savedStrategy = StrategyFactory.createFrom(member, strategyName, request);

        // 저장
        strategyRepository.save(savedStrategy);

        return MemberResponse.StrategyResponse.builder()
                .strategyId(savedStrategy.getId())
                .strategyName(savedStrategy.getStrategyName())
                .build();
    }

    @Transactional(readOnly = true)
    public MemberResponse.StrategiesResponse getStrategies(String uuid) {

        Member member = memberRepository.findByUuid(uuid)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND_MEMBER));

        List<Strategy> strategies = strategyRepository.findAllByMember(member);

        List<MemberResponse.StrategyDetailResponse> strategyDetails = strategies.stream()
                .map(StrategyConverter::toDetailResponse)
                .collect(Collectors.toList());

        return MemberResponse.StrategiesResponse.builder()
                .strategies(strategyDetails)
                .build();
    }

    @Transactional
    public void deleteStrategy(String uuid, Long strategyId) {

        Member member = memberRepository.findByUuid(uuid)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND_MEMBER));

        Strategy strategy = strategyRepository.findByIdAndMember(strategyId, member)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND_STRATEGY));

        strategyRepository.delete(strategy);
    }

    private int extractNumber(String strategyName) {
        try {
            return Integer.parseInt(strategyName.replace(STRATEGY_NAME, ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Transactional
    public MemberResponse.LikeResponse toggleLike(String uuid, Long stockId) {

        Member member = memberRepository.findByUuid(uuid)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND_MEMBER));

        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND_STOCK));

        Optional<StockLike> existingLike = stockLikeRepository.findByMemberAndStock(member, stock);

        boolean liked;
        if (existingLike.isPresent()) {
            stockLikeRepository.delete(existingLike.get());
            liked = false;
        } else {
            stockLikeRepository.save(StockLike.of(member, stock));
            liked = true;
        }

        return MemberResponse.LikeResponse.builder()
                .stockId(stockId)
                .liked(liked)
                .build();
    }

}
