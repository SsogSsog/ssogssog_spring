---
name: jpa-patterns
description: Apply JPA and QueryDSL patterns for ssogssog repositories, transactions, lazy loading, and query performance.
---

# JPA Patterns

Use this skill when changing entities, repositories, QueryDSL queries, or transaction behavior.

## Repository Placement

- Repository interfaces: `domain/{domain}/repository`.
- Custom query interface: `*RepositoryCustom`.
- QueryDSL implementation: `infrastructure/persistence/{domain}/repository/*RepositoryImpl`.

## Query Rules

- Do not load all rows for pageable screens.
- Use QueryDSL for dynamic filters and complex joins.
- Check N+1 risk whenever returning associated data.
- Use fetch join or a projection when the screen needs related data.
- Prefer read projections for list screens instead of exposing entities.

## Transaction Rules

- Put transaction boundaries at application service, use case, or writer level.
- Use `@Transactional(readOnly = true)` for read-only workflows.
- Keep external client calls outside write transactions.
- Ensure multi-step writes succeed or fail together.

## Example Shape

```java
public interface StockMetricRepositoryCustom {
    List<StockMetric> findByCondition(StockMetricScreenerCondition condition);
}
```

```java
@Repository
@RequiredArgsConstructor
public class StockMetricRepositoryImpl implements StockMetricRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<StockMetric> findByCondition(final StockMetricScreenerCondition condition) {
        return queryFactory
                .selectFrom(stockMetric)
                .where(StockMetricPredicate.byCondition(condition))
                .fetch();
    }
}
```
