package org.project.ssogssog.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * RAG 공시 임베딩 저장용 pgvector 설정.
 *
 * <p>기존 MySQL(주 DataSource, 자동설정)은 그대로 두고, PostgreSQL(pgvector)용
 * DataSource를 별도로 만들어 {@link PgVectorStore}에 명시적으로 연결한다.
 * Spring AI의 pgvector 자동설정은 주 DataSource가 PostgreSQL이라고 가정하므로,
 * MySQL이 주인 이 프로젝트에서는 수동 빈으로 격리해야 안전하다.
 *
 * <p>임베딩 차원은 gemini-embedding-001 기준 3072.
 */
@Configuration
public class PgVectorConfig {

    /**
     * pgvector 전용 DataSource. application.yml의 {@code pgvector.datasource.*}를 매핑한다.
     * 주 DataSource(MySQL)와 격리되도록 @Qualifier로 구분한다.
     */
    @Bean
    @Qualifier("pgVectorDataSource")
    @ConfigurationProperties(prefix = "pgvector.datasource")
    public DataSource pgVectorDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    @Qualifier("pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate(@Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) {
        return new JdbcTemplate(pgVectorDataSource);
    }

    /**
     * 공시 임베딩용 VectorStore. 이 빈을 직접 등록하므로 Spring AI pgvector 자동설정은 백오프된다.
     */
    @Bean
    public VectorStore vectorStore(
            @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate,
            EmbeddingModel embeddingModel
    ) {
        return PgVectorStore.builder(pgVectorJdbcTemplate, embeddingModel)
                .dimensions(1536)                                        // gemini-embedding-001을 1536으로 축소 (HNSW 최대 2000차원 제약)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(true)                                  // vector_store 테이블/인덱스 자동 생성
                .build();
    }
}
