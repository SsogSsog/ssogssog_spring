# AI 프롬프트 설계 (`POST /ai/ask`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 주식쏙쏙 도메인 시스템 프롬프트를 리소스 파일로 주입하고, 요청별 temperature를 지원하는 `POST /ai/ask` 엔드포인트를 추가한다.

**Architecture:** 기존 헥사고날 레이아웃과 Day1 패턴을 따른다. 컨트롤러(얇음) → `AiAskService`(질문 검증 + temperature resolve + 시스템 프롬프트/질문/옵션 조립 → `ChatClient` 호출). 시스템 프롬프트는 `.st` 리소스 파일에서 읽는다. 남용 방지를 위해 컨트롤러는 `@Profile("!prod")`로 제한하고 질문 길이를 제한한다.

**Tech Stack:** Java 21, Spring Boot 3.5.8, Spring AI 1.0.0 (`spring-ai-starter-model-openai`, Gemini OpenAI 호환 엔드포인트), JUnit 5 + Mockito + AssertJ.

## Global Constraints

- 패키지 루트: `org.project.ssogssog`
- 컨트롤러는 `ApiResponse<T>` 반환, JPA 엔티티 노출 금지.
- 에러는 `GeneralException` + `ErrorStatus`(`global/payload/code/status/ErrorStatus.java`)를 통해 처리.
- 값 객체·DTO는 `record` 우선.
- Swagger `@Operation`/`@Schema`, DTO 필드 설명, 코드 주석은 한국어.
- 커밋 메시지: `{purpose}({scope}): {desc}` (한국어 OK). **Co-Authored-By 없이.**
- 브랜치: 현재 `junhyeon/feature/95/spring-AI-ping-endpoint` 그대로 사용.
- `application.yml`의 민감정보(DB 비밀번호, opendart/kis/naver 키)는 절대 git에 커밋되지 않아야 한다.
- temperature 기본값 프로퍼티 키: `ssogssog.ai.default-temperature` (없으면 코드에서 `0.0` fallback).
- 질문 최대 길이: 1000자.

---

### Task 1: 프로파일 분리 (민감정보 격리 + active=dev)

현재 `src/main/resources/application.yml`은 전체가 `.gitignore` 대상이며 민감정보(DB 비밀번호, opendart/kis/naver API 키)가 평문으로 들어 있다. `@Profile("!prod")` 컨트롤러 제한이 로컬(`dev`)에서 동작하려면 active profile을 명시해야 하고, 민감정보는 계속 git에서 제외되어야 한다.

방침: **공통·비민감 설정 → `application.yml`(git 추적), 민감·로컬 설정 → `application-dev.yml`(gitignore)**.

**Files:**
- Modify: `src/main/resources/application.yml` (민감정보 제거, 공통 설정 + `spring.profiles.active: dev`만 남김)
- Create: `src/main/resources/application-dev.yml` (민감정보·로컬 datasource)
- Modify: `.gitignore:40` (`src/main/resources/application.yml` 라인 제거, `src/main/resources/application-dev.yml` 추가)

**Interfaces:**
- Produces: active profile `dev`. `ssogssog.ai.default-temperature` 프로퍼티(값 `0.0`). `AiAskController`(Task 5)가 `@Profile("!prod")`로 dev에서 활성화되는 전제.

- [ ] **Step 1: `application-dev.yml` 생성 (민감·로컬 설정)**

현재 `application.yml`에 있던 민감/로컬 값을 이 파일로 옮긴다. **아래 `<...>` 자리에는 현재
`application.yml`(로컬, gitignore 상태)에 들어 있는 실제 값을 그대로 복사해 넣는다. 실제 비밀값을
이 플랜 문서나 커밋되는 어떤 파일에도 적지 않는다.**

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    url: jdbc:mysql://localhost:3306/ssogssog?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&serverTimezone=Asia/Seoul
    password: <LOCAL_DB_PASSWORD>
  jpa:
    properties:
      hibernate:
        format_sql: true
    show-sql: true
    hibernate:
      ddl-auto: update
      use_sql_comments: true
  ai:
    openai:
      base-url: https://generativelanguage.googleapis.com/v1beta/openai
      api-key: ${GEMINI_API_KEY}
      chat:
        options:
          model: gemini-2.5-flash
          temperature: 0.0

opendart:
  api-key: <OPENDART_API_KEY>
  rate-limit:
    permits-per-second: 5.0

kis:
  app-key: <KIS_APP_KEY>
  app-secret: <KIS_APP_SECRET>
  base-url: https://openapi.koreainvestment.com:9443
  rate-limit:
    permits-per-second: 10.0

naver-search:
  app-key: <NAVER_APP_KEY>
  app-secret: <NAVER_APP_SECRET>
  rate-limit:
    permits-per-second: 10.0

scheduler:
  today-price:
    enabled: true

logging:
  level:
    com.zaxxer.hikari: DEBUG
    org.hibernate.SQL: DEBUG
```

- [ ] **Step 2: `application.yml`을 공통·비민감 설정으로 축소**

민감정보를 전부 제거하고 공통 설정 + 프로파일 지정 + 우리 전용 프로퍼티만 남긴다.

```yaml
spring:
  application:
    name: ssogssog
  profiles:
    active: dev

ssogssog:
  ai:
    default-temperature: 0.0
```

- [ ] **Step 3: `.gitignore` 조정**

`.gitignore:40`의 `src/main/resources/application.yml` 라인을 지우고, 대신 dev 프로파일을 무시하도록 바꾼다.

```gitignore
src/main/resources/application-dev.yml
```

- [ ] **Step 4: 민감정보가 추적 파일에 없는지 검증**

Run: `git add -A && git status && git ls-files src/main/resources/`
Expected: `application.yml`은 추적 목록에 나타나고, `application-dev.yml`은 나타나지 않음.

Run (staged diff 전체를 스캔 — application.yml뿐 아니라 문서·모든 스테이징 파일 포함):
```bash
git diff --cached | grep -iE "(app-secret|api-key|password):[[:space:]]*[A-Za-z0-9+/]{6,}"
```
Expected: 아무 출력도 없음(값이 비어 있거나 `${ENV}`/placeholder만 존재). 실제 비밀 문자열이 잡히면
해당 파일에서 값을 제거(또는 `${ENV}`로 치환)하고 다시 스캔한다.

- [ ] **Step 5: 앱이 dev 프로파일로 정상 기동하는지 확인**

Run: `GEMINI_API_KEY=dummy ./gradlew bootRun` (수동, 사용자 요청 시). 로그에서 `The following 1 profile is active: "dev"` 확인 후 Ctrl+C.
Expected: `Started SsogssogApplication`. (실제 AI 호출은 이 단계에서 안 함.)

- [ ] **Step 6: 커밋**

```bash
git add src/main/resources/application.yml .gitignore
git commit -m "chore(config): dev/공통 프로파일 분리 및 민감정보 격리"
```

---

### Task 1.5: 테스트 프로파일(MySQL) 설정 — 커밋되는 비민감 test 설정

`@SpringBootTest`(`SsogssogApplicationTests.contextLoads`)는 datasource가 있어야 컨텍스트가 뜬다.
현재 로컬 MySQL 설정은 `application-dev.yml`(gitignore)로 격리되므로, dev 설정이 없으면
`./gradlew test`가 컨텍스트 기동에 실패한다. 실제 런타임과 동일한 **MySQL**을 쓰되, 커밋 가능하도록
민감정보(외부 API 키, DB 비밀번호)를 뺀 **비민감 test 프로파일**을 추가한다.

**전제:** 이 방식은 테스트 실행 환경에 `ssogssog` 스키마를 가진 로컬 MySQL이 `localhost:3306`에 떠 있어야
한다(현재 개발자 로컬은 3306 오픈 상태). DB 비밀번호는 커밋하지 않고 환경변수 `TEST_DB_PASSWORD`로 주입한다.

**Files:**
- Create: `src/test/resources/application-test.yml` (로컬 MySQL, 비민감 — 커밋됨)
- Modify: `src/test/java/org/project/ssogssog/SsogssogApplicationTests.java` (`@ActiveProfiles("test")` 추가)

**Interfaces:**
- Produces: active profile `test`에서 로컬 MySQL datasource. 이후 모든 `@SpringBootTest`는 이 프로파일을 사용.

- [ ] **Step 1: `application-test.yml` 생성 (MySQL, 비민감)**

`src/test/resources/application-test.yml`. DB 비밀번호는 `${TEST_DB_PASSWORD:...}`로 주입하고,
외부 API 키는 테스트에서 실제 호출하지 않으므로 더미값으로 둔다.

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/ssogssog?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&serverTimezone=Asia/Seoul
    username: root
    password: ${TEST_DB_PASSWORD:}
  jpa:
    hibernate:
      ddl-auto: update

# 외부 클라이언트 키는 테스트에서 사용하지 않으므로 더미값
opendart:
  api-key: test
  rate-limit:
    permits-per-second: 5.0
kis:
  app-key: test
  app-secret: test
  base-url: http://localhost
  rate-limit:
    permits-per-second: 10.0
naver-search:
  app-key: test
  app-secret: test
  rate-limit:
    permits-per-second: 10.0

scheduler:
  today-price:
    enabled: false

ssogssog:
  ai:
    default-temperature: 0.0
```

- [ ] **Step 2: 기존 `@SpringBootTest`에 test 프로파일 지정**

`src/test/java/org/project/ssogssog/SsogssogApplicationTests.java`를 수정:

```java
package org.project.ssogssog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SsogssogApplicationTests {

    @Test
    void contextLoads() {
    }

}
```

- [ ] **Step 3: 컨텍스트가 test 프로파일 MySQL로 뜨는지 확인**

로컬 MySQL(3306, `ssogssog` 스키마)이 떠 있는 상태에서:

Run: `TEST_DB_PASSWORD=<로컬 DB 비밀번호> ./gradlew test --tests "org.project.ssogssog.SsogssogApplicationTests"`
Expected: PASS. 로그에 active profile `test`.

> 참고: `spring.ai.openai.api-key`는 테스트에서 실제 호출을 하지 않으므로 미설정이어도 컨텍스트 기동에는
> 문제없다. 만약 Spring AI 자동설정이 api-key 부재로 기동 실패하면, `application-test.yml`의 `spring.ai.openai`에
> `api-key: test`, `base-url: http://localhost`를 추가한다.

- [ ] **Step 4: `application-test.yml`에 민감값이 없는지 확인**

Run: `git add src/test/resources/application-test.yml && git diff --cached src/test/resources/application-test.yml | grep -iE "(app-secret|api-key|password):[[:space:]]*[A-Za-z0-9+/]{6,}"`
Expected: 출력 없음(더미값 `test`/빈 값/`${ENV}`만 존재).

- [ ] **Step 5: 커밋**

```bash
git add src/test/resources/application-test.yml src/test/java/org/project/ssogssog/SsogssogApplicationTests.java
git commit -m "test(config): 비민감 MySQL test 프로파일 추가"
```

---

### Task 2: ErrorStatus 확장 + 시스템 프롬프트 리소스 파일

`AiAskService`가 사용할 에러 코드 3종과 도메인 시스템 프롬프트 파일을 준비한다.

**Files:**
- Modify: `src/main/java/org/project/ssogssog/global/payload/code/status/ErrorStatus.java`
- Create: `src/main/resources/prompts/stock-assistant-system.st`

**Interfaces:**
- Produces: `ErrorStatus.AI_QUESTION_REQUIRED`, `ErrorStatus.AI_QUESTION_TOO_LONG`, `ErrorStatus.AI_TEMPERATURE_OUT_OF_RANGE` (모두 `HttpStatus.BAD_REQUEST`). classpath 리소스 `prompts/stock-assistant-system.st`.

- [ ] **Step 1: `ErrorStatus`에 AI 에러 코드 3종 추가**

기존 enum 상수 목록의 `KIS_OTHERS_ERROR(...)` 줄 다음, 마지막 `;` 앞에 아래를 추가한다.

```java
    // AI 에러
    AI_QUESTION_REQUIRED(HttpStatus.BAD_REQUEST, "AI4000", "질문(question)은 비어 있을 수 없습니다."),
    AI_QUESTION_TOO_LONG(HttpStatus.BAD_REQUEST, "AI4001", "질문(question)은 1000자를 초과할 수 없습니다."),
    AI_TEMPERATURE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "AI4002", "temperature는 0.0 이상 2.0 이하이어야 합니다."),
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 시스템 프롬프트 파일 생성**

`src/main/resources/prompts/stock-assistant-system.st`:

```text
너는 "주식쏙쏙" 서비스의 종목 분석 도우미다.
주식쏙쏙은 한국 주식 종목의 지표 데이터를 수집·스크리닝하는 서비스다.

[조회·설명 가능한 지표]
- 가격/규모: 현재가, 시가총액
- 밸류에이션: PER(주가수익비율)
- 수익성: ROE(자기자본이익률), 순이익률, 영업이익률
- 안정성: 부채비율
- 성장성: 매출액 성장률(전분기 대비/전년 동기 대비), 순이익 성장률(전분기 대비/전년 동기 대비)
- 배당/수급: 배당수익률, 외국인 보유율
- 수익률: 3개월/6개월/12개월 수익률

[스크리닝(필터) 조건으로 지원하는 지표]
- 현재가, 시가총액, PER, ROE, 부채비율, 영업이익률,
  매출액 성장률, 순이익 성장률, 배당수익률, 외국인 보유율
- 위 목록에 없는 지표(예: 순이익률, 3/6/12개월 수익률)는 조회·설명은 가능하지만
  종목 스크리닝 조건으로는 사용할 수 없다. 스크리닝 요청 시 이 점을 명확히 알려라.

[규칙]
- 위 [조회·설명 가능한 지표] 목록에 없는 지표(예: PBR, EPS, PEG 등)는 주식쏙쏙이 지원하지 않는다.
  요청받으면 지원하지 않는다고 명확히 답하라.
- 특정 종목의 매수/매도를 단정하거나 투자를 권유하지 마라. 지표 기반 해석과 일반적 설명까지만 제공하라.
- 확실하지 않은 수치를 지어내지 마라. 데이터가 없으면 없다고 답하라.
- 항상 한국어로, 간결하게 답하라.
```

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/org/project/ssogssog/global/payload/code/status/ErrorStatus.java src/main/resources/prompts/stock-assistant-system.st
git commit -m "feat(ai): AI 에러 코드 및 종목 분석 시스템 프롬프트 리소스 추가"
```

---

### Task 3: 요청/응답 DTO

`POST /ai/ask`의 요청·응답 record를 만든다.

**Files:**
- Create: `src/main/java/org/project/ssogssog/application/service/ai/api/dto/AiAskRequest.java`
- Create: `src/main/java/org/project/ssogssog/application/service/ai/api/dto/AiAskResponse.java`

**Interfaces:**
- Produces:
  - `AiAskRequest(String question, Double temperature)` — `temperature`는 nullable.
  - `AiAskResponse(String answer, double temperature)` — `AiAskResponse.of(String answer, double temperature)` 정적 팩토리.

- [ ] **Step 1: `AiAskRequest` 생성**

```java
package org.project.ssogssog.application.service.ai.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 질문 요청")
public record AiAskRequest(

        @Schema(description = "AI에게 보낼 질문", example = "PER이 낮은 종목은 어떤 의미인가요?")
        String question,

        @Schema(description = "샘플링 온도(0.0~2.0). 생략 시 서버 기본값 사용", example = "0.7")
        Double temperature

) {
}
```

- [ ] **Step 2: `AiAskResponse` 생성**

```java
package org.project.ssogssog.application.service.ai.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 질문 응답")
public record AiAskResponse(

        @Schema(description = "AI 답변 텍스트")
        String answer,

        @Schema(description = "실제 적용된 샘플링 온도")
        double temperature

) {
    public static AiAskResponse of(final String answer, final double temperature) {
        return new AiAskResponse(answer, temperature);
    }
}
```

- [ ] **Step 3: 컴파일 확인**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: 커밋**

```bash
git add src/main/java/org/project/ssogssog/application/service/ai/api/dto/AiAskRequest.java src/main/java/org/project/ssogssog/application/service/ai/api/dto/AiAskResponse.java
git commit -m "feat(ai): /ai/ask 요청·응답 DTO 추가"
```

---

### Task 4: AiAskService (검증 + temperature resolve + 호출 조립) + 단위 테스트

질문 검증과 temperature resolve는 순수 로직이라 단위 테스트로 검증한다. `ChatClient` 실제 호출은 프레임워크 코드라 목킹하지 않고(그 계약을 고정하지 않는다 — test-patterns 원칙), 실호출 검증은 `debug-and-verify-locally`(수동, Task 6)로 남긴다.

테스트 가능하도록, 검증·resolve 로직을 `ChatClient` 호출과 분리한다: `resolveTemperature(Double)`, `validate(AiAskRequest)`를 package-private 메서드로 두고 단위 테스트에서 직접 호출한다.

**Files:**
- Create: `src/main/java/org/project/ssogssog/application/service/ai/api/AiAskService.java`
- Test: `src/test/java/org/project/ssogssog/application/service/ai/api/AiAskServiceTest.java`
  (package-private 메서드 `resolveTemperature`/`validateQuestion`를 테스트하므로 **서비스와 동일 패키지**에 둔다.)

**Interfaces:**
- Consumes: `ChatClient`(빈, `AiConfig`), `AiAskRequest`/`AiAskResponse`(Task 3), `ErrorStatus` 3종(Task 2), classpath 리소스 `prompts/stock-assistant-system.st`.
- Produces:
  - `AiAskResponse ask(AiAskRequest request)` — 검증 → resolve → `ChatClient` 호출 → 응답.
  - `double resolveTemperature(Double requested)` (package-private) — null이면 기본값, 아니면 검증 후 그 값.
  - `void validateQuestion(String question)` (package-private) — null/blank/1000자 초과 검증.

- [ ] **Step 1: 실패하는 단위 테스트 작성**

`src/test/java/org/project/ssogssog/application/service/ai/api/AiAskServiceTest.java`:

```java
package org.project.ssogssog.application.service.ai.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ssogssog.global.payload.exception.GeneralException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiAskServiceTest {

    private static final Resource DUMMY_PROMPT = new ByteArrayResource("system".getBytes());

    // ChatClient는 이 테스트에서 사용하지 않는 로직만 검증하므로 null로 둔다.
    private final AiAskService service = new AiAskService(null, DUMMY_PROMPT, 0.0);

    @Test
    @DisplayName("temperature 미지정(null)이면 기본값을 resolve한다")
    void resolveTemperature_usesDefaultWhenNull() {
        assertThat(service.resolveTemperature(null)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("temperature 지정 시 그 값을 resolve한다")
    void resolveTemperature_usesRequestedValue() {
        assertThat(service.resolveTemperature(0.7)).isEqualTo(0.7);
    }

    @Test
    @DisplayName("temperature가 범위(0.0~2.0) 밖이면 예외를 던진다")
    void resolveTemperature_rejectsOutOfRange() {
        assertThatThrownBy(() -> service.resolveTemperature(-0.1))
                .isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> service.resolveTemperature(2.1))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("temperature 경계값 0.0, 2.0은 허용한다")
    void resolveTemperature_allowsBoundaries() {
        assertThat(service.resolveTemperature(0.0)).isEqualTo(0.0);
        assertThat(service.resolveTemperature(2.0)).isEqualTo(2.0);
    }

    @Test
    @DisplayName("질문이 null이면 예외를 던진다")
    void validateQuestion_rejectsNull() {
        assertThatThrownBy(() -> service.validateQuestion(null))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("질문이 공백이면 예외를 던진다")
    void validateQuestion_rejectsBlank() {
        assertThatThrownBy(() -> service.validateQuestion("   "))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("질문이 1000자를 초과하면 예외를 던진다")
    void validateQuestion_rejectsTooLong() {
        final String tooLong = "가".repeat(1001);
        assertThatThrownBy(() -> service.validateQuestion(tooLong))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    @DisplayName("질문이 1000자 이하면 통과한다")
    void validateQuestion_allowsWithinLimit() {
        service.validateQuestion("가".repeat(1000)); // 예외 없음
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "org.project.ssogssog.application.service.ai.api.AiAskServiceTest"`
Expected: 컴파일 실패 — `AiAskService` 없음 / 생성자·메서드 미정의.

- [ ] **Step 3: `AiAskService` 구현**

```java
package org.project.ssogssog.application.service.ai.api;

import org.project.ssogssog.application.service.ai.api.dto.AiAskRequest;
import org.project.ssogssog.application.service.ai.api.dto.AiAskResponse;
import org.project.ssogssog.global.payload.code.status.ErrorStatus;
import org.project.ssogssog.global.payload.exception.GeneralException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * 주식쏙쏙 도메인 시스템 프롬프트를 주입해 사용자 질문에 답하는 서비스.
 * 요청별 temperature를 지원하며, 미지정 시 서버 기본값을 사용한다.
 */
@Service
public class AiAskService {

    private static final int MAX_QUESTION_LENGTH = 1000;
    private static final double MIN_TEMPERATURE = 0.0;
    private static final double MAX_TEMPERATURE = 2.0;

    private final ChatClient chatClient;
    private final String systemPrompt;
    private final double defaultTemperature;

    public AiAskService(
            final ChatClient chatClient,
            @Value("classpath:prompts/stock-assistant-system.st") final Resource systemPromptResource,
            @Value("${ssogssog.ai.default-temperature:0.0}") final double defaultTemperature
    ) {
        this.chatClient = chatClient;
        this.systemPrompt = readPrompt(systemPromptResource);
        this.defaultTemperature = defaultTemperature;
    }

    public AiAskResponse ask(final AiAskRequest request) {
        validateQuestion(request.question());
        final double temperature = resolveTemperature(request.temperature());

        final String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(request.question())
                .options(ChatOptions.builder().temperature(temperature).build())
                .call()
                .content();

        return AiAskResponse.of(answer, temperature);
    }

    void validateQuestion(final String question) {
        if (question == null || question.isBlank()) {
            throw new GeneralException(ErrorStatus.AI_QUESTION_REQUIRED);
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new GeneralException(ErrorStatus.AI_QUESTION_TOO_LONG);
        }
    }

    double resolveTemperature(final Double requested) {
        if (requested == null) {
            return defaultTemperature;
        }
        if (requested < MIN_TEMPERATURE || requested > MAX_TEMPERATURE) {
            throw new GeneralException(ErrorStatus.AI_TEMPERATURE_OUT_OF_RANGE);
        }
        return requested;
    }

    private static String readPrompt(final Resource resource) {
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException("시스템 프롬프트 리소스를 읽지 못했습니다.", e);
        }
    }
}
```

주의: 이 파일이 `ChatOptions.builder().temperature(double)` API를 사용한다. Spring AI 1.0.0의 `org.springframework.ai.chat.prompt.ChatOptions` 빌더에 `temperature(Double)`가 있는지 Step 5 전에 확인한다(아래).

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "org.project.ssogssog.application.service.ai.api.AiAskServiceTest"`
Expected: 8개 테스트 모두 PASS.

- [ ] **Step 5: 전체 컴파일 확인 (ChatOptions API 검증 포함)**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL. 만약 `ChatOptions.builder().temperature(...)`가 컴파일 실패하면, `ask()`의 옵션 생성만 `org.springframework.ai.openai.OpenAiChatOptions.builder().temperature(temperature).build()`로 교체한다(import 추가). 나머지 로직은 그대로.

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/org/project/ssogssog/application/service/ai/api/AiAskService.java src/test/java/org/project/ssogssog/application/service/ai/api/AiAskServiceTest.java
git commit -m "feat(ai): 시스템 프롬프트 주입 및 temperature 지원 AiAskService 추가"
```

---

### Task 5: AiAskController (@Profile 제한) + 슬라이스 테스트

`POST /ai/ask` 엔드포인트를 추가하고, 운영 노출을 막기 위해 `@Profile("!prod")`로 제한한다.

**Files:**
- Create: `src/main/java/org/project/ssogssog/presentation/controller/ai/AiAskController.java`
- Test: `src/test/java/org/project/ssogssog/unit/AiAskControllerTest.java`

**Interfaces:**
- Consumes: `AiAskService.ask(AiAskRequest)`(Task 4), `AiAskRequest`/`AiAskResponse`(Task 3), `ApiResponse`.
- Produces: `POST /ai/ask` → `ApiResponse<AiAskResponse>`.

- [ ] **Step 1: 실패하는 슬라이스 테스트 작성**

`AiAskService`를 목킹하고, 컨트롤러가 응답 봉투를 올바르게 감싸는지 검증한다.

```java
package org.project.ssogssog.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.ssogssog.application.service.ai.api.AiAskService;
import org.project.ssogssog.application.service.ai.api.dto.AiAskRequest;
import org.project.ssogssog.application.service.ai.api.dto.AiAskResponse;
import org.project.ssogssog.presentation.controller.ai.AiAskController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiAskController.class)
class AiAskControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AiAskService aiAskService;

    @Test
    @DisplayName("POST /ai/ask 는 ApiResponse 봉투로 answer와 temperature를 반환한다")
    void ask_returnsApiResponseEnvelope() throws Exception {
        when(aiAskService.ask(any(AiAskRequest.class)))
                .thenReturn(AiAskResponse.of("PER은 주가수익비율입니다.", 0.7));

        final AiAskRequest request = new AiAskRequest("PER이 뭐야?", 0.7);

        mockMvc.perform(post("/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.answer").value("PER은 주가수익비율입니다."))
                .andExpect(jsonPath("$.result.temperature").value(0.7));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "org.project.ssogssog.unit.AiAskControllerTest"`
Expected: 컴파일 실패 — `AiAskController` 없음.

- [ ] **Step 3: `AiAskController` 구현**

```java
package org.project.ssogssog.presentation.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.project.ssogssog.application.service.ai.api.AiAskService;
import org.project.ssogssog.application.service.ai.api.dto.AiAskRequest;
import org.project.ssogssog.application.service.ai.api.dto.AiAskResponse;
import org.project.ssogssog.global.payload.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
@Profile("!prod")
public class AiAskController {

    private final AiAskService aiAskService;

    @PostMapping("/ask")
    @Operation(
            summary = "주식쏙쏙 도메인 프롬프트 기반 AI 질의",
            description = """
            주식쏙쏙 종목 분석 시스템 프롬프트를 주입해 사용자 질문에 답합니다.
            temperature를 함께 보내면(0.0~2.0) 답변의 다양성을 조절할 수 있고, 생략 시 서버 기본값을 사용합니다.
            운영 프로파일에서는 노출되지 않는 학습·실험용 엔드포인트입니다.
            """
    )
    public ApiResponse<AiAskResponse> ask(@RequestBody final AiAskRequest request) {
        return ApiResponse.onSuccess(aiAskService.ask(request));
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "org.project.ssogssog.unit.AiAskControllerTest"`
Expected: PASS.

- [ ] **Step 5: 전체 테스트 확인**

Run (로컬 MySQL 3306 기동 상태에서): `TEST_DB_PASSWORD=<로컬 DB 비밀번호> ./gradlew test`
Expected: BUILD SUCCESSFUL (기존 테스트 + 신규 테스트 모두 통과). `@SpringBootTest`는 test 프로파일(MySQL, Task 1.5),
단위/`@WebMvcTest`는 datasource 없이 통과.

- [ ] **Step 6: 커밋**

```bash
git add src/main/java/org/project/ssogssog/presentation/controller/ai/AiAskController.java src/test/java/org/project/ssogssog/unit/AiAskControllerTest.java
git commit -m "feat(ai): POST /ai/ask 컨트롤러 추가 (운영 프로파일 제외)"
```

---

### Task 6: 실호출 수동 검증 (사용자 요청 시에만)

`debug-and-verify-locally` 스킬로, 실제 Gemini 응답과 temperature 차이를 확인한다. **사용자가 명시적으로 요청할 때만** 수행한다.

- [ ] **Step 1: 앱 기동**

Run: `GEMINI_API_KEY=<유효한 키> ./gradlew bootRun` (백그라운드). 로그에서 `Started SsogssogApplication` + active profile `dev` 확인.

- [ ] **Step 2: temperature 0.0 호출**

Run:
```bash
curl -s -X POST http://localhost:8080/ai/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"PER이 낮은 종목은 어떤 의미인가요?","temperature":0.0}'
```
Expected: `isSuccess:true`, `result.answer`에 한국어 답변, `result.temperature: 0.0`.

- [ ] **Step 3: temperature 0.7로 같은 질문 호출 → 답변 톤/표현 차이 관찰**

Run:
```bash
curl -s -X POST http://localhost:8080/ai/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"PER이 낮은 종목은 어떤 의미인가요?","temperature":0.7}'
```
Expected: `result.temperature: 0.7`. 0.0 응답과 표현 차이 비교.

- [ ] **Step 4: 미지원 지표(PBR) 질문 → 도메인 가드레일 확인**

Run:
```bash
curl -s -X POST http://localhost:8080/ai/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"PBR로 종목 스크리닝 해줘"}'
```
Expected: 답변에 "PBR은 지원하지 않는다" 취지의 안내(시스템 프롬프트 규칙 작동). `result.temperature`는 기본값 `0.0`.

- [ ] **Step 5: 검증용 에러 확인 (빈 질문)**

Run:
```bash
curl -s -X POST http://localhost:8080/ai/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"  "}'
```
Expected: `isSuccess:false`, code `AI4000`.

- [ ] **Step 6: 서버 종료**

Run: 백그라운드 프로세스 종료(`pkill -f SsogssogApplication`).

---

## Self-Review

**1. Spec coverage:**
- §1 도메인 시스템 프롬프트 주입 → Task 2(파일) + Task 4(주입). ✅
- §1 프롬프트를 리소스 파일로 분리 → Task 2 `.st` 파일 + Task 4 `@Value("classpath:...")`. ✅
- §1 temperature 0 vs 0.7 비교 → Task 3(요청 필드) + Task 4(resolve/명시) + Task 6(수동 비교). ✅
- §2 컴포넌트 배치 → Task 3/4/5. ✅
- §3 호출 흐름(system/user/options 조립) → Task 4 `ask()`. ✅
- §4 temperature resolve 규칙 + 항상 명시 + 응답 표시값 일치 → Task 4 `resolveTemperature` + `ask()`. ✅
- §4 기본값 프로퍼티 `ssogssog.ai.default-temperature` fallback 0.0 → Task 1(프로퍼티) + Task 4(`@Value`). ✅
- §4 질문 1000자 제한 → Task 4 `validateQuestion`. ✅
- §5 프롬프트 내용(조회/스크리닝 지표 분리) → Task 2 `.st`. ✅
- §6 에러 3종 → Task 2 `ErrorStatus` + Task 4 검증. ✅
- §7 단위/슬라이스 테스트, 실호출은 수동 → Task 4/5/6. ✅
- §8 프로파일 제한 → Task 1(active=dev) + Task 5(`@Profile("!prod")`). ✅
- §8 민감정보 격리 → Task 1. ✅
- (Codex High) clean checkout/CI 테스트 통과 → Task 1.5(비민감 MySQL test 프로파일 + `@ActiveProfiles("test")`). ✅

**Codex 리뷰 반영:**
- Critical(플랜 문서에 실제 비밀값) → Task 1 Step 1의 실제 키/비번을 `<PLACEHOLDER>`로 교체, Step 4 스캔을
  staged diff 전체로 확대. 이미 커밋된 히스토리(`1187eb8`)의 비밀값은 별도 처리하고 노출된 키는 사용자가 폐기.
- High(테스트 컴파일 불가) → `AiAskServiceTest`를 서비스와 동일 패키지
  `org.project.ssogssog.application.service.ai.api`로 이동(package-private 메서드 접근 가능).
- High(clean checkout 테스트 실패) → Task 1.5 추가.

**2. Placeholder scan:** `<LOCAL_DB_PASSWORD>` 등은 "로컬 값을 복사해 넣으라"는 의도적 placeholder(비밀값
커밋 방지). 그 외 모든 코드/명령/기대값은 구체적으로 채워짐. TBD 없음. ✅

**3. Type consistency:**
- `AiAskRequest(String question, Double temperature)` — Task 3 정의, Task 4/5에서 동일 사용. ✅
- `AiAskResponse.of(String, double)` — Task 3 정의, Task 4 `ask()`/Task 5 테스트에서 동일 사용. ✅
- `resolveTemperature(Double)` / `validateQuestion(String)` — Task 4 정의, 같은 태스크 테스트에서 동일 시그니처 호출. ✅
- `ErrorStatus.AI_QUESTION_REQUIRED/AI_QUESTION_TOO_LONG/AI_TEMPERATURE_OUT_OF_RANGE` — Task 2 정의, Task 4 사용. ✅
