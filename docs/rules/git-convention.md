# Git Convention( Commit & PR Convention)
## Git Flow
- `main`: production-ready, always stable
- `develop`: integration branch; all feature branches branch off and merge here
- `feature`: branch from `develop` → open PR back to `develop` when done
- Deploy: merge `develop` into `main` when stable

## Branch Naming
- Format: `feat/description/#issue-number`
- Examples:
  - `feat/chip-component/#43`
  - `fix/login-validation/#12`

## Commit Message
- Format: `type: description`
- Examples:
  - `feat: 비즈니스 로직 추가`
  - `fix: 입력값 검증 수정`
  - `chore: 의존성 업데이트`
- Always small, atomic commits

## PR Title
- Format:`Type: description`
- Examples:
  - `Feat: apps/web 관련 변경`
  - `Feat: packages/kds-ui 관련 변경`

## PR Rules
- Keep PRs small; write documentation thoroughly
- Describe: what the problem was → what you considered → what the result was
- Don't delay PRs out of fear; submit early and iterate with team feedback
- Reviewers must review thoroughly in proportion to PR detail
- Unknown issues must be shared immediately with the team

## Labels
| Label | When to use |
|---|---|
| `chore` | ESLint, Prettier, package updates |
| `deploy` | Deployment-related tasks |
| `docs` | Documentation only |
| `feature` | New feature development |
| `fix` | Bug fixes |
| `refactor` | Code refactoring (no behavior change) |
| `style` | UI/design changes |
| `test` | Test code |