# 주식쏙쏙 AI 프롬프트 설계 (`POST /ai/ask`) — 설계 문서

- 작성일: 2026-07-09
- 상태: 승인 대기
- 범위: RAG 커리큘럼 **Week 1 Day 2 "프롬프트 설계"**. 도메인 시스템 프롬프트를 주입하는 `POST /ai/ask` 엔드포인트 추가.
- 커리큘럼 위치: Week 1 Day 1(`/ai/ping`, 커밋 76fc99c) 다음 단계.

## 1. 배경과 목표

Day 1에서 Spring AI 배선을 확인했다(`GET /ai/ping` — 고정 인사말을 Gemini로 보내 응답 확인).
Day 1 구조에는 시스템 프롬프트도, temperature 튜닝도 없다.

Day 2의 목표는 커리큘럼이 정한 세 가지를 코드로 실현하는 것이다.

1. **도메인 시스템 프롬프트 주입** — "너는 주식쏙쏙의 종목 분석 도우미다" + 지원 지표 + 가드레일.
2. **프롬프트를 코드가 아닌 리소스 파일로 분리** — 유지보수·확장 용이.
3. **temperature 0 vs 0.7 차이 체감** — 같은 질문을 값만 바꿔 눌러 비교.

이를 하나의 실험용 엔드포인트 `POST /ai/ask`로 묶는다. 이 엔드포인트는 Week 2~3(RAG/Function Calling)의
자연스러운 확장 지점이 된다.

### 결정 사항 요약 (brainstorming에서 확정)

| 항목 | 결정 |
|---|---|
| 결과물 형태 | `/ai/ping`은 그대로 두고 **신규 엔드포인트 `POST /ai/ask`** 추가 |
| temperature 지정 방식 | **요청 바디로 받음**(선택). 미지정 시 yml 기본값(현재 0.0) 사용 |
| 시스템 프롬프트 범위 | **역할 + 지원 지표 목록 + 핵심 가드레일** (지표별 상세 설명은 YAGNI로 제외) |
| 프롬프트 파일 형식 | **`.st` 파일 + Spring AI `Resource` 주입** (`@Value("classpath:...")`) |
| 응답 형태 | **답변 텍스트 + 실제 적용된(resolved) temperature** 를 함께 반환 |
| 남용 최소 안전장치 | **`@Profile({"local","dev"})` + `question` 최대 1000자**. 정교한 rate limit은 Week 4 (§8) |

## 2. 컴포넌트 배치

기존 헥사고날 레이아웃(MAP.md)과 Day 1 패턴을 따른다.

```text
presentation/controller/ai/
  AiAskController.java             (신규)  POST /ai/ask, @Profile({"local","dev"})

application/service/ai/api/
  AiPingService.java               (기존, 변경 없음)
  AiAskService.java                (신규)  시스템 프롬프트 + 질문 + temperature 조립
  dto/
    AiAskRequest.java              (신규)  { question, temperature? }
    AiAskResponse.java             (신규)  { answer, temperature }

infrastructure/config/
  AiConfig.java                    (기존, 변경 없음 — ChatClient 빈 재사용)

global/payload/code/status/
  ErrorStatus.java                 (수정)  AI 검증용 에러 코드 3종 추가

resources/prompts/
  stock-assistant-system.st        (신규)  도메인 시스템 프롬프트 원문
```

- 컨트롤러는 얇게: 요청을 받아 서비스 호출, `ApiResponse<AiAskResponse>` 반환.
- `AiConfig`, `/ai/ping`은 손대지 않는다.
- `AiAskController`는 `@Profile({"local","dev"})`로 제한한다(§8 참조). 운영 프로파일에는 빈이 등록되지 않는다.

## 3. 요청 흐름

```text
AiAskController → AiAskService → ChatClient
    .prompt()
    .system(<stock-assistant-system.st 내용>)
    .user(request.question())
    .options(ChatOptions.builder().temperature(<resolved temperature>).build())
    .call().content()
```

- 시스템 프롬프트: `AiAskService`가 `@Value("classpath:prompts/stock-assistant-system.st")`로 주입받은
  `Resource`에서 읽는다.
- temperature: **항상** `ChatOptions`에 명시적으로 넣는다. resolved 값은 요청에 값이 있으면 그 값,
  없으면 서비스가 주입받은 기본값(§4)이다. 이렇게 하면 provider/model 기본값에 의존하지 않고
  "실제 호출값 = 응답 표시값"이 항상 일치한다.

## 4. 데이터 계약 (DTO)

```jsonc
// AiAskRequest — temperature는 선택(null 허용)
{ "question": "삼성전자 PER 어때?", "temperature": 0.7 }

// AiAskResponse — temperature에는 실제 적용된(resolved) 값
{ "answer": "…AI 답변…", "temperature": 0.7 }
```

두 DTO 모두 record. Swagger `@Schema` 설명은 한국어.

### temperature 결정 규칙

- 요청에 `temperature`가 **있으면** 그 값을 resolved 값으로 사용.
- **없으면(null)** 서비스가 주입받은 기본값을 resolved 값으로 사용.
- resolved 값은 **항상 `ChatOptions`에 명시적으로 넣어** 호출한다(§3). provider/model 기본값에 의존하지 않는다.
- 응답의 `temperature`에는 이 resolved 값을 담아 반환한다 → "실제 호출값 = 응답 표시값" 보장.
- 기본값 주입: `@Value("${ssogssog.ai.default-temperature:0.0}")`.
  **`application.yml`이 `.gitignore` 대상이라 환경마다 값이 없을 수 있으므로, 프로퍼티가 없어도 `0.0`으로
  안전하게 fallback**되도록 기본값을 코드에 둔다. (yml의 `spring.ai.openai.chat.options.temperature`에
  직접 의존하지 않고, 우리 전용 프로퍼티 키를 둬 계약을 안정화한다.)

### 질문 길이 제한

- `question`은 최대 1000자. 초과 시 400(§6).

## 5. 시스템 프롬프트 내용 (`stock-assistant-system.st`)

실제 도메인(`MetricValues` / `StockMetricScreenerCondition`) 기준으로 지원 지표를 명시한다.
**커리큘럼 메모에는 PBR이 있었으나 실제 도메인에는 PBR이 없다(PER만 있음)**. 프롬프트는 실제 도메인 기준으로 작성한다.

지표는 **조회/설명 가능한 지표**와 **스크리닝(필터) 조건으로 지원하는 지표**를 구분한다.
후자는 `StockMetricScreenerCondition`에 실제 필터가 있는 지표만 해당한다(수익률·순이익률 등은 조회는
되지만 스크리너 조건에는 없다). 이 구분은 이후 Function Calling/RAG에서 AI가 "수익률로 스크리닝 가능"
같은 오해를 하지 않도록 하기 위함이다.

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

의도:

- **지원 지표 명시** → AI가 우리 도메인 범위 안에서만 답한다.
- **조회 가능 vs 스크리닝 가능 구분** → Function Calling/RAG 확장 시 "수익률로 스크리닝 가능" 같은 오해 방지.
- **미지원 지표 거절 규칙** → 커리큘럼이 원한 "도메인 규칙 주입"의 핵심 시연.
- **투자 자문 가드레일 + 환각 방지 + 한국어** → 금융 도메인 필수 안전장치.

## 6. 에러 처리

`GeneralException` + `BaseErrorCode`/`ErrorStatus` 규약을 따른다.

신규 에러 코드는 `global/payload/code/status/ErrorStatus.java`에 추가한다.

| 상황 | 처리 |
|---|---|
| `question`이 null/blank | `GeneralException`(신규 `ErrorStatus.AI_QUESTION_REQUIRED`), 400 |
| `question`이 1000자 초과 | `GeneralException`(신규 `ErrorStatus.AI_QUESTION_TOO_LONG`), 400 |
| `temperature`가 범위(0.0~2.0) 밖 | `GeneralException`(신규 `ErrorStatus.AI_TEMPERATURE_OUT_OF_RANGE`), 400 |
| Gemini 호출 실패(429/5xx 등) | Day 1과 동일하게 기존 글로벌 예외 핸들러가 `COMMON500`으로 처리 |

## 7. 테스트

test-patterns 스킬은 구현 단계에서 적용한다. 실제 Gemini 호출은 테스트하지 않는다(외부 의존·비용·rate limit).

- `AiAskService` 단위 테스트 (`ChatClient` 목킹):
  - 시스템 프롬프트 + 질문이 올바르게 조립되는지.
  - temperature 미지정 시 yml 기본값이 적용/반환되는지.
  - temperature 지정 시 그 값이 적용/반환되는지.
  - `temperature` 범위 밖 / 빈 질문 → 예외 발생.
- 컨트롤러 슬라이스 테스트: `POST /ai/ask`가 `ApiResponse<AiAskResponse>` 형태로 응답하는지.
- 실호출 검증: 사용자가 요청할 때 `debug-and-verify-locally`로 수동 확인.

## 8. 범위 밖 / 주의

- **남용 최소 안전장치 (이번 스펙 포함)**: `/ai/ask`는 사용자 질문을 유료 Gemini 호출로 넘기므로
  `/ai/ping`보다 남용/비용 면적이 크다. 정교한 rate limit(Resilience4j 등)은 커리큘럼 **Week 4** 몫이지만,
  그 전까지 노출 면적을 최소화하기 위해 이번 스펙에 두 가지 최소 안전장치를 포함한다.
  - **프로파일 제한**: `AiAskController`를 `@Profile({"local","dev"})`로 제한 → 운영 프로파일에 노출 안 됨.
  - **질문 길이 제한**: `question` 최대 1000자(§4, §6) → 토큰 비용 폭증 방지.
  - 인증·per-user rate limit 등 정교한 통제는 Week 4에서 다룬다.
- **모델**: `application.yml`은 `.gitignore` 대상이며 현재 `gemini-2.5-flash`로 설정되어 있다
  (`gemini-2.0-flash`는 무료 티어 limit 0). 코드/스펙에는 모델을 하드코딩하지 않는다.
- **지표 상세 설명·프롬프트 변수 치환**: 이번 범위 밖(YAGNI). 필요 시 `.st` 파일이라 쉽게 확장 가능.
