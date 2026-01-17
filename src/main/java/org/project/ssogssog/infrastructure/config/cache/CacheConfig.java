package org.project.ssogssog.infrastructure.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        // Enum의 모든 설정을 읽어서 캐시 생성
        List<CaffeineCache> caches = Arrays.stream(CacheType.values())
                .map(this::buildCache)
                .collect(Collectors.toList());

        cacheManager.setCaches(caches);

        // 설정된 캐시 로깅
        log.info("=== 캐시 설정 완료 ===");
        Arrays.stream(CacheType.values()).forEach(type ->
                log.info("  - {}: TTL={}분, 최대크기={}개",
                        type.getDescription(),
                        type.getTimeUnit().toMinutes(type.getDuration()),
                        type.getMaxSize())
        );

        return cacheManager;
    }

    /**
     * 개별 캐시 생성 헬퍼 메서드
     */
    private CaffeineCache buildCache(CacheType cacheType) {
        return new CaffeineCache(
                cacheType.getCacheName(),
                Caffeine.newBuilder()
                        .expireAfterWrite(cacheType.getDuration(), cacheType.getTimeUnit())
                        .maximumSize(cacheType.getMaxSize())
                        .recordStats()
                        .build()
        );
    }
}