package org.project.ssogssog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableFeignClients(basePackages = "org.project.ssogssog.infrastructure.client.feign")
public class SsogssogApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsogssogApplication.class, args);
    }

}
