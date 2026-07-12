package org.project.ssogssog;

import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

// pgvector는 PgVectorConfig에서 수동 빈으로 관리하므로 자동설정 제외(빈 이름 충돌 방지)
@SpringBootApplication(exclude = PgVectorStoreAutoConfiguration.class)
@EnableJpaAuditing
@EnableScheduling
@EnableFeignClients(basePackages = "org.project.ssogssog.infrastructure.client.feign")
public class SsogssogApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsogssogApplication.class, args);
    }

}
