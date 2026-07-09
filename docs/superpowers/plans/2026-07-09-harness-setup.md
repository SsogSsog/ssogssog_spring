# Harness Setup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Set up an agent harness for the ssogssog Spring project by adapting the 18th-team6-server harness (base) plus Artium `.agent` additions to our Java 21 / hexagonal stack, mirrored into both `.claude/` and `.agent/`.

**Architecture:** Copy the reference structure (commands + skills + settings + root docs), then rewrite content for our stack. AI-facing harness docs are written in **English** to save tokens. `.claude/` and `.agent/` hold an identical set. No `src/` application code is touched.

**Tech Stack:** Spring Boot 3.5.8, Java 21, Gradle, JPA/Hibernate, QueryDSL 5.0, MySQL, Redis, Caffeine, ShedLock, Actuator, springdoc-openapi.

## Global Constraints

- Language: all AI-facing harness files (CLAUDE.md, AGENT.md, `docs/specs/*`, all SKILL.md, all command `.md`) are written in **English**. This plan file and future code comments/Swagger stay Korean.
- Stack replacements applied everywhere: Kotlin→Java 21, `val`/`data class`→`final`/`record`/Lombok, DDD→hexagonal (`presentation`/`application`/`domain`/`infrastructure`/`global`), `./gradlew harness`+ktlint→`./gradlew build`/`./gradlew test`, package `depromeet.hotsix.obrit`→`org.project.ssogssog`.
- No ktlint / ArchUnit / `harness` gradle task references (not present in our project).
- No `data.sql` / H2 seed references (not applicable).
- Do NOT modify `src/`, `build.gradle`, `application.yml`, or existing app code.
- Reference base: `/Users/imjunhyeon/depromeet/18th-team6-server`. Additions: `/Users/imjunhyeon/Artium-Server/.agent`.
- Work happens on branch `chore/harness-setup` (already created; design doc already committed).
- `.claude/` and `.agent/` get the SAME command + skill set. Author each file once, then copy to both trees.

## File Structure

Authoring convention: build everything under `.claude/` first (Claude's canonical tree), then mirror the whole `commands/` + `skills/` set into `.agent/`. `settings.local.json` is authored per-tree.

```
AGENT.md                                  # entry point (English, our project)
CLAUDE.md                                 # project guide (English, our stack)
docs/specs/MAP.md                         # hexagonal architecture map (English)
docs/specs/CONVENTIONS.md                 # Java conventions (English)
.claude/settings.local.json              # our permission allowlist
.claude/commands/{dev-plan,review,harness-update}.md
.claude/skills/<10 skills>/SKILL.md
.agent/settings.local.json               # our permission allowlist
.agent/commands/  (mirror of .claude/commands)
.agent/skills/    (mirror of .claude/skills)
```

10 skills: implement-spring-backend-feature, review-spring-backend-change, test-patterns, jpa-patterns, debug-and-verify-locally, select-spring-design-pattern, organize-domain-model, fill-github-template, pre-commit-check, pre-push-check.

---

### Task 1: Project docs — CLAUDE.md, AGENT.md, docs/specs

**Files:**
- Create: `CLAUDE.md`
- Create: `AGENT.md`
- Create: `docs/specs/MAP.md`
- Create: `docs/specs/CONVENTIONS.md`

**Interfaces:**
- Produces: canonical references that AGENT.md and every skill/command link to — `CLAUDE.md`, `docs/specs/MAP.md`, `docs/specs/CONVENTIONS.md`. Later tasks link to these exact paths.

- [ ] **Step 1: Write `CLAUDE.md`** (English). Sections, adapted from the reference CLAUDE.md but rewritten for our stack:
  - Project overview: name ssogssog, stack (Spring Boot 3.5.8, Java 21, Gradle), package `org.project.ssogssog`.
  - Architecture (hexagonal): describe `presentation/controller/{domain}`, `application/service/{domain}/{api,usecase,writer,port}`, `domain/{domain}/{entity,vo,enums,repository,factory,projection,policy}`, `infrastructure/{config,client,adapter,scheduler,persistence}`, `global/{payload,paging}`. Note the outbound ports under `application/service/{domain}/port` (e.g. `StockPort`, `DailyPricePort`, `StockIssuePort`, `StockFinancialPort`) that `infrastructure` implements; read models live in `domain/{domain}/projection`; domain policies/registries in `domain/{domain}/policy` (e.g. `ThemeEmojiRegistry`). External clients under `infrastructure/client/{opendart,ksi,naver}` — the KIS (Korea Investment) client folder is literally `ksi` (`KISClient.java`); a typo, but src renaming is out of scope, so document the current path. Include dependency direction rules (presentation → application + global; application → domain + global; application defines ports, infrastructure implements them; domain has no outward deps; global is shared).
  - Coding conventions: `record` for VO/DTO, Lombok, `final` preferred, return `ApiResponse<T>` from controllers (never entities), standardize errors via `GeneralException` + `BaseErrorCode`/`ErrorStatus`. **Controller KDoc/Swagger `@Operation`/`@Schema` descriptions and DTO field descriptions in Korean** (this is the one Korean-output rule).
  - Branch convention: `{name}/{purpose}/{desc}` (purpose: feat/fix/refactor/chore/docs/test).
  - Commit convention: `{purpose}({scope}): {desc}` or `{purpose}: {desc}`.
  - Build & verify: `./gradlew build`, `./gradlew test`, `./gradlew bootRun`. Do NOT reference a `harness` gradle task or a Kotlin lint formatter — this project has neither. (State this as "this project has no separate lint/format gradle task"; avoid writing the literal tool names so the residue grep stays clean.)
  - Workflow / role split (the key section): spec authoring = superpowers (brainstorming → writing-plans); spec review & code review = **Codex** (external, user-driven); code writing = Claude (implement-spring-backend-feature, jpa-patterns, test-patterns); local run/verify = Claude via debug-and-verify-locally **only when the user explicitly asks**; pre-commit / pre-push checks = Claude.
  - Skills list: reference `/dev-plan`, `/review`, `/harness-update` and the available skills.

- [ ] **Step 2: Write `AGENT.md`** (English). Entry point. Point to: `CLAUDE.md` (read first), `docs/specs/MAP.md`, `docs/specs/CONVENTIONS.md`. Pre-work checklist (read CLAUDE.md, understand target package structure, know dependency rules, know `./gradlew build`/`test`). **Only reference files that exist** — no `docs/HARNESS_GUIDE.md`, no `docs/specs/ADR/`, no `EXECUTION_PLAN.md`.

- [ ] **Step 3: Write `docs/specs/MAP.md`** (English). Architecture map: the hexagonal package tree (from design doc §4, including `application/service/{domain}/port`, `domain/{domain}/projection`, `domain/{domain}/policy`), request flow (`controller → service(api/usecase/writer) → port → infrastructure` for outbound, and `service → domain/repository` for persistence), dependency direction rules (application defines ports, infrastructure implements them), and where external clients live (`infrastructure/client/{opendart,ksi,naver}` — note `ksi` is the actual KIS client folder name, a typo kept as-is). List existing ports: `StockPort`, `DailyPricePort`, `StockIssuePort`, `StockFinancialPort`, so agents reuse them instead of adding new persistence paths.

- [ ] **Step 4: Write `docs/specs/CONVENTIONS.md`** (English). Detailed conventions with Java code examples: `record` VO example, controller returning `ApiResponse`, exception via `GeneralException`, QueryDSL custom repository pattern (`*RepositoryCustom` + impl), naming suffixes (`*Controller`, `*Service`, `*Repository`), Korean Swagger/DTO descriptions rule.

- [ ] **Step 5: Verify no reference rot & no Kotlin residue**

Run:
```bash
cd "/Users/imjunhyeon/spring projects/ssogssog_spring"
grep -rniE 'kotlin|\bval \b|data class|obrit|data\.sql|HARNESS_GUIDE|specs/ADR|EXECUTION_PLAN' CLAUDE.md AGENT.md docs/specs/ ; echo "exit=$?"
```
Expected: no matches (grep exit=1). If any line matches, fix it.
(Note: `ktlint` / `gradlew harness` are intentionally NOT in this grep — CLAUDE.md legitimately says the project has no such task. The build-verify wording above avoids the literal tool names anyway.)

- [ ] **Step 6: Commit**

```bash
git add CLAUDE.md AGENT.md docs/specs/MAP.md docs/specs/CONVENTIONS.md
git commit -m "docs: add project guide, agent entry, and architecture/convention specs"
```

---

### Task 2: Commands (`.claude/commands/`)

**Files:**
- Create: `.claude/commands/dev-plan.md`
- Create: `.claude/commands/review.md`
- Create: `.claude/commands/harness-update.md`

**Interfaces:**
- Consumes: `CLAUDE.md`, `docs/specs/MAP.md`, `docs/specs/CONVENTIONS.md` (Task 1).
- Produces: slash commands `/dev-plan`, `/review`, `/harness-update`.

- [ ] **Step 1: Write `dev-plan.md`** (English). Adapt reference: create `docs/plans/YYYY-MM-DD-{feature}.md`, decompose into tasks, execute sequentially, track status. Replace the Kotlin task template order with **our order**: domain entity/vo → repository → application service (api/usecase/writer) → presentation controller → test → `./gradlew build` + `./gradlew test` verify. Reference `docs/specs/CONVENTIONS.md` and `MAP.md`.

- [ ] **Step 2: Write `review.md`** (English). Adapt reference review command. Add a note at top: **primary reviewer is Codex; this command is a secondary Claude pass.** Review lenses rewritten for hexagonal dependency rules (from MAP.md), `ApiResponse` return, `GeneralException` usage, Java naming, security (no secret exposure — note `application.yml` is gitignored), tests. Severity tags `[required]/[recommended]/[note]`.

- [ ] **Step 3: Write `harness-update.md`** (English). Adapt reference. Managed-files table rewritten to our reality: `CLAUDE.md`, `AGENT.md`, `docs/specs/MAP.md`, `docs/specs/CONVENTIONS.md`, `.claude/skills/*`, `.claude/commands/*`, `.githooks/*` (if added later), `.github/workflows/*` (if present). Remove ADR/ktlint/ArchUnit-specific procedures (not in our project); keep convention-sync and doc-sync procedures. Verify step = `./gradlew build`.

- [ ] **Step 4: Verify no residue**

Run:
```bash
cd "/Users/imjunhyeon/spring projects/ssogssog_spring"
grep -rniE 'kotlin|obrit|data\.sql|specs/ADR|EXECUTION_PLAN|HARNESS_GUIDE' .claude/commands/ ; echo "exit=$?"
```
Expected: no matches (exit=1).
(As in Task 1, `ktlint`/`ArchUnit`/`gradlew harness` are excluded — a command may legitimately say the project has none of these.)

- [ ] **Step 5: Commit**

```bash
git add .claude/commands/
git commit -m "chore: add adapted slash commands (dev-plan, review, harness-update)"
```

---

### Task 3: Implementation & review skills

**Files:**
- Create: `.claude/skills/implement-spring-backend-feature/SKILL.md`
- Create: `.claude/skills/review-spring-backend-change/SKILL.md`
- Create: `.claude/skills/select-spring-design-pattern/SKILL.md`
- Create: `.claude/skills/organize-domain-model/SKILL.md`

**Interfaces:**
- Consumes: `docs/specs/MAP.md`, `docs/specs/CONVENTIONS.md`.
- Produces: skills `implement-spring-backend-feature`, `review-spring-backend-change`, `select-spring-design-pattern`, `organize-domain-model`.

- [ ] **Step 1: Write `implement-spring-backend-feature/SKILL.md`** (English). Keep the reference frontmatter/structure; rewrite the layer order to ours: domain entity/vo → domain repository (+ QueryDSL `*RepositoryCustom`) → application service (`api`/`usecase`/`writer`) → presentation controller (returns `ApiResponse`) → tests. Reference `GeneralException`/`ErrorStatus` for errors. Note Korean Swagger/DTO descriptions.

- [ ] **Step 2: Write `review-spring-backend-change/SKILL.md`** (English). Keep structure. Add header note: **Codex is the primary reviewer; this is Claude's secondary pass.** Rewrite dependency-direction checks to hexagonal (MAP.md). Java naming, `ApiResponse`, no entity leakage from controllers.

- [ ] **Step 3: Write `select-spring-design-pattern/SKILL.md`** (English). Mostly structure-preserving; Java examples, our package names. Patterns: Builder/Factory/Strategy/Spring Event/Decorator/Adapter.

- [ ] **Step 4: Write `organize-domain-model/SKILL.md`** (English). Rewrite our rule: domain enums/value objects live in `domain/{domain}/enums` and `domain/{domain}/vo` (not inside DTO files, not in a type-based dir). Read-model placement guidance.

- [ ] **Step 5: Verify residue-free**

Run:
```bash
cd "/Users/imjunhyeon/spring projects/ssogssog_spring"
grep -rniE 'kotlin|\bval \b|data class|obrit|data\.sql' .claude/skills/implement-spring-backend-feature .claude/skills/review-spring-backend-change .claude/skills/select-spring-design-pattern .claude/skills/organize-domain-model ; echo "exit=$?"
```
Expected: no matches (exit=1).

- [ ] **Step 6: Commit**

```bash
git add .claude/skills/implement-spring-backend-feature .claude/skills/review-spring-backend-change .claude/skills/select-spring-design-pattern .claude/skills/organize-domain-model
git commit -m "chore: add adapted implementation and review skills"
```

---

### Task 4: JPA / test / debug skills

**Files:**
- Create: `.claude/skills/jpa-patterns/SKILL.md`
- Create: `.claude/skills/test-patterns/SKILL.md`
- Create: `.claude/skills/debug-and-verify-locally/SKILL.md`

**Interfaces:**
- Produces: skills `jpa-patterns`, `test-patterns`, `debug-and-verify-locally`.

- [ ] **Step 1: Write `jpa-patterns/SKILL.md`** (English). Keep content (N+1, lazy loading, fetch strategy, transactions). Convert examples to Java + our entities (e.g. `StockMetric`). Mention QueryDSL for complex queries.

- [ ] **Step 2: Write `test-patterns/SKILL.md`** (English). Keep core principle (test important behavior). Java + JUnit 5. Note our test dir `src/test/java/org/project/ssogssog/`. Verify command `./gradlew test`.

- [ ] **Step 3: Write `debug-and-verify-locally/SKILL.md`** (English). Keep the "no guessing → run → gather evidence → verify" workflow. Command `./gradlew bootRun` (same as ours). Add explicit note: **run local verification only when the user asks.** curl/log evidence gathering.

- [ ] **Step 4: Verify residue-free**

Run:
```bash
cd "/Users/imjunhyeon/spring projects/ssogssog_spring"
grep -rniE 'kotlin|\bval \b|data class|obrit|data\.sql' .claude/skills/jpa-patterns .claude/skills/test-patterns .claude/skills/debug-and-verify-locally ; echo "exit=$?"
```
Expected: no matches (exit=1).

- [ ] **Step 5: Commit**

```bash
git add .claude/skills/jpa-patterns .claude/skills/test-patterns .claude/skills/debug-and-verify-locally
git commit -m "chore: add adapted jpa, test, and debug skills"
```

---

### Task 5: Git-gate skills + fill-github-template

**Files:**
- Create: `.claude/skills/pre-commit-check/SKILL.md`
- Create: `.claude/skills/pre-push-check/SKILL.md`
- Create: `.claude/skills/fill-github-template/SKILL.md`

**Interfaces:**
- Produces: skills `pre-commit-check`, `pre-push-check`, `fill-github-template`.

- [ ] **Step 1: Write `pre-commit-check/SKILL.md`** (English). Keep the Artium structure: check (1) secret/sensitive-data leaks in `git diff --cached`, (2) commit-message quality. Adapt: commit convention `{purpose}({scope}): {desc}`; note `application.yml` is already gitignored, so flag if it is force-staged. No auto-unstage.

- [ ] **Step 2: Write `pre-push-check/SKILL.md`** (English). Keep structure: (1) tests must pass — run `./gradlew test`; (2) docs stay in sync. Adapt doc-sync target to `docs/specs/*` (there is no `docs/PRD/api-spec.md`); flag REST/DTO/error-shape changes not reflected in docs. Never auto-fix code.

- [ ] **Step 3: Write `fill-github-template/SKILL.md`** (English). Adapt: fill `.github/pull_request_template.md` (lowercase — this repo's actual filename) and `.github/ISSUE_TEMPLATE/*` if present; both exist in this repo. Instruct the skill to locate the PR template case-insensitively (`pull_request_template.md` or `PULL_REQUEST_TEMPLATE.md`). Replace when2go/Artium repo names with ssogssog.

- [ ] **Step 4: Verify residue-free**

Run:
```bash
cd "/Users/imjunhyeon/spring projects/ssogssog_spring"
grep -rniE 'kotlin|\bval \b|data class|obrit|when2go|api-spec\.md|PRD/idea' .claude/skills/pre-commit-check .claude/skills/pre-push-check .claude/skills/fill-github-template ; echo "exit=$?"
```
Expected: no matches (exit=1).

- [ ] **Step 5: Commit**

```bash
git add .claude/skills/pre-commit-check .claude/skills/pre-push-check .claude/skills/fill-github-template
git commit -m "chore: add git-gate and github-template skills"
```

---

### Task 6: Settings + mirror to `.agent/`

**Files:**
- Create: `.claude/settings.local.json`
- Create: `.agent/settings.local.json`
- Create: `.agent/commands/` (mirror of `.claude/commands/`)
- Create: `.agent/skills/` (mirror of `.claude/skills/`)

**Interfaces:**
- Consumes: everything under `.claude/commands` and `.claude/skills` (Tasks 2–5).

- [ ] **Step 1: Write `.claude/settings.local.json`** — minimal permission allowlist for our project. Include read-only/build commands we will actually use:

```json
{
  "permissions": {
    "allow": [
      "Bash(./gradlew build)",
      "Bash(./gradlew build *)",
      "Bash(./gradlew test)",
      "Bash(./gradlew test *)",
      "Bash(./gradlew bootRun)",
      "Bash(./gradlew bootRun *)",
      "Bash(git status)",
      "Bash(git diff *)",
      "Bash(git log *)"
    ]
  }
}
```

- [ ] **Step 2: Copy the same file to `.agent/settings.local.json`**

Run:
```bash
cd "/Users/imjunhyeon/spring projects/ssogssog_spring"
mkdir -p .agent
cp .claude/settings.local.json .agent/settings.local.json
```

- [ ] **Step 3: Mirror commands and skills into `.agent/`**

`.agent/commands` and `.agent/skills` are harness-owned: this plan creates them and no user-authored local files live there. We create the `.agent` tree fresh in this run, so there is nothing to preserve. Use `rsync --delete` (mirror without a blind `rm -rf`) so the copy is idempotent and only touches these two subdirectories:

Run:
```bash
cd "/Users/imjunhyeon/spring projects/ssogssog_spring"
mkdir -p .agent/commands .agent/skills
rsync -a --delete .claude/commands/ .agent/commands/
rsync -a --delete .claude/skills/   .agent/skills/
```
(If `rsync` is unavailable, fall back to `rm -rf .agent/commands .agent/skills && cp -R ...` — but only because `.agent` is harness-owned and was created by this plan.)

- [ ] **Step 4: Verify the two trees match and counts are right**

Run:
```bash
cd "/Users/imjunhyeon/spring projects/ssogssog_spring"
echo "claude commands:"; ls .claude/commands | wc -l
echo "agent commands:";  ls .agent/commands  | wc -l
echo "claude skills:";   ls .claude/skills   | wc -l
echo "agent skills:";    ls .agent/skills    | wc -l
diff -r .claude/commands .agent/commands && echo "commands identical"
diff -r .claude/skills   .agent/skills   && echo "skills identical"
```
Expected: commands=3 both, skills=10 both, both `diff` report no differences.

- [ ] **Step 5: Commit**

```bash
git add .claude/settings.local.json .agent/
git commit -m "chore: add permission settings and mirror harness into .agent"
```

---

### Task 7: Final verification

**Files:** none (verification only).

- [ ] **Step 1: Full residue sweep across the whole harness**

Run:
```bash
cd "/Users/imjunhyeon/spring projects/ssogssog_spring"
grep -rniE 'kotlin|\bval \b|data class|obrit|data\.sql|when2go|HARNESS_GUIDE|specs/ADR|EXECUTION_PLAN|api-spec\.md' \
  CLAUDE.md AGENT.md docs/specs/ .claude/ .agent/ ; echo "exit=$?"
```
Expected: no matches (exit=1). Fix any hit in the offending file and re-run.
(`ktlint`/`ArchUnit`/`gradlew harness` excluded on purpose — see Task 1 note.)

- [ ] **Step 2: Confirm structure completeness**

Run:
```bash
cd "/Users/imjunhyeon/spring projects/ssogssog_spring"
test -f CLAUDE.md && test -f AGENT.md && test -f docs/specs/MAP.md && test -f docs/specs/CONVENTIONS.md && echo "root docs OK"
for d in .claude .agent; do
  echo "== $d =="
  ls $d/commands/*.md | wc -l
  ls -d $d/skills/*/ | wc -l
done
```
Expected: "root docs OK"; each tree shows 3 command files and 10 skill dirs.

- [ ] **Step 3: Confirm Claude Code loads the skills** — in a fresh turn, check that the session's available-skills list includes our project skills (e.g. `implement-spring-backend-feature`). This is a manual/observational check by the user.

- [ ] **Step 4: Final commit if any fixes were made** (skip if clean)

```bash
git add -A
git commit -m "chore: fix residue found in final harness verification"
```

---

## Self-Review

**Spec coverage** (design doc §9 completion criteria):
- `.claude/` + `.agent/` both have 3 commands + 10 skills → Tasks 2–6, verified in Task 6 Step 4 & Task 7 Step 2. ✓
- CLAUDE.md reflects real stack/architecture/build, no Kotlin/harness/ktlint residue → Task 1 Steps 1,5. ✓
- AGENT.md references only existing docs → Task 1 Step 2 (explicit no-broken-refs). ✓
- Skills free of Kotlin/`val`/`data.sql`/`obrit` residue → Tasks 3–5 verify steps + Task 7 sweep. ✓
- MAP.md, CONVENTIONS.md describe hexagonal structure → Task 1 Steps 3,4. ✓
- Claude Code loads `.claude/skills` → Task 7 Step 3. ✓
- English for AI-facing docs → Global Constraints + each write step says "(English)". ✓

**Placeholder scan:** No TBD/TODO. Each write step names concrete sections/rules to include. Content of skills is "adapt reference, replacing X with Y" — the reference files exist at known paths and the replacements are enumerated in Global Constraints, so no invented content. ✓

**Type consistency:** Consistent path/name usage — `docs/specs/MAP.md`, `docs/specs/CONVENTIONS.md`, `GeneralException`, `ApiResponse`, `ErrorStatus`, 10 skill names identical across tasks. Skill count "10" consistent (Task descriptions 4+3+3=10, matches Task 6/7 checks). ✓
