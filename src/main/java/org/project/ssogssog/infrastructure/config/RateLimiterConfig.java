package org.project.ssogssog.infrastructure.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Qualifier("kisRateLimiter")
    public RateLimiter kisRateLimiter(
            @Value("${kis.rate-limit.permits-per-second}") double permitsPerSecond
    ) {
        return RateLimiter.create(permitsPerSecond);
    }

    @Bean
    @Qualifier("naverSearchRateLimiter")
    public RateLimiter naverSearchRateLimiter(
            @Value("${naver-search.rate-limit.permits-per-second}") double permitsPerSecond
    ) {
        return RateLimiter.create(permitsPerSecond);
    }

    @Bean
    @Qualifier("openDartRateLimiter")
    public RateLimiter openDartRateLimiter(
            @Value("${opendart.rate-limit.permits-per-second}") double permitsPerSecond
    ) {
        return RateLimiter.create(permitsPerSecond);
    }
}