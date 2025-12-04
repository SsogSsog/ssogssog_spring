package org.project.ssogssog.presentation.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.project.ssogssog.application.common.response.status.BaseCode;
import org.project.ssogssog.application.common.response.status.BaseErrorCode;
import org.project.ssogssog.application.common.response.status.SuccessStatus;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess;
    private final String code;
    private final String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;

    public static <T> ApiResponse<T> of(BaseCode code, T result){
        return new ApiResponse<>(true, code.getReasonHttpStatus().getCode() , code.getReasonHttpStatus().getMessage(), result);
    }

    // 성공한 경우 응답 생성
    public static <T> ApiResponse<T> onSuccess(T result){
        return new ApiResponse<>(true, SuccessStatus._OK.getCode(),
                SuccessStatus._OK.getMessage(), result);
    }

    public static <T> ApiResponse<T> onSuccess(BaseCode successCode, T result){
        return new ApiResponse<>(true, successCode.getReasonHttpStatus().getCode(),
                successCode.getReasonHttpStatus().getMessage(), result);
    }

    // 실패한 경우 응답 생성
    public static <T> ApiResponse<T> onFailure(BaseErrorCode errorCode, T data){
        return new ApiResponse<>(false, errorCode.getReasonHttpStatus().getCode(), errorCode.getReasonHttpStatus().getMessage(), data);
    }

    // ErrorReasonDTO를 위한 전용 onFailure 오버로딩
    public static <T> ApiResponse<T> onFailure(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }
}