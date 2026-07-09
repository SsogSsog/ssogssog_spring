---
name: select-spring-design-pattern
description: Choose a Spring-friendly design pattern only when a concrete backend problem needs it.
---

# Select Spring Design Pattern

Use this skill when implementation logic is becoming hard to extend or test and a known pattern may reduce real complexity.

## Default

Prefer straightforward service and domain code. Do not add a pattern for style alone.

## Pattern Choices

- Builder: complex object creation with many optional fields.
- Factory: domain object creation with validation or calculation.
- Strategy: interchangeable algorithms selected by condition.
- Spring Event: after-commit side effects or decoupled notifications.
- Decorator: cross-cutting behavior around an existing interface.
- Adapter: infrastructure implementation of an application port.

## Selection Process

1. Name the concrete problem.
2. Check whether a simple method or class is enough.
3. Choose the smallest pattern that removes duplication or clarifies ownership.
4. Keep interfaces in the layer that owns the abstraction.
5. Verify the dependency direction from `docs/specs/MAP.md`.

## Examples

- Use a factory for `StockMetric` calculation creation rules.
- Use strategy when multiple screening algorithms share one contract.
- Use an adapter when implementing `StockPort` against an external provider.
- Use events for side effects that must happen after a committed write.
