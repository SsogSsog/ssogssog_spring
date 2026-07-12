# pgvector 연결 & 벡터 검색 — 복습 노트

- 작성일: 2026-07-13
- 커리큘럼 위치: RAG 3주 커리큘럼 **Week 2 Day 2 "pgvector 세팅"**
- 실습 파일: `src/test/java/org/project/ssogssog/ai/PgVectorStoreTest.java` (학습용)
- 목적: 기존 MySQL을 건드리지 않고 벡터 전용 DB(pgvector)를 추가, 문서 저장·유사도 검색 검증.

---

## 0. 오늘의 검증 결과

pgvector에 문서 3개 저장 → "삼성전자 반도체 이익"으로 유사도 검색(topK=2):

| 순위 | 결과 | 판정 |
|---|---|---|
| 1 | [공시A] 삼성전자가 반도체 실적 호조로 영업이익이 크게 늘었다 | ✅ 가장 관련 높음 |
| 2 | [공시B] 현대차가 전기차 판매 증가로 매출이 성장했다 | 산업(실적) 맥락 유사 |
| — | [잡담] 오늘 점심 김치찌개 | ❌ 걸러짐 (topK 밖) |

→ **의미 기반 검색이 실제로 작동.** 질문과 글자가 안 겹쳐도("이익" vs "영업이익") 의미로 찾아냄.

---

## 1. 아키텍처 결정: MySQL은 그대로, pgvector는 별도 DataSource

이 프로젝트는 MySQL이 **주 DataSource**(Spring Boot 자동설정). 여기에 pgvector(PostgreSQL)를 추가하면 DataSource가 2개가 된다.

- **Spring AI pgvector 자동설정은 "주 DataSource = PostgreSQL"이라고 가정** → MySQL이 주인 우리 프로젝트에선 그대로 쓰면 깨짐.
- **해결**: PG 전용 DataSource를 `@Bean`으로 별도 생성 → 그 JdbcTemplate으로 `PgVectorStore`를 수동 등록. MySQL 계층은 안 건드림.
- 파일: `infrastructure/config/PgVectorConfig.java`

**"MySQL→PG 통합" 유혹은 토끼굴.** 벡터 전용 DB 하나 추가로 끝. 데이터 특성이 다르므로(정형 지표 vs 벡터) 저장소를 분리하는 게 정답.

---

## 2. 핵심 함정 3개 (실무에서 반드시 만남)

### 함정 A — HNSW 인덱스는 최대 2000차원
```
ERROR: column cannot have more than 2000 dimensions for hnsw index
```
- `gemini-embedding-001`의 기본 출력은 **3072차원** → HNSW 인덱스 생성 불가.
- **해결**: 임베딩 차원을 **1536으로 축소**. Gemini 임베딩은 Matryoshka 방식이라 차원을 잘라도 의미가 보존됨. 저장 공간·검색 속도도 이득.
- 설정 2곳을 맞춰야 함: 임베딩 모델 옵션 `dimensions: 1536` + `PgVectorStore.builder().dimensions(1536)`.
- 실무 함의: "임베딩 차원은 벡터 DB 인덱스 제약과 함께 결정한다"는 운영 포인트. 포폴 재료.

### 함정 B — 빈 이름 충돌 (BeanDefinitionOverrideException)
- 수동 `vectorStore` 빈과 자동설정의 `vectorStore` 빈 이름이 같아 override 예외.
- **해결**: 메인 클래스에서 자동설정 제외.
  `@SpringBootApplication(exclude = PgVectorStoreAutoConfiguration.class)`
  → 수동 빈으로 관리한다는 의도가 코드에 드러나고, 충돌도 사라짐.

### 함정 C — HikariCP는 `url`이 아니라 `jdbc-url`
- `DataSourceBuilder` + Hikari 조합에서는 프로퍼티가 `jdbc-url`.
- `pgvector.datasource.url`로 적으면 접속 실패. `jdbc-url`로 적어야 함.

---

## 3. 인프라 (docker-compose)

```yaml
pgvector:
  image: pgvector/pgvector:pg16   # arm64(라즈베리파이/애플실리콘) 지원
  ports:
    - "5433:5432"                 # 호스트 5432가 네이티브 PG와 충돌 → 5433
  environment:
    POSTGRES_DB: ssogssog_vector
    POSTGRES_USER: ${PGVECTOR_USER:-vectoruser}
    POSTGRES_PASSWORD: ${PGVECTOR_PASSWORD:-vectorpass}   # 로컬 개발용 기본값(env 우선)
```
- `vector` 익스텐션은 `initialize-schema: true`면 자동 생성(수동으로도 `CREATE EXTENSION vector` 확인함, 버전 0.8.5).
- **포트 충돌 주의**: 로컬에 네이티브 PostgreSQL이 이미 5432를 쓰고 있으면 호스트 포트를 바꿔야 함.

---

## 4. Spring AI VectorStore 핵심 API

```java
vectorStore.add(List.of(new Document("본문", Map.of("source", "공시A"))));  // 저장(임베딩 자동)

vectorStore.similaritySearch(
    SearchRequest.builder().query("질문").topK(2).build());               // 유사도 검색
```
- `add()` 시 텍스트 임베딩을 Spring AI가 자동으로 해서 벡터로 저장. 개발자가 임베딩 호출을 직접 안 함.
- `Document`는 본문 + metadata(Map). metadata로 나중에 필터링(종목/날짜) 가능 → W2 Day5.

---

## 트러블슈팅 순서 요약 (오늘 실제로 밟은 경로)
1. docker pgvector 기동 → 포트 5432 충돌 → **5433으로 변경**.
2. 의존성 추가(`spring-ai-starter-vector-store-pgvector` + postgresql).
3. 수동 config 작성 → **빈 이름 충돌** → 자동설정 exclude.
4. 실행 → **3072차원 HNSW 인덱스 불가** → dimensions 1536으로 축소, 테이블 drop 후 재생성.
5. 통과. add/search 검증 + pgvector 직접 조회로 1536차원 저장 확인.

+ 곁다리로 발견: `application-dev.yml`의 `ai:` 블록이 `management:` 하위로 잘못 들여쓰기 되어 있었음(→ `spring.ai`로 이동). 그동안 env 주입으로 가려져 있던 버그.
