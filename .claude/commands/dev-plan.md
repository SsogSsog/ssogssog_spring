# /dev-plan

Create a development plan and execute it task by task.

## Procedure

1. Read `CLAUDE.md`, `docs/specs/MAP.md`, and `docs/specs/CONVENTIONS.md`.
2. Analyze `$ARGUMENTS`.
3. Create `docs/plans/YYYY-MM-DD-{feature}.md`.
4. Decompose the work into small tasks.
5. Execute tasks in order and update checkboxes as work progresses.

## Plan Template

```markdown
# {feature}

## Status: In Progress

## Summary
{feature summary}

## Technical Design
- Domain:
- Entities / value objects:
- Ports / adapters:
- API endpoints:
- Dependencies:

## Tasks
- [ ] Domain entity / value object
- [ ] Domain repository and custom QueryDSL contract
- [ ] Application service: api / usecase / writer / port
- [ ] Presentation controller
- [ ] Tests
- [ ] Verify with ./gradlew build and ./gradlew test

## Change Log
| Date | Task | Status |
|---|---|---|
```

## Rules

- Follow `docs/specs/MAP.md`.
- Follow `docs/specs/CONVENTIONS.md`.
- Prefer this implementation order: domain entity/vo -> repository -> application service -> presentation controller -> tests.
- Reuse existing ports before creating new ones.
- Do not leave plan checkboxes stale after completing a task.
