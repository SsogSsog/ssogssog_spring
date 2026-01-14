package org.project.ssogssog.infrastructure.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.project.ssogssog.global.payload.code.status.ErrorStatus;
import org.project.ssogssog.global.payload.utils.ErrorResponseWriter;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UuidInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String uuid = request.getHeader("X-User-ID");

        if (uuid == null || uuid.isBlank()) {
            ErrorResponseWriter.setErrorResponse(response, ErrorStatus.INVALID_AUTH_HEADER);
            return false;
        }


        // UUID 형식 검증
        if (!isValidUUID(uuid)) {
            ErrorResponseWriter.setErrorResponse(response, ErrorStatus.INVALID_UUID);
            return false;
        }

        // 검증된 UUID를 request attribute에 저장하여 다운스트림에서 사용 가능하도록 함
        request.setAttribute("memberUuId", uuid);

        return true;
    }

    // TODO pattern 도입하기
    // UUID 형식 검증 로직
    private boolean isValidUUID(String uuid) {
        try {
            java.util.UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}