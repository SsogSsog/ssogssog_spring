package org.project.ssogssog.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 작업용 ThreadPoolTaskExecutor 설정
 *
 * ThreadPoolTaskExecutor vs ExecutorService:
 * - Spring이 자동으로 라이프사이클 관리 (shutdown 자동 처리)
 * - @PreDestroy 불필요
 * - Actuator 메트릭 연동 용이
 * - 더 세밀한 설정 가능 (corePoolSize, maxPoolSize, queueCapacity 등)
 *
 * 스레드 풀 크기 계산:
 * - OpenDART RateLimiter: 5 req/s
 * - TimeLimiter: 3초
 * - 최대 동시 대기 요청: 5 * 3 = 15개
 * - corePoolSize: 10 (기본 상주 스레드)
 * - maxPoolSize: 15 (최대 스레드, 큐가 꽉 차면 여기까지 확장)
 * - queueCapacity: 100 (대기열, 스케줄러 배치 처리용 버퍼)
 */
@Slf4j
@Configuration
public class AsyncConfig {

    /**
     * OpenDART API 호출용 전용 스레드 풀
     * - Spring이 자동으로 shutdown 처리
     * - 스레드 이름 prefix로 디버깅 용이
     */
    @Bean(name = "openDartApiExecutor")
    public Executor openDartApiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(15);      // 기본 스레드 수
        executor.setMaxPoolSize(15);       // 최대 스레드 수
        executor.setThreadNamePrefix("OpenDart-");  // 로그에서 식별 쉬움

        // 종료 시 진행 중인 작업 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);

        executor.initialize();

        log.info("OpenDART API ThreadPoolTaskExecutor 생성 완료 (core: {}, max: {}, queue: {})",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }
}
