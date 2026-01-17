package org.project.ssogssog.global.payload.code.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.project.ssogssog.global.payload.code.dto.ErrorReasonDTO;
import org.project.ssogssog.global.payload.code.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    // 가장 일반적인 응답
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON401","인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),

    // 유효성 검사 에러(메시지는 @interface의 message로 처리)
    VALIDATOR_ERROR(HttpStatus.BAD_REQUEST,"VALID400",null),

    // Stock 에러
    NOT_FOUND_STOCK(HttpStatus.NOT_FOUND,"STOCK4000","해당 주식이 존재하지 않습니다"),

    // StockMetric 에러
    INVALID_METRIC_BASED_PERIOD(HttpStatus.BAD_REQUEST, "METRIC4100", "직전 분기 또는 작년 중 분기 선택이 필요합니다."),

    // DailyPrice 에러
    NOT_FOUND_DAILY_PRICE(HttpStatus.NOT_FOUND, "DAILY_PRICE4000", "해당 주식의 일별시세가 존재하지 않습니다"),

    // Member 에러
    NOT_EMPTY_UUID(HttpStatus.BAD_REQUEST, "MEMBER4100", "로그인 요청 형식이 잘못됐습니다"),
    INVALID_UUID(HttpStatus.BAD_REQUEST, "MEMBER4101", "UUID 형식이 잘못 되었습니다"),
    NOT_FOUND_MEMBER(HttpStatus.NOT_FOUND, "MEMBER4102", "해당 회원이 존재하지 않습니다"),

    // Strategy 에러
    NOT_FOUND_STRATEGY(HttpStatus.NOT_FOUND, "STRATEGY4000", "해당 전략이 존재하지 않습니다"),
    STRATEGY_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "STRATEGY4100", "전략은 최대 5개까지 생성할 수 있습니다"),

    // Auth 에러
    INVALID_AUTH_HEADER(HttpStatus.UNAUTHORIZED, "AUTH4000", "인증 헤더가 잘못되었습니다"),

    // KIS 에러
    BODY_NULL(HttpStatus.INTERNAL_SERVER_ERROR, "KIS4000", "KIS Body가 비어있습니다."),
    FAIL_RT_CD(HttpStatus.INTERNAL_SERVER_ERROR, "KIS4001", "RT_CD 값을 받아오는 것에 실패했습니다"),
    KIS_HTTP_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "KIS4100", "KIS HTTP 에러가 발생했습니다."),
    KIS_OTHERS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "KIS4200", "KIS unknown 에러가 발생했습니다."),


    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }
    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build()
                ;
    }
}