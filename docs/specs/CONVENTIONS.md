# Java Conventions

## API And DTO

Controllers return `ApiResponse<T>` and never return JPA entities directly.

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    @Operation(summary = "종목 목록 조회", description = "조건에 맞는 종목 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<StockResponse>> getStocks() {
        return ApiResponse.onSuccess(stockService.getStocks());
    }
}
```

Use records for immutable request, response, value object, and projection data when possible.

```java
@Schema(description = "분기 정보")
public record YearQuarter(
        @Schema(description = "연도") int year,
        @Schema(description = "분기") int quarter
) {
}
```

Swagger `@Operation`, `@Schema`, DTO field descriptions, and Korean-facing generated comments must be written in Korean.

## Errors

Use project error types instead of ad hoc exceptions.

```java
if (stock == null) {
    throw new GeneralException(ErrorStatus.STOCK_NOT_FOUND);
}
```

Add or reuse a `BaseErrorCode` implementation such as `ErrorStatus` when a public error shape is needed.

## QueryDSL Custom Repository

Custom query interfaces live in `domain/{domain}/repository`.

```java
public interface StockMetricRepositoryCustom {
    List<StockMetric> findByCondition(StockMetricScreenerCondition condition);
}
```

Implementations live under `infrastructure/persistence/{domain}/repository`.

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

## Naming

- Controllers: `*Controller`
- API services: `*Service`
- Use cases: `*UseCase`
- Writers: `*Writer`
- Ports: `*Port`
- Adapters: `*Adapter`
- Repositories: `*Repository`
- Custom repository interfaces: `*RepositoryCustom`
- QueryDSL implementations: `*RepositoryImpl`
- Value objects: domain-specific nouns, usually records

## Layer Rules

- Do not call repositories directly from controllers.
- Do not put external API client code in `domain`.
- Do not place domain enums or value objects inside DTO files.
- Do not create a type-based common directory for domain concepts if the concept belongs to one domain.
- Keep transaction boundaries in application services, use cases, or writers.
- Avoid external HTTP calls inside write transactions.
