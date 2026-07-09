# Agent Entry Point

Read this file before working in the ssogssog repository.

## Required Reading

1. `CLAUDE.md` for project workflow, stack, and role split.
2. `docs/specs/MAP.md` for package layout and dependency direction.
3. `docs/specs/CONVENTIONS.md` for Java, API, repository, error, and documentation rules.

## Pre-Work Checklist

- Confirm the target package under `src/main/java/org/project/ssogssog`.
- Follow the hexagonal dependency rules before adding imports.
- Reuse existing ports under `application/service/{domain}/port` before creating new adapter paths.
- Keep read models in `domain/{domain}/projection` and domain policies in `domain/{domain}/policy`.
- Use `ApiResponse<T>` from controllers and standard errors through `GeneralException`.
- Verify with `./gradlew build` or `./gradlew test` when the task calls for it.
- Do not modify `src/` for harness-only work.

## Default Collaboration

Codex is the primary reviewer for specs and code. Claude may perform secondary checks through the project review command or review skill, but final review judgment belongs to Codex or a human reviewer.
