---
name: pre-commit-check
description: Lightweight staged-diff check before creating a commit. Checks only secret exposure and commit message quality.
---

# Pre-Commit Check

Run this right before creating a commit.

## Scope

Only inspect staged changes with `git diff --cached`.

## Check 1: Secret Or Sensitive Data

Block and report if staged files contain:

- hardcoded passwords, tokens, API keys, or DB credentials.
- real-looking personal data that is not a test fixture.
- `.env`-style files.
- `src/main/resources/application.yml` force-staged despite being ignored.
- long access-token shapes or known cloud credential prefixes.

Point to the file and line. Do not unstage automatically.

## Check 2: Commit Message

Expected message format:

- `{purpose}({scope}): {desc}`
- `{purpose}: {desc}`

Purpose examples: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `ci`.

Compare the message with the staged diff. If it is vague or mismatched, suggest one or two better messages.

## Output

Keep it short. If both checks pass:

```text
pre-commit check passed: no secrets found, commit message is suitable.
```

## Do Not Do

- Do not run tests.
- Do not rewrite staged files.
- Do not unstage files.
- Do not perform a full code review.
