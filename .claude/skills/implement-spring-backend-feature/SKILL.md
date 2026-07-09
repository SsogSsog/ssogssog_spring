---
name: implement-spring-backend-feature
description: Implement Spring Boot backend features across hexagonal layers, API services, ports, JPA, exceptions, and tests. Not for PR review.
---

# Implement Spring Backend Feature

Use this skill when implementing or modifying a Spring Boot backend feature.

## Use This For

- New API endpoints
- Application service, use case, writer, or port logic
- Request and response DTOs
- JPA entities, repositories, and QueryDSL queries
- Transaction boundaries
- Custom exceptions and error responses
- Unit, slice, or integration tests

## Do Not Use This For

- Reviewing a completed diff
- Only checking merge readiness
- Pure style review
- Adding patterns without a real requirement

## Core Principle

Start with the simplest working design. Add Factory, Strategy, Event, Decorator, or Adapter only when the requirement clearly needs it.

## Implementation Flow

1. Read `docs/specs/MAP.md` and `docs/specs/CONVENTIONS.md`.
2. Define the feature goal and public contract.
3. Add or update domain entity, value object, enum, factory, projection, or policy.
4. Add repository contracts, including `*RepositoryCustom` for complex QueryDSL reads.
5. Add or reuse application ports under `application/service/{domain}/port`.
6. Implement application service, use case, or writer.
7. Implement infrastructure adapter, client, or persistence implementation when needed.
8. Add presentation controller returning `ApiResponse<T>`.
9. Add tests.
10. Verify with `./gradlew test` or `./gradlew build`.

## API / Controller

- Keep controllers thin.
- Use proper HTTP methods.
- Validate request DTOs.
- Return `ApiResponse<T>`.
- Never expose JPA entities.
- Write Swagger `@Operation` and DTO `@Schema` descriptions in Korean.

## Application Layer

- Put orchestration in `api`, `usecase`, and `writer`.
- Define outbound interfaces in `port`.
- Avoid external HTTP calls inside write transactions.
- Use `@Transactional(readOnly = true)` for read-only service methods when useful.

## Domain Layer

- Keep business rules in domain objects, factories, or policies.
- Place read models in `projection`.
- Place value objects in `vo` and enums in `enums`.
- Do not import infrastructure types into domain code.

## Persistence

- Use repositories around use cases, not generic data access convenience.
- Use QueryDSL for complex filtering.
- Put repository interfaces in `domain/{domain}/repository`.
- Put QueryDSL implementations in `infrastructure/persistence/{domain}/repository`.
- Check pagination and N+1 risks.

## Errors

- Throw `GeneralException` with `ErrorStatus` or another `BaseErrorCode`.
- Do not create ad hoc public error shapes.
- Keep error names specific and stable.

## Tests

- Test important behavior, not implementation trivia.
- Use unit tests for business logic and use cases.
- Add repository tests when query behavior is complex.
