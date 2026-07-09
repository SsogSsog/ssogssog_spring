---
name: fill-github-template
description: Fill ssogssog GitHub PR or issue templates using the repository's existing template files.
---

# Fill GitHub Template

Use this skill when preparing a PR or issue body.

## Template Locations

- PR template: `.github/pull_request_template.md`
- Also accept `.github/PULL_REQUEST_TEMPLATE.md` if a future rename changes case.
- Issue templates: `.github/ISSUE_TEMPLATE/*`

## Procedure

1. Inspect the current diff or the user's requested change.
2. Read the matching GitHub template.
3. Fill only sections supported by the actual change.
4. Keep wording concise and concrete.
5. Use the repo's commit-convention tone.

## PR Body Guidance

- Link the issue when provided.
- Summarize what changed.
- List behavior or infrastructure changes.
- Check the correct PR type.
- Mention tests run or not run.
- Mention Swagger updates when API docs changed.

## Rules

- Do not invent screenshots.
- Do not claim tests were run unless they were.
- Do not replace the template structure unless no template exists.
