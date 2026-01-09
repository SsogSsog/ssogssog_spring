package org.project.ssogssog.application.service.member.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.ssogssog.application.service.member.api.dto.MemberRequest;
import org.project.ssogssog.application.service.member.api.dto.MemberResponse;
import org.project.ssogssog.domain.member.entity.Member;
import org.project.ssogssog.domain.member.repository.MemberRepository;
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
            if (fcmToken != null) {
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

}
