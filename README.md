# 주식필터 

주식필터는 다양한 재무 지표와 조건을 기준으로 주식 종목을 필터링할 수 있는 주식 스크리너 서비스입니다.
공시·시세 데이터를 활용해 관심 종목을 효율적으로 탐색할 수 있습니다.

## 📱 주요 기능 미리보기

| 홈 / 초보자 필터 / 테마 조회 |                                           스크리너 조건 설정                        |
| :---: |:---------------------------------------------------------------------------:|
| ![Image](https://github.com/user-attachments/assets/ac4637a1-750a-46a9-9a56-34e2e574d9a3) |   ![Image](https://github.com/user-attachments/assets/f55f7310-0a8b-4ad4-b43a-523a2d75e507) 
|
| **주식 종목 상세 조회** |                                       **전략(저장된 필터) 조회**                     |
| ![Image](https://github.com/user-attachments/assets/f55f7310-0a8b-4ad4-b43a-523a2d75e507) |    ![Image](https://github.com/user-attachments/assets/6ab596b5-fd94-43ff-83e6-1b8e0f9be12c)     |

<br>

## 기술 스택

| 분류 | 기술                                                             |
|-----|----------------------------------------------------------------|
| **Framework** | Spring Boot 3.5.3, Java 21                                     |
| **Database** | MySQL, Spring Data JPA, QueryDSL                               |
| **Cache** | Caffeine, Redis                                                |
| **외부 API** | Spring Cloud OpenFeign (KIS, OpenDART, Naver)                  |
| **회복탄력성** | Resilience4j (CircuitBreaker, Retry, RateLimiter, TimeLimiter) |
| **스케줄링** | Spring Scheduler, ShedLock                                     |

<br>

## 아키텍처

헥사고날(포트/어댑터) 아키텍처와 DDD 전술 패턴을 기반으로 설계되었습니다.

```
src/main/java/org/project/ssogssog/
├── presentation/          # REST Controller
├── application/           # Application Layer
├── domain/                # Domain Layer (서브 도메인별 분리)
├── infrastructure/        # Infrastructure Layer
└── global/                # 공통 모듈
```

### Application Layer

CQRS 패턴을 적용하여 Reader/Writer로 책임을 분리하고, Shared Kernel로 도메인 간 공유 객체를 관리합니다.

```
application/
├── common/                      # Shared Kernel (전략적 설계)
│   └── dto/condition/           # 도메인 간 공유되는 검색 조건 DTO
├── service/
│   ├── stock/
│   │   ├── port/                # 외부 시스템 인터페이스 (Port)
│   │   ├── collect/             # 데이터 수집 UseCase
│   │   ├── reader/              # 조회 전용 서비스 (CQRS - Query)
│   │   ├── writer/              # 저장 전용 서비스 (CQRS - Command)
│   │   └── api/                 # API 서비스
│   ├── stockmetric/
│   │   ├── collect/             # 메트릭 수집 UseCase
│   │   ├── reader/              # Bulk 데이터 조회
│   │   ├── writer/              # Bulk 데이터 저장
│   │   └── api/                 # API 서비스
│   └── member/
│       ├── reader/              # 캐시 기반 조회
│       └── api/                 # API 서비스
└── utils/                       # 공통 유틸리티
```

### Domain Layer

서브 도메인별로 분리하고, DDD 전술 패턴(Entity, VO, Factory, Repository 등)을 적용했습니다.

```
domain/
├── stock/                       # 주식 도메인
│   ├── entity/                  # Stock, DailyPrice, StockFinancial
│   ├── enums/                   # MarketType 등
│   ├── policy/                  # 도메인 정책 (ThemeEmojiRegistry)
│   ├── projection/              # 쿼리 전용 DTO
│   └── repository/              # Repository 인터페이스
│
├── stockmetric/                 # 투자 지표 도메인
│   ├── entity/                  # StockMetric (Aggregate Root)
│   ├── vo/                      # MetricValues (Value Object)
│   ├── factory/                 # StockMetricCalculator (Factory)
│   ├── enums/                   # 지표 관련 열거형
│   └── repository/              # Repository 인터페이스
│
└── member/                      # 회원 도메인
    ├── entity/                  # Member, Strategy, StockLike
    │   └── range/               # 필터 범위 VO (Embedded)
    ├── factory/                 # StrategyFactory
    └── repository/              # Repository 인터페이스
```

### Infrastructure Layer

```
infrastructure/
├── adapter/stock/               # Port 구현체 (Adapter)
├── client/feign/                # Feign Client (KIS, OpenDART, Naver)
├── scheduler/                   # 스케줄러
└── config/                      # 설정 (Cache, Async, Resilience4j)
```
<br>

## 외부 API 연동

| 에러 유형 | KIS API | OpenDART API | Naver API |
|---------|--------|--------------|-----------|
| **403 (토큰)** | 1분 대기 → 2회 재시도 → Circuit Breaker | - | - |
| **429 (Rate Limit)** | 1초 20회 제한<br/>→ 1초 대기 후 재시도 | 1초 10회 제한<br/>→ 1초 대기 후 재시도 | 1초 10회 제한<br/>→ 1초 대기 후 재시도 |
| **5xx (서버 오류)** | 지수 백오프 2~3회 | 지수 백오프 2~3회 | 지수 백오프 2~3회 |
| **Timeout** | - | 응답 지연 시 Timeout 처리 및 Skip | - |
| **Circuit Breaker** | 연속 실패 시 호출 차단 | 연속 실패 시 호출 차단 | API 사용량 보호 목적 |

<br>

## 주요 기술적 도전

### 1. 외부 API 회복탄력성
- Resilience4j 4중 방어 패턴 적용 (CircuitBreaker, Retry, RateLimiter, TimeLimiter)
- 부분 장애 시 서비스 영향 최소화
- https://actually-drive-a39.notion.site/2fa4a42baed680e387cff0c9f7fe496e?pvs=74

### 2. 배치 처리 최적화(960% 성능 개선)
- N*M DB 접근 문제 해결: Bulk Query + 메모리 Map 구조
- 비상관 서브 쿼리 및 인덱스 적용: 쿼리 최적화
- 3,000개 종목 메트릭 계산 시간 15초 → 1.5초
- https://actually-drive-a39.notion.site/AI-CS-960-15s-1-5s-2f24a42baed681d1a3dbd705e1b6ff8b

### 3. 핵사고날 아키텍처 적용
- 외부 인프라 수정에도 도메인 오염이 되지 않도록 아키텍처 설계
- https://actually-drive-a39.notion.site/2e14a42baed680838ff2e321138d7b52?pvs=74
