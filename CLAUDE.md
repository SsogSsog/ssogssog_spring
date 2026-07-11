# ssogssog Agent Guide

## Project

ssogssog is a Spring Boot backend for stock data collection and screening.

- Package root: `org.project.ssogssog`
- Runtime: Java 21
- Framework: Spring Boot 3.5.8
- Build: Gradle
- Persistence: JPA/Hibernate, QueryDSL 5.0, MySQL
- Infrastructure: Redis, Caffeine, ShedLock, Actuator
- API docs: springdoc-openapi

## Architecture

This project follows a hexagonal layout under `src/main/java/org/project/ssogssog`.

```text
presentation/
  controller/{domain}/
application/
  service/{domain}/
    api/
    usecase/
    writer/
    port/
  utils/
domain/
  {domain}/
    entity/
    vo/
    enums/
    repository/
    factory/
    projection/
    policy/
infrastructure/
  config/
  client/{opendart,ksi,naver}/
  adapter/
  scheduler/
  persistence/
global/
  payload/
  paging/
```

The `ksi` client folder is the current path for Korea Investment client code (`KISClient.java`). Keep that path unless a separate source rename task is approved.

Dependency rules:

- `presentation` calls `application` and may use `global` response/error types.
- `application` coordinates use cases, defines outbound ports, and depends on `domain` and `global`.
- `application/service/{domain}/port` contains outbound ports such as `StockPort`, `DailyPricePort`, `StockIssuePort`, and `StockFinancialPort`.
- `domain` contains business concepts and must not depend on `presentation`, `application`, or `infrastructure`.
- `domain/{domain}/projection` contains read models. `domain/{domain}/policy` contains domain policies and registries.
- `infrastructure` implements adapters, clients, persistence implementations, schedulers, and config.
- `global` contains shared payload, error, and paging support.

## Coding Rules

- Prefer Java `record` for value objects and DTOs when mutation is unnecessary.
- Use Lombok where it matches existing entity and service patterns.
- Prefer `final` for local variables and fields when practical.
- Controllers return `ApiResponse<T>`. Do not expose JPA entities from controllers.
- Standard errors go through `GeneralException` and `BaseErrorCode`/`ErrorStatus`.
- Swagger `@Operation` and `@Schema` descriptions, DTO field descriptions, and code comments generated for Korean-facing APIs must be written in Korean.
- Keep controllers thin. Put orchestration in `application` services and domain decisions in `domain`.
- For complex database reads, use QueryDSL through `*RepositoryCustom` plus an implementation under `infrastructure/persistence`.

## Branch And Commit

- Branch: `{name}/{purpose}/{desc}`
- Purpose examples: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`
- Commit: `{purpose}({scope}): {desc}` or `{purpose}: {desc}`

## Build And Verify

- `./gradlew build`
- `./gradlew test`
- `./gradlew bootRun`

This project has no separate lint or format Gradle task. Use the commands above unless a task adds a new verifier.

## Git Hooks

Two hooks live in `.githooks/`: `pre-commit` blocks commits that contain hardcoded secrets or a force-staged ignored `application.yml`, and `pre-push` runs `./gradlew test` and blocks the push on failure. Because `core.hooksPath` is stored in local `.git/config`, it is not applied automatically on clone. After cloning, activate it once:

```
git config core.hooksPath .githooks
```

The hooks are automated safety nets for `pre-commit-check` Check 1 and `pre-push-check` Check 1; still run the `pre-commit-check` skill before committing and the `pre-push-check` skill before pushing (its docs-sync check cannot be automated). Bypass intentionally with `git commit --no-verify` / `git push --no-verify`.

## Workflow

- Spec authoring: superpowers, usually `brainstorming` then `writing-plans`.
- Spec review and code review: Codex, triggered externally by the user.
- Code writing: Claude, using project skills such as `implement-spring-backend-feature`, `jpa-patterns`, and `test-patterns`.
- Local run verification: Claude uses `debug-and-verify-locally` only when the user explicitly asks for local execution.
- Commit gate: Claude uses `pre-commit-check`.
- Push gate: Claude uses `pre-push-check`.

## Commands And Skills

Commands:

- `/dev-plan`
- `/review`
- `/harness-update`

Skills:

- `implement-spring-backend-feature`
- `review-spring-backend-change`
- `test-patterns`
- `jpa-patterns`
- `debug-and-verify-locally`
- `select-spring-design-pattern`
- `organize-domain-model`
- `fill-github-template`
- `pre-commit-check`
- `pre-push-check`
