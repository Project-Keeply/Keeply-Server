---
name: branch-review
description: 현재 브랜치의 diff를 focus point 기준으로 리뷰하는 스킬. "리뷰해줘", "코드 리뷰", "브랜치 리뷰", "push 전 확인해줘", "focus point 리뷰", "branch-review" 등의 요청에 반드시 이 스킬을 사용한다.
---

# branch-review command

Perform a selective review for the current branch only.

## 1) Collect user focus points (mandatory)

Ask:
- "Which review focus points should I use for this branch?"

Rules:
- Do not proceed without explicit user input.
- If the user does not provide focus points, ask again and stop.
- Print the exact user input in the final report under `Review Focus Points (User Input)`.

## 2) Build review scope

Run:

```bash
bash ./tools/branch-review/collect_scope.sh
```

Optional base ref:

```bash
bash ./tools/branch-review/collect_scope.sh origin/develop
```

## 3) Review policy

- Primary scope: changed lines in `.tmp/branch-review/diff.patch`
- Secondary scope: local context in changed files only when needed
- Avoid style-only comments unless they affect maintainability or defects
- 서버 컨벤션 준수 여부 확인: `docs/rules/coding-convention.md`

## 4) Output format

Start with:

`[SKILL ACTIVE] branch-review`

Then provide:
- `Review Focus Points (User Input)`
- Findings by severity (`High`, `Medium`, `Low`)
- Each finding with `file:line`, reasoning, and short fix suggestion
- A final **Refactoring Priority Queue**

If nothing is found, output:
- `[SKILL ACTIVE] branch-review`
- `No blocking issues found in the current branch diff.`

## 5) Rules

- Do not review unrelated untouched areas unless required for impact analysis.
