# /harness-update

Update the agent harness when project conventions, architecture, or workflow changes.

## Managed Files

| Area | Files |
|---|---|
| Root guides | `CLAUDE.md`, `AGENT.md` |
| Specs | `docs/specs/MAP.md`, `docs/specs/CONVENTIONS.md` |
| Claude commands | `.claude/commands/*` |
| Claude skills | `.claude/skills/*/SKILL.md` |
| Agent mirror | `.agent/commands/*`, `.agent/skills/*/SKILL.md` |
| Future hooks | `.githooks/*` if added later |
| CI | `.github/workflows/*` if present |

## Procedure

1. Identify the changed convention or workflow.
2. Update the canonical `.claude` files first.
3. Mirror command and skill changes into `.agent`.
4. Keep `AGENT.md`, `CLAUDE.md`, `MAP.md`, and `CONVENTIONS.md` synchronized.
5. Run `./gradlew build` if application code or build behavior is affected.
6. Run a residue sweep for obsolete reference names and broken doc paths.

## Rules

- Do not edit application code for a harness-only update.
- Do not add new verification tools unless the project build actually supports them.
- Keep AI-facing harness docs in English.
- Keep Korean Swagger and DTO description rules intact.
