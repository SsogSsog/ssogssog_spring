---
name: test-patterns
description: Add focused Java and JUnit 5 tests for ssogssog backend behavior.
---

# Test Patterns

Use this skill when adding or changing tests.

## Principle

Test important behavior and observable contracts. Avoid tests that only freeze private implementation details.

## Locations

Tests live under `src/test/java/org/project/ssogssog/`.

## What To Test

- Use cases and writers: business rules, branching, transaction-visible results.
- Services: API contract decisions and orchestration.
- Repositories: QueryDSL filtering, pagination, joins, and projections.
- Controllers: request validation, response envelope, status, and error mapping when needed.

## Rules

- Use JUnit 5.
- Keep test data small and explicit.
- Name tests after behavior.
- Do not disable new tests without explaining why.
- Run `./gradlew test` after test changes when practical.

## Test Shape

```java
@Test
void returnsOnlyMetricsMatchingCondition() {
    final StockMetricScreenerCondition condition = new StockMetricScreenerCondition(...);

    final List<StockMetric> result = repository.findByCondition(condition);

    assertThat(result).allMatch(metric -> metric.matches(condition));
}
```
