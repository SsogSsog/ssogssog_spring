package org.project.ssogssog.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.project.ssogssog.infrastructure.interceptor.UuidInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final UuidInterceptor uuidInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(uuidInterceptor)
                //.addPathPatterns("/**") // 모든 API 검사
                //.excludePathPatterns("/members/register"); // 등록 API는 검사 제외
                .addPathPatterns("/members/**") // 개발, 테스트 편의성을 member 관련만 인증
                .addPathPatterns("/stock/*/liked") // 주식 좋아요 여부 조회
                .excludePathPatterns("/members/register");
    }
}
