---
name: pre-push-check
description: Pre-push check for tests and documentation synchronization. Never fixes code automatically.
---

# Pre-Push Check

Run this before pushing commits.

## Check 1: Tests

Run:

```bash
./gradlew test
```

Wait for the final result. If tests fail, report the failing class, method, and first useful failure line.

## Check 2: Docs Stay In Sync

Inspect the diff that will be pushed. Flag public behavior changes not reflected in docs, especially:

- REST endpoint additions, removals, or shape changes.
- request or response DTO field changes.
- public error code or response envelope changes.
- renamed domain concepts referenced in docs.

Use `docs/specs/*` as the current sync target. If a code change affects public API behavior and no matching doc update exists, propose a concrete doc edit and wait for user confirmation before applying it.

## Rules

- Do not check commit message quality here; that belongs to pre-commit.
- Do not automatically fix code.
- Do not silently edit docs.
- Do not retry failing tests without user direction.
