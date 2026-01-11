package org.project.ssogssog.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;


@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
                // 랭킹: 1시간 TTL (실시간 데이터)
                buildCache("stockRanking", 1, TimeUnit.HOURS, 100),

                // 뉴스: 8시간 TTL (API 제한 고려)
                buildCache("stockNews", 8, TimeUnit.HOURS, 300_000), // 현재 주식 개수 기준

                // 공시: 8시간 TTL (API 제한 고려)
                buildCache("stockDisclosures", 8, TimeUnit.HOURS, 300_000) // 현재 주식 개수 기준
        ));

        return cacheManager;
    }

    /**
     * 개별 캐시 생성 헬퍼 메서드
     */
    private CaffeineCache buildCache(String name, long duration, TimeUnit unit, long maxSize) {
        return new CaffeineCache(
                name,
                Caffeine.newBuilder()
                        .expireAfterWrite(duration, unit)
                        .maximumSize(maxSize)
                        .recordStats()
                        .build()
        );
    }
}