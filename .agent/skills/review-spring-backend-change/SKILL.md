---
name: review-spring-backend-change
description: Secondary Spring backend review for ssogssog changes. Codex remains the primary reviewer.
---

# Review Spring Backend Change

Codex is the primary reviewer. Use this skill as Claude's secondary review pass.

## Review Order

1. Correctness and broken behavior.
2. Hexagonal dependency violations.
3. API and error contract changes.
4. Persistence and transaction risks.
5. Security and secret exposure.
6. Missing tests.

## Required Checks

- Controllers call application services, not repositories.
- Controllers return `ApiResponse<T>`.
- Controllers do not expose entities.
- Application defines outbound ports; infrastructure implements them.
- Domain has no dependency on presentation, application, or infrastructure.
- Existing ports are reused before new ports are introduced.
- QueryDSL implementations live under `infrastructure/persistence`.
- Errors use `GeneralException` and `BaseErrorCode`/`ErrorStatus`.
- Korean Swagger and DTO descriptions are present for API changes.

## Output

Use:

- `[required]` for issues that should block merge.
- `[recommended]` for important improvements.
- `[note]` for small observations.

Lead with findings. If there are no findings, say that clearly and mention any unverified tests.
