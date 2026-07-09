---
name: organize-domain-model
description: Place ssogssog domain models, value objects, enums, projections, and policies in the correct hexagonal packages.
---

# Organize Domain Model

Use this skill when adding or moving domain concepts.

## Placement

- Entity: `domain/{domain}/entity`
- Value object: `domain/{domain}/vo`
- Enum: `domain/{domain}/enums`
- Repository contract: `domain/{domain}/repository`
- Factory or calculator: `domain/{domain}/factory`
- Read model: `domain/{domain}/projection`
- Policy or registry: `domain/{domain}/policy`

## Rules

- Do not place domain enums or value objects inside DTO files.
- Do not create generic type-based folders for concepts that belong to one domain.
- Keep API DTOs in `application/service/{domain}/api/dto` unless they are pure domain projections.
- Keep use-case DTOs in `application/service/{domain}/usecase/dto`.
- If a class has business meaning independent of one endpoint, it probably belongs in `domain`.
- If a class exists only to shape an API response, it belongs in application DTOs.

## Read Models

Use `projection` for query result shapes that represent domain reads and may be reused by services or repository queries. Keep controller-specific response formatting in API response DTOs.
