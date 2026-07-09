# 주식쏙쏙 하네스 세팅 — 설계 문서

- 작성일: 2026-07-09
- 상태: 승인 대기
- 범위: 에이전트 작업 환경(하네스) 세팅. **애플리케이션 코드(`src/`)는 건드리지 않음.**

## 1. 배경과 목표

주식쏙쏙에 RAG/Function Calling 기능을 4주 커리큘럼으로 붙이기에 앞서, AI 에이전트가
일관된 규칙 위에서 작업하도록 **하네스**를 먼저 세팅한다.

참조 하네스 두 곳을 재료로 삼는다.

- **기본**: `/Users/imjunhyeon/depromeet/18th-team6-server`
  — `.claude/`(commands 3종 + skills 8종 + settings) + 루트 `AGENT.md`, `CLAUDE.md`
- **추가**: `/Users/imjunhyeon/Artium-Server/.agent`
  — 같은 구성 + `pre-commit-check`, `pre-push-check` 스킬 2종

두 참조 모두 **Kotlin + DDD** 기준으로 작성되어 있다. 우리 프로젝트는 **Java 21 + 헥사고날**
이므로, 구조와 문서 형식은 참조하되 **내용은 우리 스택에 맞게 각색**한다.

### 결정 사항 요약 (brainstorming에서 확정)

| 항목 | 결정 |
|---|---|
| 이식 방식 | 우리 프로젝트에 맞게 적응 (원본 복붙 아님) |
| 각색 수준 | 핵심 뼈대만 교체 (언어/아키텍처/빌드 커맨드), 교육적 내용·구조는 유지 |
| 디렉토리 | `.claude/`(Claude Code)와 `.agent/`(Codex) **양쪽 모두** 생성, 스킬 세트 동일하게 |
| 스킬 범위 | 참조 스킬 전부 각색 이식 + Artium의 pre-commit/pre-push 2종 추가 |
| 작성 언어 | **AI가 읽는 하네스 문서(skills, commands, CLAUDE.md, AGENT.md, MAP.md, CONVENTIONS.md)는 영어**로 작성 (토큰 절약). 단, 이 설계 문서 자체와 향후 AI가 작성하는 코드 주석/Swagger는 한국어 규칙 유지 |

## 2. 워크플로우 (역할 분담)

이 하네스가 지원하려는 작업 흐름. CLAUDE.md/AGENT.md에 명문화한다.

| 단계 | 담당 | 도구/스킬 |
|---|---|---|
| 스펙 작성 | superpowers | `brainstorming` → `writing-plans` |
| 스펙 리뷰 | **Codex** | (외부, 사용자가 Codex로 수행) |
| 코드 작성 | Claude | `implement-spring-backend-feature`, `jpa-patterns`, `test-patterns` 등 참조 |
| 코드 리뷰 | **Codex** | (외부, 사용자가 Codex로 수행) |
| 실행 검증 | Claude | `debug-and-verify-locally` — **사용자가 명시적으로 요청할 때만** |
| 커밋 전 검사 | Claude | `pre-commit-check` (시크릿/커밋 메시지) |
| 푸시 전 검사 | Claude | `pre-push-check` (테스트/문서 동기화) |

> 스킬 세트는 참조를 폭넓게 담되, "코드 리뷰의 최종 판단은 Codex/사람"이라는 점을
> CLAUDE.md에 명시한다. (Claude가 리뷰 스킬을 보유하더라도 주 리뷰어는 Codex)

## 3. 최종 디렉토리 구조

```
ssogssog_spring/
├── AGENT.md                       # 에이전트 진입점 (우리 프로젝트 버전)
├── CLAUDE.md                      # 프로젝트 가이드 (Java 21 / 헥사고날 / 실제 스택)
├── .claude/
│   ├── settings.local.json        # 우리 프로젝트용 permission allowlist
│   ├── commands/
│   │   ├── dev-plan.md
│   │   ├── review.md
│   │   └── harness-update.md
│   └── skills/
│       ├── implement-spring-backend-feature/SKILL.md
│       ├── review-spring-backend-change/SKILL.md
│       ├── test-patterns/SKILL.md
│       ├── jpa-patterns/SKILL.md
│       ├── debug-and-verify-locally/SKILL.md
│       ├── select-spring-design-pattern/SKILL.md
│       ├── organize-domain-model/SKILL.md
│       ├── fill-github-template/SKILL.md
│       ├── pre-commit-check/SKILL.md
│       └── pre-push-check/SKILL.md
├── .agent/                        # Codex용 — .claude와 동일 세트
│   ├── settings.local.json
│   ├── commands/  (동일 3종)
│   └── skills/    (동일 10종)
└── docs/
    └── specs/
        ├── MAP.md                 # 우리 헥사고날 아키텍처 맵
        └── CONVENTIONS.md         # 우리 Java 컨벤션
```

## 4. 우리 프로젝트의 실제 아키텍처 (각색 기준)

각색·문서화의 기준이 되는 실제 구조. `src/main/java/org/project/ssogssog/` 하위:

```
presentation/
  controller/{도메인}/           # @RestController, ApiResponse 반환
application/
  service/{도메인}/
    api/                         # 외부에 노출되는 서비스 진입점
    usecase/                     # 유스케이스 단위 로직
    writer/                      # 쓰기(적재) 책임 분리
  utils/
domain/
  {도메인}/
    entity/                      # JPA 엔티티
    vo/                          # 값 객체 (record)
    enums/                       # 도메인 enum
    repository/                  # 리포지토리 + Custom(QueryDSL)
    factory/                     # 계산/생성 로직
infrastructure/
  config/                        # QueryDSLConfig, CacheConfig, RateLimiterConfig 등
  client/{opendart,kis,naver}/   # 외부 API 클라이언트
  adapter/, scheduler/, persistence/
global/
  payload/                       # ApiResponse, BaseErrorCode, GeneralException, ExceptionAdvice
  paging/
```

**스택**: Spring Boot 3.5.8 / Java 21 / Gradle / JPA(Hibernate) / QueryDSL 5.0 /
MySQL / Redis / Caffeine / ShedLock / Actuator / springdoc-openapi(Swagger).

**빌드/검증 커맨드** (참조의 `./gradlew harness`·`ktlint`를 우리 것으로 교체):
- `./gradlew build` — 빌드 + 전체 검증
- `./gradlew test` — 테스트
- (ktlint / ArchUnit / `harness` 태스크는 현재 없음 → 스킬에서 해당 참조 제거)

## 5. 각색 원칙 (핵심 뼈대만 교체)

각 파일에서 아래만 우리 것으로 바꾸고, 스킬의 교육적 설명·구조·체크리스트는 유지한다.

| 참조(Kotlin/DDD) | 우리 것(Java/헥사고날) |
|---|---|
| Kotlin, `val`, `data class`, 불변 컬렉션 | Java 21, `record`(VO/DTO), `final`, Lombok |
| DDD(`{도메인}/controller|service|dto|entity|repository`) | 헥사고날(`presentation/application/domain/infrastructure/global`) |
| `./gradlew harness` (ktlint+ArchUnit+test) | `./gradlew build`, `./gradlew test` |
| `ktlintCheck` / `ktlintFormat` | (없음 — 제거하거나 "포매터 미도입" 명시) |
| `data.sql`(H2 시드) | (해당 없음 — 제거) |
| `depromeet.hotsix.obrit` 패키지 | `org.project.ssogssog` |
| `docs/PRD/api-spec.md`(pre-push 문서 동기화 대상) | `docs/specs/*` 및 향후 문서로 조정 |

## 6. 스킬별 각색 메모

- **implement-spring-backend-feature**: 레이어 순서를 우리 구조
  (domain entity/vo → repository → application service(api/usecase/writer) →
  presentation controller → test)로. `ApiResponse` 반환, `GeneralException`/`BaseErrorCode`
  예외 표준 사용 명시.
- **review-spring-backend-change**: 보유하되 "주 리뷰어는 Codex"임을 헤더에 명시.
  헥사고날 의존성 방향으로 규칙 교체.
- **jpa-patterns / test-patterns**: 내용 대체로 유지, 코드 예시만 Java로.
- **debug-and-verify-locally**: `./gradlew bootRun` 유지(우리도 동일). "사용자 요청 시에만
  실행 검증"을 명시.
- **pre-commit-check**: 그대로 유용(시크릿/커밋 메시지). 우리 커밋 컨벤션으로 예시 교체.
  `application.yml`은 `.gitignore`에 이미 포함됨을 반영.
- **pre-push-check**: 테스트 커맨드를 `./gradlew test`로. 문서 동기화 대상은 `docs/specs/*`.
- **select-spring-design-pattern / organize-domain-model / fill-github-template**:
  구조 유지, 언어/네이밍만 각색. (organize-domain-model은 "enum은 domain/enums에" 우리 규칙으로)

## 7. settings.local.json

참조의 permission allowlist는 각 프로젝트 전용이라 그대로 쓸 수 없다.
우리 프로젝트에서 자주 쓸 read-only/빌드 커맨드 위주로 **최소 allowlist**를 새로 구성한다.
(예: `./gradlew build`, `./gradlew test *`, `./gradlew bootRun`, git 상태 조회류)
`.claude/`와 `.agent/` 각각에 둔다.

## 8. 명시적 비범위 (하지 않는 것)

- `src/` 애플리케이션 코드 수정 (Spring AI 코드는 이후 별도 작업)
- ktlint/ArchUnit/harness gradle 태스크 도입 (참조엔 있으나 우리 프로젝트엔 미도입)
- `docs/HARNESS_GUIDE.md`, `docs/specs/ADR/`, `EXECUTION_PLAN.md` 등 참조의 부가 문서
  전체 이식 (MAP.md, CONVENTIONS.md 2종만 우선)
- 기존 `application.yml`의 평문 키 정리 (별도 작업으로 분리)
- Git hook 실제 설치(`.githooks/`) — 스킬은 넣되 hook 파일 강제 설치는 이번 범위 밖

## 9. 검증 기준 (완료 정의)

- [ ] `.claude/`, `.agent/` 양쪽에 commands 3종 + skills 10종이 존재
- [ ] `CLAUDE.md`가 우리 실제 스택/아키텍처/빌드 커맨드를 반영 (Kotlin/harness/ktlint 잔재 없음)
- [ ] `AGENT.md`가 우리 문서(`CLAUDE.md`, `docs/specs/MAP.md`, `CONVENTIONS.md`)만 참조 (깨진 참조 없음)
- [ ] 스킬 SKILL.md들에 Kotlin/`val`/`data.sql`/`obrit` 잔재가 없음
- [ ] `docs/specs/MAP.md`, `CONVENTIONS.md`가 우리 헥사고날 구조를 기술
- [ ] Claude Code가 `.claude/skills`를 로드하는지 확인 (세션에서 스킬 목록 노출)
