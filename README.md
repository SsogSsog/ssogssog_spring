# 📈 주식필터 (Stock Filter)

> **"데이터로 찾는 투자의 확신"**

**주식필터**는 다양한 재무 지표와 복합적인 조건을 기준으로 주식 종목을 정밀하게 필터링할 수 있는 **주식 스크리너 서비스**입니다.
흩어져 있는 공시·시세 데이터를 한곳에 모아, 나만의 투자 전략에 맞는 종목을 효율적으로 탐색해 보세요.

<br>

## 📱 주요 기능 미리보기

| | |
| :---: | :---: |
| **🏠 홈 / 초보자 필터 / 테마**<br>![Image](https://github.com/user-attachments/assets/ac4637a1-750a-46a9-9a56-34e2e574d9a3) | **🔎 스크리너 조건 설정**<br>![Image](https://github.com/user-attachments/assets/f55f7310-0a8b-4ad4-b43a-523a2d75e507) |
| **📊 주식 종목 상세 조회**<br>![Image](https://github.com/user-attachments/assets/f55f7310-0a8b-4ad4-b43a-523a2d75e507) | **💾 전략(저장된 필터) 조회**<br>![Image](https://github.com/user-attachments/assets/6ab596b5-fd94-43ff-83e6-1b8e0f9be12c) |

<br>

## 🛠️ 기술 스택 (Tech Stack)

| 분류 | 기술                                                                                                                                                                                                                                                                                           |
|:---:|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Framework** | ![SpringBoot](https://img.shields.io/badge/Spring_Boot_3.5.3-6DB33F?style=for-the-badge&logo=spring&logoColor=white) ![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)                                                                   |
| **Database** | ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white) ![JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white) ![QueryDSL](https://img.shields.io/badge/QueryDSL-0078D7?style=for-the-badge) |
| **Cache** | ![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white) ![Caffeine](https://img.shields.io/badge/Caffeine_Cache-555555?style=for-the-badge&logo=caffeine&logoColor=white)                                                                         |
| **Interface** | ![Feign](https://img.shields.io/badge/Spring_Cloud_OpenFeign-000000?style=for-the-badge&logo=spring&logoColor=white)                                                                                                                                                                         |
| **Resilience** | ![Resilience4j](https://img.shields.io/badge/Resilience4j-3D5A80?style=for-the-badge)                                                                                                                                                                                                        |
| **Scheduling** | ![Scheduler](https://img.shields.io/badge/Spring_Scheduler-6DB33F?style=for-the-badge&logo=spring&logoColor=white) ![ShedLock](https://img.shields.io/badge/ShedLock-2C3E50?style=for-the-badge)                                                                                             |

<br>

## 🏗️ 아키텍처

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

## 🚀 주요 기술적 도전 (Challenges)

### 1. 외부 API 회복탄력성 (Resilience4j)
- **문제:** 외부 API의 간헐적 장애가 전체 서비스의 데이터 정합성을 위협
- **해결:** Resilience4j의 4중 방어 패턴 적용 (`CircuitBreaker`, `Retry`, `RateLimiter`, `TimeLimiter`)
- **성과:** 부분 장애 발생 시에도 전체 시스템 셧다운 없이 **서비스 가용성 확보**
- [👉 📄 기술 블로그 포스팅 보러가기](https://actually-drive-a39.notion.site/2fa4a42baed680e387cff0c9f7fe496e?pvs=74)

### 2. 배치 처리 최적화 (960% 성능 개선)
- **문제:** 3,000개 종목의 지표 계산 시 N*M 문제로 인한 심각한 성능 저하 (15초 소요)
- **해결:** `Bulk Query`로 데이터 조회 후 메모리 상에서 `Map` 구조로 매핑하여 DB 접근 최소화, 비상관 서브 쿼리 및 인덱스 튜닝
- **성과:** 메트릭 계산 시간 **15초 → 1.5초 **
- [👉 📄 기술 블로그 포스팅 보러가기](https://actually-drive-a39.notion.site/AI-CS-960-15s-1-5s-2f24a42baed681d1a3dbd705e1b6ff8b)

### 3. 헥사고날 아키텍처 도입
- **목표:** 외부 인프라(API, DB) 변경이 핵심 비즈니스 로직(도메인)에 영향을 주지 않는 구조 설계
- **해결:** DIP(의존성 역전 원칙)를 적용하여 도메인은 오직 내부 인터페이스(Port)에만 의존하고, 구현체(Adapter)가 이를 의존하도록 설계
- [👉 📄 기술 블로그 포스팅 보러가기](https://actually-drive-a39.notion.site/2e14a42baed680838ff2e321138d7b52?pvs=74)