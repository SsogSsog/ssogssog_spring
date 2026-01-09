package org.project.ssogssog.presentation.controller.member;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.application.service.member.api.MemberService;
import org.project.ssogssog.application.service.member.api.dto.MemberRequest;
import org.project.ssogssog.application.service.member.api.dto.MemberResponse;
import org.project.ssogssog.global.payload.ApiResponse;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    /**
     * 앱 실행 시 (또는 켤 때마다) 호출
     * 역할: DB에 UUID가 없으면 등록, 있으면 FCM 토큰 갱신
     */
    @PostMapping("/register")
    @Operation(
            summary = "기기(UUID) 기반 회원 등록 및 로그인",
            description = """
            앱 실행 시점에 호출하여 사용자를 식별합니다.
            전통적인 ID/PW 로그인 대신, 클라이언트가 생성한 UUID를 식별자로 사용하는 '비로그인' 방식을 처리합니다.

            - 동작 로직
            1. 신규 사용자 (INSERT): DB에 해당 UUID가 존재하지 않으면 신규 회원으로 등록
            2. *기존 사용자 (UPDATE): 이미 존재하는 UUID라면, 요청된 최신 FCM 토큰으로 정보를 갱신

            - 필수 값: uuid (최대 36자)
            - 선택 값: fcm -> 알림 수신을 위함
            """
    )
    public ApiResponse<MemberResponse.RegisterResponse> register(
            @RequestBody
            @Valid
            MemberRequest.RegisterRequest request
    ) {

        return ApiResponse.onSuccess(memberService.register(request));

    }














}
