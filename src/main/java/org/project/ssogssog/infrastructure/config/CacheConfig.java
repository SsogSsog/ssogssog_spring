package org.project.ssogssog.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, String> kisTokenCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(12))
                .maximumSize(1)
                .build();
    }
}
