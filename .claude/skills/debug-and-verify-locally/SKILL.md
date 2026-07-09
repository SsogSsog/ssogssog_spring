---
name: debug-and-verify-locally
description: Run local Spring Boot verification only when the user explicitly asks for runtime evidence.
---

# Debug And Verify Locally

Use this skill only when the user explicitly asks to run the application, reproduce a runtime issue, or verify behavior locally.

## Workflow

1. State the behavior to verify.
2. Run the smallest useful command.
3. Gather evidence from logs, HTTP responses, or test output.
4. Fix only the relevant issue.
5. Re-run the same verification.

## Commands

- `./gradlew bootRun` for local server execution.
- `./gradlew test` for test evidence.
- `./gradlew build` for full build evidence.

## Runtime Evidence

- Use curl or HTTP client output for endpoint behavior.
- Capture the relevant log lines.
- Report the exact failing class, endpoint, or configuration.
- Do not assume success from a partial boot log.

## Rules

- Do not run local server verification unless explicitly requested.
- Do not leave long-running server sessions active after finishing.
- Prefer a focused test over booting the whole app when a test proves the behavior.
