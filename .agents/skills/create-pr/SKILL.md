---
name: create-pr
description: 현재 브랜치를 푸시하고 gh CLI로 diff를 분석해 한국어 PR 설명을 자동 생성/업데이트하는 스킬. "PR 만들어줘", "PR 설명 작성", "PR description 써줘", "PR 올려야 하는데", "create-pr" 등의 요청에 반드시 이 스킬을 사용한다.
---

# create-pr 스킬

사용자에게 PR 제목과 중점 컨텐츠를 입력받고, gh CLI로 diff를 분석해 한국어 PR 설명을 자동 생성한다.

---

## Step 1: 전제 조건 확인

### 1.1 gh CLI 설치 확인
```bash
gh --version
```
- 설치 안 되어 있으면 중단:
  "GitHub CLI(gh)가 설치되어 있지 않습니다.
   아래 명령어로 설치해주세요:

   Mac: brew install gh
   Windows: winget install GitHub.cli

   설치 직후에는 PATH가 반영되도록 터미널을 닫았다가 다시 열거나, 새 터미널 세션에서 진행해주세요.
   이후 `gh auth login` 으로 GitHub 로그인을 해주세요."

### 1.2 gh CLI 인증 확인
```bash
gh auth status
```
- 인증 안 되어 있으면 중단: "`gh auth login`으로 먼저 인증해주세요."

### 1.3 git 레포지토리 확인
```bash
git rev-parse --git-dir
```

---

## Step 2: 브랜치 정보 파악

### 2.1 현재 브랜치 확인
```bash
git rev-parse --abbrev-ref HEAD
```
- `CURRENT_BRANCH` 변수에 저장

### 2.2 이슈 번호 추출
브랜치 네이밍 규칙: `{type}/{description}/#{issue-number}`

- `CURRENT_BRANCH`에서 `#` 뒤 숫자를 추출 → `ISSUE_NUMBER` 저장
  - 예: `feat/kakao-login/#43` → `ISSUE_NUMBER=43`
- 추출 불가 시 `ISSUE_NUMBER=null`

### 2.3 베이스 브랜치 결정
```bash
git branch -r | grep -E "origin/(develop|main|master)"
```
- 피처 브랜치(`feat/*`, `fix/*`, `chore/*`, `refactor/*`, `docs/*` 등)인 경우:
  - `develop` 존재하면 → `develop`
  - 없으면 → `main`
- `develop` 브랜치인 경우 → `main`
- `BASE_BRANCH` 변수에 저장

---

## Step 3: 사용자 입력 받기

### 3.1 PR 제목 입력
사용자에게 PR 제목을 요청한다.

"PR 제목을 입력해주세요. (예: `Feat: 카카오 OAuth 로그인 API 구현`)"

- 입력받은 값을 `PR_TITLE`로 저장
- 형식이 `Type: 설명` 패턴이 아니면 한 번 확인:
  "`Feat: ...` 형식으로 작성하시겠어요, 아니면 그대로 사용할까요?"

### 3.2 중점 컨텐츠 입력
사용자에게 PR에서 중점적으로 설명할 내용을 요청한다.

"어떤 부분을 중점적으로 설명할까요?
예) 성능 개선 수치, 특정 파일의 설계 결정, 리뷰어가 집중해야 할 부분 등
없으면 엔터로 건너뛰세요."

- 입력받은 값을 `PR_FOCUS` 변수에 저장
- 입력 없으면 `PR_FOCUS=null`

---

## Step 4: 브랜치 푸시

### 4.1 원격 브랜치 존재 여부 확인
```bash
git ls-remote --heads origin ${CURRENT_BRANCH}
```

### 4.2 푸시
- 원격에 없으면: `git push -u origin ${CURRENT_BRANCH}`
- 원격에 있으면: 로컬이 앞서있을 때만 `git push origin ${CURRENT_BRANCH}`
- 푸시 실패 시 중단 후 에러 메시지 표시

---

## Step 5: 기존 PR 확인

```bash
gh pr list --head ${CURRENT_BRANCH} --json number,url,state
```
- PR 있으면 `EXISTING_PR_NUMBER`, `EXISTING_PR_URL` 저장
- PR 없으면 `EXISTING_PR_NUMBER=null`

---

## Step 6: 변경 사항 분석

### 6.1 커밋 목록
```bash
git log ${BASE_BRANCH}..HEAD --oneline
```

### 6.2 변경된 파일 목록
```bash
git diff ${BASE_BRANCH}...HEAD --name-status
```

### 6.3 전체 diff

- PR이 이미 있으면:
  ```bash
  gh pr diff
  ```
- PR이 없으면:
  ```bash
  git diff ${BASE_BRANCH}...HEAD
  ```

### 6.4 대용량 diff 처리

**기본은 6.3 전체 diff** 로 분석한다.

**대용량으로 판단될 때만** 전체 raw diff 대신 아래를 조합한다.

```bash
git diff ${BASE_BRANCH}...HEAD --name-only | wc -l
```
- **30개 이상** 이면 대용량으로 판단

대용량 시 조합 방식:
- `git diff ${BASE_BRANCH}...HEAD --stat`
- 6.1 커밋 목록, 6.2 파일 목록
- 필요 시 핵심 경로만 부분 diff: `git diff ${BASE_BRANCH}...HEAD -- {path}`
- PR 본문에 "변경 범위가 커서 파일·커밋 단위로 요약했습니다"를 짧게 명시

---

## Step 7: PR 본문 생성

`.github/PULL_REQUEST_TEMPLATE.md`의 구조를 그대로 사용한다.

**작성 원칙:**
- **Summary:** `PR_TITLE`의 목적을 기반으로 한 줄 요약. `ISSUE_NUMBER`가 있으면 첫 줄에 `Closes #{ISSUE_NUMBER}` 추가. **`ISSUE_NUMBER=null`이면 `Closes #...` 줄 전체를 제거하여 `Closes #null` 같은 잘못된 이슈 참조가 생기지 않게 한다.**
- **Tasks:** `PR_TITLE`의 목적을 달성하기 위한 항목을 기능/의도 단위로 나열. 파일/함수 단위 금지.
- **To Reviewer:** `PR_FOCUS`가 있으면 최우선으로 상세 기술. diff에서 추론한 설계 결정(예: Controller 분리, Service 인터페이스 도입, JPA 관계 매핑 등), 리뷰어가 집중할 부분 포함. 없으면 섹션 삭제.
- **Screenshot:** 서버 프로젝트 특성상 UI 스크린샷이 필요한 경우는 드묾. Swagger UI 캡처나 Postman 응답 캡처가 있으면 유지, 없으면 섹션 삭제.
- **추론 불가 항목:** diff에서 확인하기 어려운 내용은 `[작성 필요]` 플레이스홀더 사용.

프로젝트 로컬 임시 디렉토리(`.tmp/`, gitignored)에 저장한다. 없으면 먼저 `mkdir -p .tmp`.
아래 템플릿을 채워서 `.tmp/pr-body.md`에 저장한다.

```markdown
## Summary

Closes #{ISSUE_NUMBER} <!-- ISSUE_NUMBER가 null이면 이 줄 전체를 제거할 것 -->

{PR_TITLE 기반 한 줄 요약}

## Tasks

- {완료된 작업 1}
- {완료된 작업 2}

## To Reviewer

{PR_FOCUS 기반 중점 설명 및 리뷰 포인트}

## Screenshot

| As-is | To-be |
|-------|-------|
| | |

```

**Screenshot 섹션 처리:**
- Swagger/API 응답 캡처 등 시각 자료 감지 안 되면 → `## Screenshot` 섹션 전체 제거
- 감지되면 → 테이블 빈 채로 유지 후 사용자에게 요청:
  "API 스펙 변경이 감지됐어요. Swagger UI 캡처나 Postman 응답 캡처를 첨부해주시면 PR에 추가해드릴게요 "
  → 사용자가 이미지 첨부하면 테이블에 삽입 후 `gh pr edit`으로 본문 업데이트

---

## Step 8: 생성 전 프리뷰 확인

생성할 PR 내용을 사용자에게 보여주고 승인을 받는다.

```
---
PR 프리뷰

제목: {PR_TITLE}
베이스 브랜치: {BASE_BRANCH} ← {CURRENT_BRANCH}
이슈 연결: {Closes #ISSUE_NUMBER | 없음}

본문:
{생성된 PR 본문 전체}
---

이 내용으로 PR을 생성할까요? (승인 / 수정 요청)
```

- **승인** → Step 9로
- **수정 요청** → 사용자 피드백 반영 후 Step 7부터 다시 실행

---

## Step 9: PR 생성 또는 업데이트

### 9.1 PR이 없는 경우
```bash
gh pr create \
  --base ${BASE_BRANCH} \
  --head ${CURRENT_BRANCH} \
  --title "${PR_TITLE}" \
  --body-file .tmp/pr-body.md
```

### 9.2 PR이 이미 있는 경우

본문 덮어쓰기 전에 사용자에게 확인한다.

1. `gh pr view ${EXISTING_PR_NUMBER} --json body`로 기존 본문 확인. 비어 있으면 확인 생략.
2. **사용자에게 질문:** "기존 PR 본문을 이번에 생성한 내용으로 **전부 교체** 할까요?"
   - **예(본문 교체):**
     ```bash
     gh pr edit ${EXISTING_PR_NUMBER} \
       --title "${PR_TITLE}" \
       --body-file .tmp/pr-body.md
     ```
   - **아니오(본문 유지):** `gh pr edit`을 호출하지 않는다.

- 성공 시: " PR 생성/업데이트됨: {URL}" 출력
- 실패 시: 에러 메시지 표시 후 수동 생성 안내

---

## 중요 규칙

1. **커밋되지 않은 변경사항은 절대 커밋하거나 스테이징하지 않는다**
2. **PR 본문은 항상 한국어로 작성**
3. **PR 머지는 절대 하지 않는다**
4. **반드시 사용자 프리뷰 승인 후에만 PR을 생성/업데이트한다**
5. **`gh pr diff --name-only`는 사용하지 않는다 — 파일 목록은 항상 `git diff --name-status`로 조회한다**
