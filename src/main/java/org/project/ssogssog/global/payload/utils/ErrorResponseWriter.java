package org.project.ssogssog.global.payload.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.project.ssogssog.global.payload.ApiResponse;
import org.project.ssogssog.global.payload.code.status.ErrorStatus;

import java.io.IOException;

public class ErrorResponseWriter {

    public static void setErrorResponse(HttpServletResponse response, ErrorStatus errorStatus) throws IOException {

        try{
            // HTTP 상태 코드 설정
            response.setStatus(errorStatus.getHttpStatus().value()); // 또는 e.getStatusCode()
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Object> apiResponse = ApiResponse.onFailure(errorStatus, null);

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = objectMapper.writeValueAsString(apiResponse);

            // 응답 출력
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
        }catch (IOException ioException){
            throw new RuntimeException(ioException);
        }
    }
}
