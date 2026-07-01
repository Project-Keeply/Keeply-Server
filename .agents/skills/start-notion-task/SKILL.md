---
name: start-notion-task
description: Notion 태스크 페이지에서 GitHub Issue와 브랜치를 자동 생성하고 Notion 상태를 "진행 중"으로 전이하는 스킬. "Notion 태스크 시작해줘", "이 태스크 시작하자", "start-notion-task" 등의 요청에 반드시 이 스킬을 사용한다.
---

# start-notion-task 스킬

Notion 태스크 페이지를 시작점으로 GitHub Issue, 로컬 브랜치, Notion 상태를
한 번에 세팅한다.

---

## Step 1: 전제 조건 확인

### 1.1 도구 확인
```bash
gh --version && gh auth status
```
- gh CLI 설치 + 인증 상태 확인
- 미인증 시 중단

### 1.2 현재 브랜치 확인
```bash
git rev-parse --abbrev-ref HEAD
```
- `develop`/`main`인지 확인 (다른 브랜치면 경고 후 사용자 확인)

### 1.3 커밋되지 않은 변경사항 확인
```bash
git status --porcelain
```
- 변경사항이 있으면 중단 후 사용자에게 커밋/스태시 요청
- 새 브랜치 체크아웃 시 손실 방지 목적

---

## Step 2: Notion 태스크 정보 수집

### 2.1 URL 입력
"Notion 태스크 페이지 URL을 알려주세요."
- 입력값을 `NOTION_URL`로 저장

### 2.2 Notion 페이지 fetch (MCP)
- `notion-fetch`로 페이지 조회
- 추출:
  - `TITLE` (이름 프로퍼티)
  - `TYPE` (Type 프로퍼티: feat/fix/chore 등)
  - `STATUS` (상태 프로퍼티)
  - `NOTION_PAGE_ID` (페이지 ID)

### 2.3 유효성 검증
- Status가 이미 "완료" 또는 "리뷰 중"이면 경고 → 사용자 확인
- Type이 비어있으면 사용자에게 선택 요청
  (feat / fix / chore / refactor / docs / style / perf / test)

---

## Step 3: 브랜치명 결정

### 3.1 슬러그 입력
"브랜치 슬러그를 영문 kebab-case로 입력해주세요.
예: `kakao-login`, `error-handling`"
- 입력값을 `SLUG`로 저장

### 3.2 이슈 번호는 아직 모름 (Step 4 이후 결정)

---

## Step 4: GitHub Issue 생성

### 4.1 이슈 본문 조립
아래 템플릿을 `/tmp/notion-task-issue-body.md`에 저장:

```markdown
## Related Notion Task
{NOTION_URL}

## Tasks (To-Do)
- [ ] {TITLE 기반 작업 항목}

## Description
{Notion 페이지 설명 요약, 없으면 "-"}

## ETC
-
```

### 4.2 이슈 프리뷰 → 승인
- 최종 이슈 제목/본문을 사용자에게 프리뷰
- 승인 대기 → 승인 후 진행

### 4.3 이슈 생성
```bash
gh issue create --title "{TITLE}" --body-file /tmp/notion-task-issue-body.md
```
- 응답 URL에서 `ISSUE_NUMBER`, `ISSUE_URL` 추출

---

## Step 5: 브랜치 생성 및 체크아웃

```bash
git checkout -b {TYPE}/{SLUG}/#{ISSUE_NUMBER}
```
- 예: `feat/kakao-login/#63`
- `BRANCH_NAME`으로 저장

---

## Step 6: Notion 페이지 업데이트

### 6.1 페이지 프로퍼티 업데이트 (MCP)
`notion-update-page` 호출:
- `상태`: "진행 중"
- `GitHub Issue`: `ISSUE_URL`
- `Branch`: `BRANCH_NAME`

### 6.2 업데이트 결과 확인
- 응답의 properties에서 3개 값이 정상 반영됐는지 검증

---

## Step 7: 최종 보고 (debrief 형식)

`.agents/checklists/debrief.md` 형식을 따르되, 이 스킬 특성에 맞게:

```
Notion 태스크 시작 완료

### 컨텍스트
- Notion: {NOTION_URL}
- GitHub Issue: #{ISSUE_NUMBER} — {TITLE}
- 브랜치: {BRANCH_NAME} (체크아웃 완료)

### Notion 업데이트
- 상태: {기존 상태} → 진행 중
- GitHub Issue URL 채움
- Branch 채움

### 검증 결과
- Spotless: N/A / Compile: N/A / Test: N/A / Build: N/A
- (코드 변경 없음)

### 다음 스텝 제안
- `logic-design` 스킬로 구현 계획 수립
- 개발 진행
- PR 시점에 `create-pr` 스킬 사용 (Notion PR/상태 수동 업데이트 필요)
```

---

## 중요 규칙

1. **커밋되지 않은 변경사항이 있으면 중단** — 브랜치 체크아웃 시 손실 방지
2. **모든 프리뷰는 사용자 승인 후 진행**
3. **Notion 페이지 상태가 "완료"/"리뷰 중"이면 신중하게 진행 확인**
4. **본문은 항상 한국어**
5. **preflight → skill → postflight → debrief** 프로토콜 따를 것
   - 이 스킬은 코드 변경이 없으므로 postflight의 spotless/compile/test/build는 N/A로 처리
6. **Notion 업데이트 실패 시** GitHub Issue와 브랜치는 이미 생성됐음을 명시하고
   사용자에게 수동 처리 안내
