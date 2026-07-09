# Architecture Map

This document describes the actual ssogssog package layout and dependency rules.

## Package Tree

```text
org.project.ssogssog
├── presentation
│   └── controller/{domain}
├── application
│   ├── service/{domain}
│   │   ├── api
│   │   ├── usecase
│   │   ├── writer
│   │   └── port
│   └── utils
├── domain
│   └── {domain}
│       ├── entity
│       ├── vo
│       ├── enums
│       ├── repository
│       ├── factory
│       ├── projection
│       └── policy
├── infrastructure
│   ├── adapter
│   ├── client/{opendart,ksi,naver}
│   ├── config
│   ├── persistence
│   └── scheduler
└── global
    ├── payload
    └── paging
```

The Korea Investment client currently lives in `infrastructure/client/ksi/KISClient.java`. Keep the current folder name in references and imports unless a separate source rename is approved.

## Request Flow

Inbound HTTP flow:

```text
controller -> service/api -> usecase/writer -> domain/repository
```

Outbound integration flow:

```text
usecase -> application port -> infrastructure adapter -> infrastructure client
```

Examples of existing outbound ports:

- `StockPort`
- `DailyPricePort`
- `StockIssuePort`
- `StockFinancialPort`

Reuse these ports when adding stock data collection behavior. Create a new port only when the required external capability is not represented.

## Dependency Direction

- `presentation` may depend on `application` and `global`.
- `application` may depend on `domain` and `global`.
- `application` defines outbound ports; `infrastructure` implements them.
- `domain` must not depend on `presentation`, `application`, or `infrastructure`.
- `infrastructure` may depend on `application` ports and `domain` models to implement adapters and persistence.
- `global` is shared support and should not contain business rules.

## Placement Rules

- REST controllers: `presentation/controller/{domain}`.
- API-facing services and DTOs: `application/service/{domain}/api`.
- Use-case orchestration: `application/service/{domain}/usecase`.
- Write/persist orchestration: `application/service/{domain}/writer`.
- Outbound port interfaces: `application/service/{domain}/port`.
- JPA entities: `domain/{domain}/entity`.
- Value objects: `domain/{domain}/vo`.
- Domain enums: `domain/{domain}/enums`.
- Domain repositories and custom repository interfaces: `domain/{domain}/repository`.
- Calculation or creation logic: `domain/{domain}/factory`.
- Read models: `domain/{domain}/projection`.
- Domain policies and registries: `domain/{domain}/policy`.
- QueryDSL implementations: `infrastructure/persistence/{domain}/repository`.
- External API clients: `infrastructure/client/{provider}`.
- Adapter implementations: `infrastructure/adapter/{domain}`.
