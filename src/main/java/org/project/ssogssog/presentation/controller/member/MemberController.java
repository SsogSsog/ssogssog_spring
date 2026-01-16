package org.project.ssogssog.presentation.controller.member;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.application.service.member.api.MemberService;
import org.project.ssogssog.application.service.member.api.dto.MemberRequest;
import org.project.ssogssog.application.service.member.api.dto.MemberResponse;
import org.project.ssogssog.global.payload.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping("/strategies")
    @Operation(
            summary = "투자 전략 저장",
            description = """
            회원의 투자 전략을 저장합니다.

            - 헤더의 X-User-ID로 회원을 식별합니다
            - 전략 이름은 자동 생성됩니다 (전략1, 전략2, ...)
            - 회원당 최대 5개의 전략을 생성할 수 있습니다
            - 각 조건은 선택적이며, 설정하지 않은 조건은 필터링에서 제외됩니다
            """,
            parameters = {
                    @Parameter(
                            name = "X-User-ID",
                            description = "회원 UUID (예: 550e8400-e29b-41d4-a716-446655440000)",
                            in = ParameterIn.HEADER,
                            required = true
                    )
            }
    )
    public ApiResponse<MemberResponse.StrategyResponse> saveStrategy(
            HttpServletRequest httpRequest,
            @RequestBody MemberRequest.StrategyRequest request
    ) {
        String uuid = (String) httpRequest.getAttribute("memberUuId");
        return ApiResponse.onSuccess(memberService.saveStrategy(uuid, request));
    }

    @GetMapping("/strategies")
    @Operation(
            summary = "내 투자 전략 목록 조회",
            description = """
            회원의 모든 투자 전략을 조회합니다.

            - 헤더의 X-User-ID로 회원을 식별합니다
            - 회원이 저장한 모든 전략의 상세 정보를 반환합니다
            """,
            parameters = {
                    @Parameter(
                            name = "X-User-ID",
                            description = "회원 UUID (예: 550e8400-e29b-41d4-a716-446655440000)",
                            in = ParameterIn.HEADER,
                            required = true
                    )
            }
    )
    public ApiResponse<MemberResponse.StrategiesResponse> getStrategies(
            HttpServletRequest httpRequest
    ) {
        String uuid = (String) httpRequest.getAttribute("memberUuId");
        return ApiResponse.onSuccess(memberService.getStrategies(uuid));
    }

    @DeleteMapping("/strategies/{strategyId}")
    @Operation(
            summary = "투자 전략 삭제",
            description = """
            회원의 투자 전략을 삭제합니다.

            - 헤더의 X-User-ID로 회원을 식별합니다
            - 본인의 전략만 삭제할 수 있습니다
            """,
            parameters = {
                    @Parameter(
                            name = "X-User-ID",
                            description = "회원 UUID (예: 550e8400-e29b-41d4-a716-446655440000)",
                            in = ParameterIn.HEADER,
                            required = true
                    ),
                    @Parameter(
                            name = "strategyId",
                            description = "삭제할 전략 ID",
                            in = ParameterIn.PATH,
                            required = true
                    )
            }
    )
    public ApiResponse<Void> deleteStrategy(
            HttpServletRequest httpRequest,
            @PathVariable Long strategyId
    ) {
        String uuid = (String) httpRequest.getAttribute("memberUuId");
        memberService.deleteStrategy(uuid, strategyId);
        return ApiResponse.onSuccess(null);
    }

    @PostMapping("/likes/{stockId}")
    @Operation(
            summary = "주식 좋아요 설정/해제",
            description = """
            특정 주식의 좋아요를 토글합니다.

            - 좋아요가 없으면 설정, 있으면 해제됩니다
            - 헤더의 X-User-ID로 회원을 식별합니다
            """,
            parameters = {
                    @Parameter(
                            name = "X-User-ID",
                            description = "회원 UUID (예: 550e8400-e29b-41d4-a716-446655440000)",
                            in = ParameterIn.HEADER,
                            required = true
                    ),
                    @Parameter(
                            name = "stockId",
                            description = "좋아요할 주식 ID",
                            in = ParameterIn.PATH,
                            required = true
                    )
            }
    )
    public ApiResponse<MemberResponse.LikeResponse> toggleLike(
            HttpServletRequest httpRequest,
            @PathVariable Long stockId
    ) {
        String uuid = (String) httpRequest.getAttribute("memberUuId");
        return ApiResponse.onSuccess(memberService.toggleLike(uuid, stockId));
    }







}
