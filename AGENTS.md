# AGENTS.md

> Entry point and routing document for AI agents (Claude Code, Codex, etc.).
> This file only tells you WHERE to look — detailed rules live in linked docs.

## Project Summary

Keeply — a mobile-first web service for convenience store workers.
Consolidates announcements, operation logs, and expiration-date tracking to
eliminate information loss and duplicate work across shifts.

- Language: Java 21
- Framework: Spring Boot 3.5.0
- Build Tool: Gradle (Kotlin DSL)
- Database: MySQL 8.0 (Docker) + Flyway 마이그레이션
- ORM: Spring Data JPA / Hibernate
- Security: Spring Security + Kakao OAuth2 + JWT (Access/Refresh Token)
- Validation: Spring Validation (Bean Validation)
- Documentation: SpringDoc OpenAPI (Swagger)
- Formatting: Spotless (Google Java Format)

## Required Reading (Source of Truth)

Read these before starting any work.

- **[Coding Convention](docs/rules/coding-convention.md)** — 패키지 / 클래스 / 변수 / 메서드 / 컬렉션 / 타입 규칙
- **[Git Convention](docs/rules/git-convention.md)** — 브랜치 / 커밋 / PR 규칙
- **[Local Branch Review](docs/branch-review/README.md)** — pre-push 리뷰 도구

## Skill Routing

Use the following skills based on task type. Natural language triggers auto-match.

| Task Type | Skill | Trigger Examples |
|---|---|---|
| Start Notion task (Notion → GitHub) | [`start-notion-task`](.agents/skills/start-notion-task/SKILL.md) | "Notion 태스크 시작해줘", "이 태스크 시작하자" |
| Create GitHub issue | [`create-issue`](.agents/skills/create-issue/SKILL.md) | "이슈 만들어줘", "이슈 올려야 해" |
| Design implementation | [`logic-design`](.agents/skills/logic-design/SKILL.md) | "설계 좀 해줘", "구현 계획 세워줘" |
| Review branch (pre-push) | [`branch-review`](.agents/skills/branch-review/SKILL.md) | "리뷰해줘", "push 전 확인해줘" |
| Create / update PR | [`create-pr`](.agents/skills/create-pr/SKILL.md) | "PR 올려줘", "PR 설명 써줘" |

## Standard Skill Execution

Every skill invocation follows this flight protocol:

1. **Preflight** — [`.agents/checklists/preflight.md`](.agents/checklists/preflight.md) — verify context, branch, issue, and get user approval
2. **Flight** — Execute skill-specific steps (from the skill's SKILL.md)
3. **Postflight** — [`.agents/checklists/postflight.md`](.agents/checklists/postflight.md) — spotless / compile / test / build / convention checks
4. **Debrief** — Report using [`.agents/checklists/debrief.md`](.agents/checklists/debrief.md) format

**Exception**: Trivial fixes (typo, missing semicolon, 1 file & ≤5 lines) may skip preflight steps 3-5, but must be explicitly declared upfront.

## Standard Workflow

Typical feature development order (Notion-first hybrid):

```text
1. Create Notion task    → (create manually in Notion "Task 관리" DB)
2. Start Notion task     → start-notion-task
                           (auto: GitHub Issue + branch + Notion → "진행 중")
3. Design implementation → logic-design
4. Implement
5. Review branch         → branch-review
6. Create PR             → create-pr
                           (manually update Notion "리뷰 중" / PR URL; auto sync TBD)
```

**Note**: Notion is the source of truth for tasks. GitHub Issues are auto-mirrored
for PR linking. Steps 6+ Notion sync automation is planned but not yet implemented.

## Skill Specification

- Location: `.agents/skills/{name}/SKILL.md` (single source of truth)
- Claude Code slash command compatibility: `.claude/commands/{name}.md` is a symlink
- Frontmatter only uses `name` and `description`
- Names must be lowercase kebab-case

## Work Policy (Mandatory)

1. **Always preview → get approval → execute** before creating or modifying code
2. **Respond in Korean** by default (exception only when requested)
3. **Always include file paths** (e.g., `src/main/java/com/keeply/...`)
4. **Stay within scope** — do only what was requested

## Folder Structure (Summary)

```text
keeply-server/
├── AGENTS.md                    ← this file (router)
├── CLAUDE.md                    ← Claude Code entry point (references this file)
│
├── docs/                        ← human-facing manuals (source of truth)
│   ├── rules/
│   │   ├── coding-convention.md
│   │   └── git-convention.md
│   └── branch-review/
│       └── README.md
│
├── .agents/                     ← AI execution harness
│   ├── checklists/              ← flight protocol (preflight / postflight / debrief)
│   │   ├── preflight.md
│   │   ├── postflight.md
│   │   └── debrief.md
│   └── skills/                  ← actual skill files
│       ├── branch-review/SKILL.md
│       ├── create-issue/SKILL.md
│       ├── create-pr/SKILL.md
│       ├── logic-design/SKILL.md
│       └── start-notion-task/SKILL.md
│
├── tools/
│   └── branch-review/
│       └── collect_scope.sh     ← diff-based review artifact collector
│
└── .claude/
    └── commands/                ← Claude Code slash commands (symlinks → .agents/skills)
```

## Documentation Sync Rule

- Modify `docs/` first → `.agents/` follows (**one-way, never reverse**)
- Editing a skill file auto-reflects to symlinks (`.agents/skills/` → `.claude/commands/`)
