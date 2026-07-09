# /review

Codex is the primary reviewer. This command is Claude's secondary pass for a local review before Codex or a human makes the final call.

## Procedure

1. Read `CLAUDE.md`, `docs/specs/MAP.md`, and `docs/specs/CONVENTIONS.md`.
2. Inspect the relevant diff or files from `$ARGUMENTS`.
3. Report findings first, ordered by severity.
4. Use these tags:
   - `[required]` for correctness, security, data loss, broken build, or architecture violations.
   - `[recommended]` for maintainability or test gaps.
   - `[note]` for minor observations.

## Review Lenses

- Hexagonal dependency direction:
  - controllers call application services, not repositories.
  - application defines ports and infrastructure implements them.
  - domain has no outward dependency.
- API correctness:
  - controllers return `ApiResponse<T>`.
  - controllers do not expose entities.
  - request validation is present where needed.
- Error handling:
  - use `GeneralException` with `BaseErrorCode`/`ErrorStatus`.
  - do not expose internal exception details.
- Persistence:
  - custom queries use `*RepositoryCustom` and implementation under `infrastructure/persistence`.
  - pagination and N+1 risk are considered.
- Security:
  - no secrets or sensitive config in committed files.
  - `src/main/resources/application.yml` is ignored; flag it if force-staged.
- Tests:
  - changed behavior has focused tests.
  - repository/query behavior has suitable coverage when logic is non-trivial.

## Output

Lead with findings. If no issues are found, say so and mention any residual test risk.
