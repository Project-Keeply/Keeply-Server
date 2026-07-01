# Branch Review (Codex + Claude Shared Setup)

This setup gives both agents the same local, diff-based pre-push review workflow.

## What is shared

- Scope collector script: `tools/branch-review/collect_scope.sh`
- Output artifacts:
  - `.tmp/branch-review/files.txt`
  - `.tmp/branch-review/commits.txt`
  - `.tmp/branch-review/diff.patch`
  - `.tmp/branch-review/summary.md`

## Run manually

기본 base ref는 `origin/develop`입니다.

```bash
bash ./tools/branch-review/collect_scope.sh
```

Or with a custom base ref:

```bash
bash ./tools/branch-review/collect_scope.sh origin/main
```

## Skill-specific wrappers

- Skill 정의: `.agents/skills/branch-review/SKILL.md`
- Claude command: `.claude/commands/branch-review.md` (symlink → skill)

## How to show "skill is active"

Use this marker at the top of any review response:

`[SKILL ACTIVE] branch-review`

The collector script also prints the same marker so terminal logs and agent responses stay aligned.

## Mandatory input rule

Before any review starts, the agent must ask for user-defined review focus points.
No focus points = no review execution.
