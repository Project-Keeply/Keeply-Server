# CLAUDE.md

> Claude Code entry point. All project rules, skill routing, and workflow live in AGENTS.md.

@AGENTS.md

## Claude Code Specific

### Slash Commands

`.claude/commands/*.md` are symlinks to `.agents/skills/*/SKILL.md`.
Available: `/branch-review`, `/create-issue`, `/create-pr`, `/logic-design`.

### Skill Auto-Matching

Natural-language triggers auto-invoke skills via `description` frontmatter matching.
Examples: "PR 올려줘" → `create-pr`, "리뷰해줘" → `branch-review`.

## ⚠️ CRITICAL — Agent Usage Policy

> **You MUST NEVER spawn or invoke any agent without explicit user approval.**
>
> - Before using any agent, always discuss the task with the user first. Do not proceed with agent execution on your own — present the plan, explain which agent will be used and why, and wait for explicit approval.
> - If the user has already granted approval upfront, proceed with the agent work immediately. Once the work is complete, provide a clear bulleted list of everything that was done.
