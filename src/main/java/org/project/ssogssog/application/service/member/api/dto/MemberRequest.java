package org.project.ssogssog.application.service.member.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MemberRequest {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotBlank(message = "uuid는 필수입니다.")
        @Size(max = 36, message = "uuid는 최대 36자까지 가능합니다.") // TODO 추후 @Pattern 방식으로 변경하기
        private String uuid;
        private String fcm;
    }

}
