package org.project.ssogssog.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.ssogssog.application.service.member.api.MemberService;
import org.project.ssogssog.application.service.member.api.dto.MemberRequest;
import org.project.ssogssog.application.service.member.api.dto.MemberResponse;
import org.project.ssogssog.domain.member.entity.Member;
import org.project.ssogssog.domain.member.entity.Strategy;
import org.project.ssogssog.domain.member.repository.MemberRepository;
import org.project.ssogssog.domain.member.repository.StrategyRepository;
import org.project.ssogssog.global.payload.exception.GeneralException;

import java.util.Optional;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StrategyRepository strategyRepository;

    @InjectMocks
    private MemberService memberService;

    private Member testMember;
    private MemberRequest.StrategyRequest testRequest;

    @BeforeEach
    void setUp() {
        // 테스트용 Member 생성
        testMember = new Member("test-uuid-1234", "fcm-token");

        // 테스트용 Request 생성
        testRequest = MemberRequest.StrategyRequest.builder()
                .stockPriceRange(null)
                .marketCapBucket(null)
                .per(null)
                .roe(null)
                .build();
    }

    @Test
    @DisplayName("전략 저장 성공")
    void saveStrategy_success() {
        // Given
        String uuid = "test-uuid-1234";

        given(memberRepository.findByUuid(uuid))
                .willReturn(Optional.of(testMember));

        given(strategyRepository.countByMember(testMember))
                .willReturn(2);

        // save() 호출 시 그대로 반환
        given(strategyRepository.save(any(Strategy.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        MemberResponse.StrategyResponse response =
                memberService.saveStrategy(uuid, testRequest);

        // Then
        // ID는 검증하지 않고, 다른 부분만 검증
        assertThat(response).isNotNull();
        assertThat(response.getStrategyName()).isEqualTo("전략3");

        // save 메서드가 호출되었는지만 확인
        verify(strategyRepository, times(1)).save(any(Strategy.class));
    }

    @Test
    @DisplayName("회원을 찾을 수 없으면 예외 발생")
    void saveStrategy_memberNotFound_throwsException() {
        // Given
        String uuid = "non-existent-uuid";

        given(memberRepository.findByUuid(uuid))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                memberService.saveStrategy(uuid, testRequest)
        )
                .isInstanceOf(GeneralException.class);

    }

    @Test
    @DisplayName("전략 5개 초과 시 예외 발생")
    void saveStrategy_exceedsLimit_throwsException() {
        // Given
        String uuid = "test-uuid-1234";

        given(memberRepository.findByUuid(uuid))
                .willReturn(Optional.of(testMember));

        given(strategyRepository.countByMember(testMember))
                .willReturn(5);  // 이미 5개

        // When & Then
        assertThatThrownBy(() ->
                memberService.saveStrategy(uuid, testRequest)
        )
                .isInstanceOf(GeneralException.class);

        // 검증: save는 호출되지 않아야 함
        verify(strategyRepository, times(0)).save(any(Strategy.class));
    }

    @Test
    @DisplayName("첫 번째 전략 저장 시 이름은 '전략1'")
    void saveStrategy_firstStrategy_nameIsStrategy1() {
        // Given
        String uuid = "test-uuid-1234";

        given(memberRepository.findByUuid(uuid))
                .willReturn(Optional.of(testMember));

        given(strategyRepository.countByMember(testMember))
                .willReturn(0);  // 전략 0개

        given(strategyRepository.save(any(Strategy.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        MemberResponse.StrategyResponse response =
                memberService.saveStrategy(uuid, testRequest);

        // Then
        assertThat(response.getStrategyName()).isEqualTo("전략1");
    }

}